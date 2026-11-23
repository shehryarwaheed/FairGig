package com.fairgig.service.platform;

import com.fairgig.model.Platform;
import com.fairgig.model.WorkerPlatform;
import com.fairgig.repository.PlatformRepository;
import com.fairgig.repository.WorkerPlatformRepository;
import com.fairgig.util.NotFoundException;
import com.fairgig.util.ValidationException;
import com.fairgig.util.Validators;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Real implementation of {@link PlatformService}. Wrap it with AccessProxy before giving it to screens. */
public class DefaultPlatformService implements PlatformService {

    private static final int MAX_NAME = 100;
    private static final int MAX_CATEGORY = 50;
    private static final int MAX_HANDLE = 100;

    private final PlatformRepository platforms;
    private final WorkerPlatformRepository workerPlatforms;

    public DefaultPlatformService(PlatformRepository platforms, WorkerPlatformRepository workerPlatforms) {
        this.platforms = platforms;
        this.workerPlatforms = workerPlatforms;
    }

    @Override
    public List<Platform> listActivePlatforms() {
        return platforms.findAllActive();
    }

    @Override
    public Platform getPlatform(int platformId) {
        return platforms.findById(platformId)
            .orElseThrow(() -> new NotFoundException("This platform no longer exists."));
    }

    @Override
    public List<WorkerPlatformInfo> myPlatforms(int workerId) {
        List<WorkerPlatformInfo> result = new ArrayList<>();
        for (WorkerPlatform link : workerPlatforms.findByWorker(workerId)) {
            platforms.findById(link.getPlatformId())
                .ifPresent(p -> result.add(new WorkerPlatformInfo(p, link.getAccountHandle(), link.getJoinedDate())));
        }
        result.sort((a, b) -> a.platform().getName().compareToIgnoreCase(b.platform().getName()));
        return result;
    }

    @Override
    public void registerPlatform(int workerId, int platformId, String accountHandle, LocalDate joinedDate) {
        List<String> problems = new ArrayList<>();
        Optional<Platform> platform = platforms.findById(platformId);
        if (platform.isEmpty() || !platform.get().isActive()) {
            problems.add("This platform is not available. Please choose one from the list.");
        } else if (workerPlatforms.exists(workerId, platformId)) {
            problems.add("You are already registered on " + platform.get().getName() + ".");
        }
        checkDetails(accountHandle, joinedDate, problems);
        if (!problems.isEmpty()) {
            throw new ValidationException(problems);
        }
        workerPlatforms.add(new WorkerPlatform(workerId, platformId,
            Validators.blankToNull(accountHandle), joinedDate));
    }

    @Override
    public void updatePlatformDetails(int workerId, int platformId, String accountHandle, LocalDate joinedDate) {
        List<String> problems = new ArrayList<>();
        if (!workerPlatforms.exists(workerId, platformId)) {
            problems.add("You are not registered on this platform yet. Please add it first.");
        }
        checkDetails(accountHandle, joinedDate, problems);
        if (!problems.isEmpty()) {
            throw new ValidationException(problems);
        }
        workerPlatforms.update(new WorkerPlatform(workerId, platformId,
            Validators.blankToNull(accountHandle), joinedDate));
    }

    @Override
    public void removePlatform(int workerId, int platformId) {
        if (!workerPlatforms.exists(workerId, platformId)) {
            throw new ValidationException("You are not registered on this platform.");
        }
        workerPlatforms.remove(workerId, platformId);
    }

    @Override
    public Platform createPlatform(String name, String category) {
        List<String> problems = new ArrayList<>();
        String cleanName = Validators.clean(name).replaceAll("\\s+", " ");
        String cleanCategory = Validators.clean(category).replaceAll("\\s+", " ");

        if (cleanName.isEmpty()) {
            problems.add("Please enter the platform name.");
        } else if (cleanName.length() > MAX_NAME) {
            problems.add("The platform name must be " + MAX_NAME + " characters or fewer.");
        } else if (platforms.findByName(cleanName).isPresent()) {
            problems.add("A platform called \"" + cleanName + "\" already exists. Please pick it from the list instead.");
        }
        if (cleanCategory.isEmpty()) {
            problems.add("Please enter a category, for example Ride-hailing or Food delivery.");
        } else if (cleanCategory.length() > MAX_CATEGORY) {
            problems.add("The category must be " + MAX_CATEGORY + " characters or fewer.");
        }
        if (!problems.isEmpty()) {
            throw new ValidationException(problems);
        }
        return platforms.save(new Platform(cleanName, cleanCategory));
    }

    private void checkDetails(String accountHandle, LocalDate joinedDate, List<String> problems) {
        if (accountHandle != null && accountHandle.trim().length() > MAX_HANDLE) {
            problems.add("Your account handle must be " + MAX_HANDLE + " characters or fewer.");
        }
        if (joinedDate != null && joinedDate.isAfter(LocalDate.now())) {
            problems.add("The joined date cannot be in the future.");
        }
    }
}
