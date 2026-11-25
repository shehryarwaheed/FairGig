package com.fairgig.repository;

import com.fairgig.model.ComplaintTag;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Data access for complaint tags and which complaints carry them (tables "complaint_tags", "complaint_tag_map"). */
public interface ComplaintTagRepository {

    /** Inserts the tag and fills in its id. Tag names are unique. */
    ComplaintTag save(ComplaintTag tag);

    /** Same, but inside the caller's transaction (connection is not closed or committed). */
    ComplaintTag save(Connection connection, ComplaintTag tag);

    /** Finds a tag by name, ignoring upper/lower case. */
    Optional<ComplaintTag> findByName(String name);

    /** Every tag, by name. */
    List<ComplaintTag> findAll();

    /** The tags on one complaint, by name. */
    List<ComplaintTag> findByComplaint(int complaintId);

    /** The tags of ALL complaints in one query, so a list screen needs no query per row. Key = complaint id. */
    Map<Integer, List<ComplaintTag>> findAllByComplaint();

    /** Puts a tag on a complaint. Returns false when it already had that tag. */
    boolean addToComplaint(Connection connection, int complaintId, int tagId);

    /** Takes a tag off a complaint. Returns false when it did not have that tag. */
    boolean removeFromComplaint(Connection connection, int complaintId, int tagId);
}
