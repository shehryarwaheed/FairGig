package com.fairgig.model;

/** A short label an advocate puts on complaints, such as "commission" (table "complaint_tags", FR-33). */
public class ComplaintTag {

    private Integer tagId;
    private Integer advocateId;
    private String name;

    public ComplaintTag() {}

    public Integer getTagId() { return tagId; }
    public void setTagId(Integer tagId) { this.tagId = tagId; }

    public Integer getAdvocateId() { return advocateId; }
    public void setAdvocateId(Integer advocateId) { this.advocateId = advocateId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public boolean equals(Object other) {
        return this == other
            || (other instanceof ComplaintTag that && tagId != null && tagId.equals(that.tagId));
    }

    @Override
    public int hashCode() {
        return tagId == null ? System.identityHashCode(this) : tagId.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
