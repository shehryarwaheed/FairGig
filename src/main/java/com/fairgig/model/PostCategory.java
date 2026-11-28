package com.fairgig.model;

/** Category of a community post. The names match the database CHECK constraint. */
public enum PostCategory {
    PLATFORM_RATES("Platform rates"),
    COMPLAINT("Complaint"),
    SUPPORT_REQUEST("Support request");

    private final String label;

    PostCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
