package com.fairgig.storage;

import com.fairgig.model.FileType;

/**
 * Where evidence files live (the "storage" package of the architecture). The database only keeps
 * the relative path returned by {@link #store}; the bytes stay on the file system.
 * All methods throw DataAccessException when the disk fails.
 */
public interface EvidenceStorage {

    /**
     * Saves the bytes under a NEW, safe name (the user's file name is never used on disk).
     *
     * @return the relative path to keep in the database, e.g. "record-12/3f2a....png"
     */
    String store(int recordId, FileType type, byte[] content);

    /** Reads a file back. Paths that try to leave the storage folder are refused. */
    byte[] read(String relativePath);

    /** Removes a file (used to clean up when saving to the database fails). Missing files are ignored. */
    void delete(String relativePath);
}
