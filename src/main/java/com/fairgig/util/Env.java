package com.fairgig.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads settings (database host, password, ...) from the local ".env" file.
 * <p>
 * Order of priority: real environment variable, then the ".env" file, then the default.
 * The ".env" file is git-ignored, so secrets never reach GitHub.
 */
public final class Env {

    private static final Map<String, String> FILE_VALUES = loadFile(Path.of(".env"));

    private Env() {}

    /** Returns the setting or {@code defaultValue} when it is missing or blank. */
    public static String get(String key, String defaultValue) {
        String fromSystem = System.getenv(key);
        if (fromSystem != null && !fromSystem.isBlank()) {
            return fromSystem.trim();
        }
        String fromFile = FILE_VALUES.get(key);
        if (fromFile != null && !fromFile.isBlank()) {
            return fromFile;
        }
        return defaultValue;
    }

    /** Parses lines like KEY=value (ignores empty lines and lines starting with #). */
    static Map<String, String> parse(List<String> lines) {
        Map<String, String> values = new HashMap<>();
        for (String raw : lines) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int eq = line.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = line.substring(0, eq).strip();
            String value = line.substring(eq + 1).strip();
            if (value.length() >= 2
                    && ((value.startsWith("\"") && value.endsWith("\""))
                    || (value.startsWith("'") && value.endsWith("'")))) {
                value = value.substring(1, value.length() - 1);
            }
            values.put(key, value);
        }
        return values;
    }

    private static Map<String, String> loadFile(Path file) {
        if (!Files.isRegularFile(file)) {
            return Map.of();
        }
        try {
            return parse(Files.readAllLines(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            return Map.of();
        }
    }
}
