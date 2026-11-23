package com.fairgig.model;

import java.time.LocalDate;

/** Links a worker to a platform they work on (table "worker_platforms", FR-07). */
public class WorkerPlatform {

    private Integer workerId;
    private Integer platformId;
    private String accountHandle;   // optional
    private LocalDate joinedDate;   // optional

    public WorkerPlatform() {}

    public WorkerPlatform(Integer workerId, Integer platformId, String accountHandle, LocalDate joinedDate) {
        this.workerId = workerId;
        this.platformId = platformId;
        this.accountHandle = accountHandle;
        this.joinedDate = joinedDate;
    }

    public Integer getWorkerId() { return workerId; }
    public void setWorkerId(Integer workerId) { this.workerId = workerId; }

    public Integer getPlatformId() { return platformId; }
    public void setPlatformId(Integer platformId) { this.platformId = platformId; }

    public String getAccountHandle() { return accountHandle; }
    public void setAccountHandle(String accountHandle) { this.accountHandle = accountHandle; }

    public LocalDate getJoinedDate() { return joinedDate; }
    public void setJoinedDate(LocalDate joinedDate) { this.joinedDate = joinedDate; }
}
