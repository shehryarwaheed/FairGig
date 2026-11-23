package com.fairgig.repository.jdbc;

import com.fairgig.model.Platform;
import com.fairgig.repository.PlatformRepository;
import com.fairgig.util.NotFoundException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** PostgreSQL implementation of {@link PlatformRepository}. */
public class JdbcPlatformRepository implements PlatformRepository {

    private static final String SELECT = "SELECT platform_id, name, category, is_active FROM platforms ";

    @Override
    public Platform save(Platform platform) {
        return Jdbc.queryOne(
            "INSERT INTO platforms (name, category, is_active) VALUES (?, ?, ?) RETURNING platform_id",
            ps -> {
                ps.setString(1, platform.getName());
                ps.setString(2, platform.getCategory());
                ps.setBoolean(3, platform.isActive());
            },
            rs -> {
                platform.setPlatformId(rs.getInt(1));
                return platform;
            }).orElseThrow();
    }

    @Override
    public Optional<Platform> findById(int platformId) {
        return Jdbc.queryOne(SELECT + "WHERE platform_id = ?", ps -> ps.setInt(1, platformId),
            JdbcPlatformRepository::map);
    }

    @Override
    public Optional<Platform> findByName(String name) {
        String wanted = name == null ? "" : name.trim().toLowerCase();
        return Jdbc.queryOne(SELECT + "WHERE LOWER(name) = ?", ps -> ps.setString(1, wanted),
            JdbcPlatformRepository::map);
    }

    @Override
    public List<Platform> findAll() {
        return Jdbc.queryList(SELECT + "ORDER BY name", Jdbc.NO_PARAMS, JdbcPlatformRepository::map);
    }

    @Override
    public List<Platform> findAllActive() {
        return Jdbc.queryList(SELECT + "WHERE is_active = TRUE ORDER BY name", Jdbc.NO_PARAMS,
            JdbcPlatformRepository::map);
    }

    @Override
    public void update(Platform platform) {
        int changed = Jdbc.update(
            "UPDATE platforms SET name = ?, category = ?, is_active = ? WHERE platform_id = ?",
            ps -> {
                ps.setString(1, platform.getName());
                ps.setString(2, platform.getCategory());
                ps.setBoolean(3, platform.isActive());
                ps.setInt(4, platform.getPlatformId());
            });
        if (changed == 0) {
            throw new NotFoundException("This platform no longer exists.");
        }
    }

    private static Platform map(ResultSet rs) throws SQLException {
        Platform p = new Platform();
        p.setPlatformId(rs.getInt("platform_id"));
        p.setName(rs.getString("name"));
        p.setCategory(rs.getString("category"));
        p.setActive(rs.getBoolean("is_active"));
        return p;
    }
}
