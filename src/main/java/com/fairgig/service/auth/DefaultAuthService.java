package com.fairgig.service.auth;

import com.fairgig.factory.user.NewUserData;
import com.fairgig.factory.user.UserFactoryProvider;
import com.fairgig.model.Platform;
import com.fairgig.model.Role;
import com.fairgig.model.User;
import com.fairgig.model.Worker;
import com.fairgig.repository.PlatformRepository;
import com.fairgig.repository.UserRepository;
import com.fairgig.security.PasswordHasher;
import com.fairgig.security.PasswordPolicy;
import com.fairgig.security.Session;
import com.fairgig.util.NotFoundException;
import com.fairgig.util.ValidationException;
import com.fairgig.util.Validators;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Accounts: register, log in, log out, view and update the profile (FR-01 to FR-04).
 * All problems are reported as ValidationException with messages the user can act on (NFR-10).
 * Passwords are never stored, logged or returned: only BCrypt hashes (NFR-01).
 */
public class DefaultAuthService implements AuthService {

    /** Used to spend the same time when the email is unknown, so attackers cannot tell it apart. */
    private static final String DUMMY_HASH = PasswordHasher.hash("not-a-real-password-1");

    private static final String BAD_LOGIN = "Incorrect email or password. Please check them and try again.";

    private final UserRepository users;
    private final PlatformRepository platforms;

    public DefaultAuthService(UserRepository users, PlatformRepository platforms) {
        this.users = users;
        this.platforms = platforms;
    }

    // ---------------------------------------------------------------- register (FR-01, FR-02)

    /** Validates the request, creates the right kind of user through the factory and saves it. */
    @Override
    public User register(RegistrationRequest request) {
        List<String> problems = new ArrayList<>();
        String name = Validators.clean(request.name());
        String email = Validators.clean(request.email()).toLowerCase(Locale.ROOT);

        checkName(name, problems);
        checkEmail(email, null, problems);
        problems.addAll(PasswordPolicy.problems(request.password()));

        Role role = request.role();
        if (role == null) {
            problems.add("Please choose a role: Worker, Platform Representative or Advocate.");
        } else if (role == Role.WORKER) {
            checkWorkerFields(request.phone(), request.city(), request.cityZone(), problems);
        } else if (role == Role.VERIFIER) {
            checkAssignedPlatform(request.platformId(), problems);
        }
        if (!problems.isEmpty()) {
            throw new ValidationException(problems);
        }

        NewUserData data = new NewUserData(
            name,
            email,
            PasswordHasher.hash(request.password()),
            Validators.blankToNull(request.phone()),
            Validators.clean(request.city()),
            Validators.clean(request.cityZone()),
            request.platformId());
        return users.save(UserFactoryProvider.forRole(role).create(data));
    }

    // ---------------------------------------------------------------- login / logout (FR-03)

    /** Checks the credentials and starts the session. */
    @Override
    public User login(String email, String password) {
        if (Validators.isBlank(email) || Validators.isBlank(password)) {
            throw new ValidationException("Please enter your email and your password.");
        }
        Optional<User> found = users.findByEmail(email);
        String hashToCheck = found.map(User::getPasswordHash).orElse(DUMMY_HASH);
        boolean passwordOk = PasswordHasher.matches(password, hashToCheck);

        if (found.isEmpty() || !passwordOk) {
            throw new ValidationException(BAD_LOGIN);
        }
        User user = found.get();
        if (!user.isActive()) {
            throw new ValidationException("This account has been deactivated. Please contact support.");
        }
        Session.start(user);
        return user;
    }

    @Override
    public void logout() {
        Session.clear();
    }

    @Override
    public Optional<User> currentUser() {
        return Session.currentUser();
    }

    // ---------------------------------------------------------------- profile (FR-04)

    @Override
    public User viewProfile(int userId) {
        return users.findById(userId)
            .orElseThrow(() -> new NotFoundException("This account no longer exists."));
    }

    /**
     * Updates the profile. Phone, city and zone only apply to workers and are ignored for other roles.
     * The role itself can never be changed here.
     */
    @Override
    public User updateProfile(int userId, String name, String email, String phone, String city, String cityZone) {
        User user = viewProfile(userId);
        List<String> problems = new ArrayList<>();
        String cleanName = Validators.clean(name);
        String cleanEmail = Validators.clean(email).toLowerCase(Locale.ROOT);

        checkName(cleanName, problems);
        checkEmail(cleanEmail, userId, problems);
        if (user instanceof Worker) {
            checkWorkerFields(phone, city, cityZone, problems);
        }
        if (!problems.isEmpty()) {
            throw new ValidationException(problems);
        }

        user.setName(cleanName);
        user.setEmail(cleanEmail);
        if (user instanceof Worker worker) {
            worker.setPhone(Validators.blankToNull(phone));
            worker.setCity(Validators.clean(city));
            worker.setCityZone(Validators.clean(cityZone));
        }
        users.updateProfile(user);

        Session.currentUser()
            .filter(current -> userId == current.getUserId())
            .ifPresent(current -> Session.start(user));
        return user;
    }

    @Override
    public void changePassword(int userId, String currentPassword, String newPassword) {
        User user = viewProfile(userId);
        if (!PasswordHasher.matches(currentPassword, user.getPasswordHash())) {
            throw new ValidationException("Your current password is not correct.");
        }
        List<String> problems = PasswordPolicy.problems(newPassword);
        if (!problems.isEmpty()) {
            throw new ValidationException(problems);
        }
        users.updatePasswordHash(userId, PasswordHasher.hash(newPassword));
    }

    // ---------------------------------------------------------------- checks

    private void checkName(String name, List<String> problems) {
        if (name.isEmpty()) {
            problems.add("Please enter your full name.");
        } else if (name.length() > 100) {
            problems.add("Your name must be 100 characters or fewer.");
        }
    }

    /** @param ownId the user's own id when editing a profile (their own email is allowed), else null */
    private void checkEmail(String email, Integer ownId, List<String> problems) {
        if (email.isEmpty()) {
            problems.add("Please enter your email address.");
        } else if (!Validators.isValidEmail(email)) {
            problems.add("Please enter a valid email address, for example name@example.com.");
        } else if (email.length() > 150) {
            problems.add("Your email must be 150 characters or fewer.");
        } else {
            Optional<User> existing = users.findByEmail(email);
            if (existing.isPresent() && (ownId == null || !ownId.equals(existing.get().getUserId()))) {
                problems.add("This email is already registered. Try logging in instead.");
            }
        }
    }

    private void checkWorkerFields(String phone, String city, String cityZone, List<String> problems) {
        String cleanCity = Validators.clean(city);
        String cleanZone = Validators.clean(cityZone);
        if (cleanCity.isEmpty()) {
            problems.add("Please enter your city.");
        } else if (cleanCity.length() > 50) {
            problems.add("Your city must be 50 characters or fewer.");
        }
        if (cleanZone.isEmpty()) {
            problems.add("Please enter your city zone, for example Gulberg.");
        } else if (cleanZone.length() > 50) {
            problems.add("Your city zone must be 50 characters or fewer.");
        }
        if (phone != null && phone.trim().length() > 20) {
            problems.add("Your phone number must be 20 characters or fewer.");
        }
    }

    private void checkAssignedPlatform(Integer platformId, List<String> problems) {
        if (platformId == null) {
            problems.add("Please select your assigned platform.");
            return;
        }
        Optional<Platform> platform = platforms.findById(platformId);
        if (platform.isEmpty() || !platform.get().isActive()) {
            problems.add("The selected platform is not available. Please choose another one.");
        }
    }
}
