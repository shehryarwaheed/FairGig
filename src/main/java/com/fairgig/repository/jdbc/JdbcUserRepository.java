package com.fairgig.repository.jdbc;

import com.fairgig.database.TransactionManager;
import com.fairgig.model.Advocate;
import com.fairgig.model.PlatformRepresentative;
import com.fairgig.model.Role;
import com.fairgig.model.User;
import com.fairgig.model.Worker;
import com.fairgig.repository.UserRepository;
import com.fairgig.util.DataAccessException;
import com.fairgig.util.NotFoundException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** PostgreSQL implementation of {@link UserRepository}. */
public class JdbcUserRepository implements UserRepository {

    /** One query loads the user plus the columns of whichever role table applies. */
    private static final String SELECT =
        "SELECT u.user_id, u.name, u.email, u.password_hash, u.role, u.is_active, u.created_at, "
        + "       w.phone, w.city, w.city_zone, pr.platform_id "
        + "FROM users u "
        + "LEFT JOIN workers w ON w.user_id = u.user_id "
        + "LEFT JOIN platform_representatives pr ON pr.user_id = u.user_id ";

    @Override
    public User save(User user) {
        user.setEmail(normalizeEmail(user.getEmail()));
        try {
            return TransactionManager.inTransaction(connection -> {
                Jdbc.queryOne(connection,
                    "INSERT INTO users (name, email, password_hash, role, is_active) "
                        + "VALUES (?, ?, ?, ?, ?) RETURNING user_id, created_at",
                    ps -> {
                        ps.setString(1, user.getName());
                        ps.setString(2, user.getEmail());
                        ps.setString(3, user.getPasswordHash());
                        ps.setString(4, user.getRole().name());
                        ps.setBoolean(5, user.isActive());
                    },
                    rs -> {
                        user.setUserId(rs.getInt(1));
                        user.setCreatedAt(rs.getObject(2, LocalDateTime.class));
                        return user;
                    });
                insertRoleRow(connection, user);
                return user;
            });
        } catch (SQLException e) {
            throw new DataAccessException("Could not save the user.", e);
        }
    }

    private static void insertRoleRow(Connection connection, User user) {
        if (user instanceof Worker worker) {
            Jdbc.update(connection,
                "INSERT INTO workers (user_id, phone, city, city_zone) VALUES (?, ?, ?, ?)",
                ps -> {
                    ps.setInt(1, worker.getUserId());
                    ps.setString(2, worker.getPhone());
                    ps.setString(3, worker.getCity());
                    ps.setString(4, worker.getCityZone());
                });
        } else if (user instanceof PlatformRepresentative rep) {
            Jdbc.update(connection,
                "INSERT INTO platform_representatives (user_id, platform_id) VALUES (?, ?)",
                ps -> {
                    ps.setInt(1, rep.getUserId());
                    Jdbc.setInt(ps, 2, rep.getPlatformId());
                });
        } else if (user instanceof Advocate) {
            Jdbc.update(connection, "INSERT INTO advocates (user_id) VALUES (?)",
                ps -> ps.setInt(1, user.getUserId()));
        } else {
            throw new DataAccessException("Unknown user type: " + user.getClass().getSimpleName());
        }
    }

    @Override
    public Optional<User> findById(int userId) {
        return Jdbc.queryOne(SELECT + "WHERE u.user_id = ?", ps -> ps.setInt(1, userId),
            JdbcUserRepository::mapUser);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        String wanted = normalizeEmail(email);
        return Jdbc.queryOne(SELECT + "WHERE LOWER(u.email) = ?", ps -> ps.setString(1, wanted),
            JdbcUserRepository::mapUser);
    }

    @Override
    public boolean emailExists(String email) {
        String wanted = normalizeEmail(email);
        return Jdbc.queryInt("SELECT count(*) FROM users WHERE LOWER(email) = ?",
            ps -> ps.setString(1, wanted)) > 0;
    }

    @Override
    public Optional<Worker> findWorkerById(int userId) {
        return findById(userId).filter(Worker.class::isInstance).map(Worker.class::cast);
    }

    @Override
    public List<PlatformRepresentative> findRepresentativesByPlatform(int platformId) {
        return Jdbc.queryList(SELECT + "WHERE pr.platform_id = ? AND u.is_active = TRUE ORDER BY u.name",
                ps -> ps.setInt(1, platformId), JdbcUserRepository::mapUser)
            .stream()
            .filter(PlatformRepresentative.class::isInstance)
            .map(PlatformRepresentative.class::cast)
            .toList();
    }

    @Override
    public List<User> findByRole(Role role) {
        return Jdbc.queryList(SELECT + "WHERE u.role = ? AND u.is_active = TRUE ORDER BY u.name",
            ps -> ps.setString(1, role.name()), JdbcUserRepository::mapUser);
    }

    @Override
    public void updateProfile(User user) {
        user.setEmail(normalizeEmail(user.getEmail()));
        try {
            TransactionManager.inTransaction(connection -> {
                int changed = Jdbc.update(connection,
                    "UPDATE users SET name = ?, email = ?, is_active = ? WHERE user_id = ?",
                    ps -> {
                        ps.setString(1, user.getName());
                        ps.setString(2, user.getEmail());
                        ps.setBoolean(3, user.isActive());
                        ps.setInt(4, user.getUserId());
                    });
                if (changed == 0) {
                    throw new NotFoundException("This account no longer exists.");
                }
                if (user instanceof Worker worker) {
                    Jdbc.update(connection,
                        "UPDATE workers SET phone = ?, city = ?, city_zone = ? WHERE user_id = ?",
                        ps -> {
                            ps.setString(1, worker.getPhone());
                            ps.setString(2, worker.getCity());
                            ps.setString(3, worker.getCityZone());
                            ps.setInt(4, worker.getUserId());
                        });
                }
                return null;
            });
        } catch (SQLException e) {
            throw new DataAccessException("Could not update the profile.", e);
        }
    }

    @Override
    public void updatePasswordHash(int userId, String newPasswordHash) {
        int changed = Jdbc.update("UPDATE users SET password_hash = ? WHERE user_id = ?", ps -> {
            ps.setString(1, newPasswordHash);
            ps.setInt(2, userId);
        });
        if (changed == 0) {
            throw new NotFoundException("This account no longer exists.");
        }
    }

    // ---------------------------------------------------------------- mapping

    private static User mapUser(ResultSet rs) throws SQLException {
        Role role = Role.fromDb(rs.getString("role"));
        User user;
        switch (role) {
            case WORKER -> {
                Worker worker = new Worker();
                worker.setPhone(rs.getString("phone"));
                worker.setCity(rs.getString("city"));
                worker.setCityZone(rs.getString("city_zone"));
                user = worker;
            }
            case VERIFIER -> {
                PlatformRepresentative rep = new PlatformRepresentative();
                rep.setPlatformId(Jdbc.getInteger(rs, "platform_id"));
                user = rep;
            }
            default -> user = new Advocate();
        }
        user.setUserId(rs.getInt("user_id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setActive(rs.getBoolean("is_active"));
        user.setCreatedAt(Jdbc.getDateTime(rs, "created_at"));
        return user;
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
