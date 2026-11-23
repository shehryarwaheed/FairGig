package com.fairgig.model;

import java.time.LocalDateTime;

/** A proof file (screenshot or statement) attached to an earning (table "evidence"). */
public class Evidence {

    private Integer evidenceId;
    private Integer recordId;
    private String filePath;        // where the file sits on disk (see the storage package)
    private FileType fileType;
    private LocalDateTime uploadedAt;

    public Evidence() {}

    public Integer getEvidenceId() { return evidenceId; }
    public void setEvidenceId(Integer evidenceId) { this.evidenceId = evidenceId; }

    public Integer getRecordId() { return recordId; }
    public void setRecordId(Integer recordId) { this.recordId = recordId; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public FileType getFileType() { return fileType; }
    public void setFileType(FileType fileType) { this.fileType = fileType; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}
