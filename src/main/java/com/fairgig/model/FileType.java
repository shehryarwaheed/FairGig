package com.fairgig.model;

import java.util.Locale;

/** Allowed evidence file types (NFR-09). */
public enum FileType {
    PNG,
    JPG,
    PDF;

    /**
     * Maps a file name or extension to a type, or returns null when the type is not allowed.
     * "photo.JPEG", "photo.jpg" and "jpg" all give JPG.
     */
    public static FileType fromFileName(String fileNameOrExtension) {
        if (fileNameOrExtension == null) {
            return null;
        }
        String text = fileNameOrExtension.trim().toLowerCase(Locale.ROOT);
        int dot = text.lastIndexOf('.');
        String ext = dot >= 0 ? text.substring(dot + 1) : text;
        return switch (ext) {
            case "png" -> PNG;
            case "jpg", "jpeg" -> JPG;
            case "pdf" -> PDF;
            default -> null;
        };
    }

    public static FileType fromDb(String value) {
        return valueOf(value.trim().toUpperCase());
    }
}
