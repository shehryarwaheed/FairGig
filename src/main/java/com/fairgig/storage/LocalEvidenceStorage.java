package com.fairgig.storage;

import com.fairgig.model.FileType;
import com.fairgig.util.DataAccessException;
import com.fairgig.util.Env;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.UUID;

/** Keeps evidence on the local disk, one folder per earning: root/record-12/uuid.png */
public class LocalEvidenceStorage implements EvidenceStorage {

    private final Path root;

    public LocalEvidenceStorage(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    /** Uses the folder named by UPLOAD_DIR in .env (default "uploads"). */
    public static LocalEvidenceStorage fromEnv() {
        return new LocalEvidenceStorage(Path.of(Env.get("UPLOAD_DIR", "uploads")));
    }

    @Override
    public String store(int recordId, FileType type, byte[] content) {
        String relative = "record-" + recordId + "/" + UUID.randomUUID() + "." + type.name().toLowerCase(Locale.ROOT);
        Path target = resolve(relative);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content, StandardOpenOption.CREATE_NEW);
            return relative;
        } catch (IOException e) {
            throw new DataAccessException("The file could not be saved on disk. Please try again.", e);
        }
    }

    @Override
    public byte[] read(String relativePath) {
        try {
            return Files.readAllBytes(resolve(relativePath));
        } catch (NoSuchFileException e) {
            throw new DataAccessException("The evidence file is missing from storage.", e);
        } catch (IOException e) {
            throw new DataAccessException("The file could not be read from disk.", e);
        }
    }

    @Override
    public void delete(String relativePath) {
        try {
            Files.deleteIfExists(resolve(relativePath));
        } catch (IOException ignored) {
            // best-effort clean-up
        }
    }

    /** Turns a stored relative path into a real path, refusing anything outside the root folder. */
    private Path resolve(String relativePath) {
        Path resolved = root.resolve(relativePath).normalize();
        if (!resolved.startsWith(root)) {
            throw new DataAccessException("Invalid file path.");
        }
        return resolved;
    }
}
