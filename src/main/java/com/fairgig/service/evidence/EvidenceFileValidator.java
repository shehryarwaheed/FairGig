package com.fairgig.service.evidence;

import com.fairgig.model.FileType;
import com.fairgig.util.ValidationException;
import com.fairgig.util.Validators;
import java.util.Locale;

/**
 * NFR-09: evidence must be a PNG, JPG or PDF of at most 5 MB. Invalid files are rejected with
 * a message that says what is wrong. The file CONTENT is checked too (not just the name), so a
 * renamed "virus.exe" or a text file called "proof.png" is refused.
 */
public final class EvidenceFileValidator {

    public static final long MAX_BYTES = 5L * 1024 * 1024;

    private EvidenceFileValidator() {}

    /** Returns the detected type or throws ValidationException. */
    public static FileType validate(String fileName, byte[] content) {
        String name = Validators.clean(fileName);
        FileType type = FileType.fromFileName(name);
        if (type == null) {
            throw new ValidationException("Only PNG, JPG or PDF files are accepted"
                + (name.isEmpty() ? "." : ", but \"" + shorten(name) + "\" is not one of them. Please choose another file."));
        }
        if (content == null || content.length == 0) {
            throw new ValidationException("The file \"" + shorten(name) + "\" is empty. Please choose the original file.");
        }
        if (content.length > MAX_BYTES) {
            throw new ValidationException(String.format(Locale.ROOT,
                "The file is too large (%.1f MB). The limit is 5 MB. Please choose a smaller file or compress it.",
                content.length / (1024.0 * 1024.0)));
        }
        if (!contentMatches(type, content)) {
            throw new ValidationException("The content of \"" + shorten(name) + "\" does not look like a real "
                + type.name() + " file. Please choose the original file.");
        }
        return type;
    }

    /** Checks the first bytes ("magic numbers") of the file. */
    static boolean contentMatches(FileType type, byte[] c) {
        return switch (type) {
            case PNG -> startsWith(c, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            case JPG -> startsWith(c, 0xFF, 0xD8, 0xFF);
            case PDF -> startsWith(c, 0x25, 0x50, 0x44, 0x46, 0x2D);        // "%PDF-"
        };
    }

    private static boolean startsWith(byte[] content, int... expected) {
        if (content.length < expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if ((content[i] & 0xFF) != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private static String shorten(String name) {
        return name.length() <= 60 ? name : name.substring(0, 57) + "...";
    }
}
