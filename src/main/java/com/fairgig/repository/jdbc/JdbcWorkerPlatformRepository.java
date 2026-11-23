package com.fairgig.repository.jdbc;

import com.fairgig.model.WorkerPlatform;
import com.fairgig.repository.WorkerPlatformRepository;
import com.fairgig.util.NotFoundException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/** PostgreSQL implementation of {@link WorkerPlatformRepository}. */
public class JdbcWorkerPlatformRepository implements WorkerPlatformRepository {

    @Override
    public void add(WorkerPlatform link) {
        Jdbc.update(
            "INSERT INTO worker_platforms (worker_id, platform_id, account_handle, joined_date) "
                + "VALUES (?, ?, ?, ?)",
            ps -> {
                ps.setInt(1, link.getWorkerId());
                ps.setInt(2, link.getPlatformId());
                ps.setString(3, link.getAccountHandle());
                ps.setObject(4, link.getJoinedDate());
            });
    }

    @Override
    public void update(WorkerPlatform link) {
        int changed = Jdbc.update(
            "UPDATE worker_platforms SET account_handle = ?, joined_date = ? "
                + "WHERE worker_id = ? AND platform_id = ?",
            ps -> {
                ps.setString(1, link.getAccountHandle());
                ps.setObject(2, link.getJoinedDate());
                ps.setInt(3, link.getWorkerId());
                ps.setInt(4, link.getPlatformId());
            });
        if (changed == 0) {
            throw new NotFoundException("You are not registered on this platform.");
        }
    }

    @Override
    public void remove(int workerId, int platformId) {
        Jdbc.update("DELETE FROM worker_platforms WHERE worker_id = ? AND platform_id = ?", ps -> {
            ps.setInt(1, workerId);
            ps.setInt(2, platformId);
        });
    }

    @Override
    public boolean exists(int workerId, int platformId) {
        return Jdbc.queryInt(
            "SELECT count(*) FROM worker_platforms WHERE worker_id = ? AND platform_id = ?", ps -> {
                ps.setInt(1, workerId);
                ps.setInt(2, platformId);
            }) > 0;
    }

    @Override
    public List<WorkerPlatform> findByWorker(int workerId) {
        return Jdbc.queryList(
            "SELECT worker_id, platform_id, account_handle, joined_date FROM worker_platforms "
                + "WHERE worker_id = ? ORDER BY platform_id",
            ps -> ps.setInt(1, workerId),
            JdbcWorkerPlatformRepository::map);
    }

    private static WorkerPlatform map(ResultSet rs) throws SQLException {
        LocalDate joined = Jdbc.getDate(rs, "joined_date");
        return new WorkerPlatform(rs.getInt("worker_id"), rs.getInt("platform_id"),
            rs.getString("account_handle"), joined);
    }
}
