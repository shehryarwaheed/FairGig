package com.fairgig.repository;

import com.fairgig.model.ComplaintCluster;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/** Data access for common issues (table "complaint_clusters", FR-33). */
public interface ComplaintClusterRepository {

    /** Inserts the cluster and fills in its id and createdAt. */
    ComplaintCluster save(ComplaintCluster cluster);

    /** Same, but inside the caller's transaction (connection is not closed or committed). */
    ComplaintCluster save(Connection connection, ComplaintCluster cluster);

    Optional<ComplaintCluster> findById(int clusterId);

    /** All clusters, newest first. */
    List<ComplaintCluster> findAll();
}
