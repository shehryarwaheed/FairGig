package com.fairgig.repository;

import com.fairgig.model.WorkerPlatform;
import java.util.List;

/** Data access for "which platforms does this worker work on" (table "worker_platforms"). */
public interface WorkerPlatformRepository {

    void add(WorkerPlatform link);

    /** Changes the account handle / joined date of an existing link. */
    void update(WorkerPlatform link);

    void remove(int workerId, int platformId);

    boolean exists(int workerId, int platformId);

    List<WorkerPlatform> findByWorker(int workerId);
}
