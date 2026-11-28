package com.fairgig.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fairgig.model.FileType;
import com.fairgig.service.evidence.EvidenceFileValidator;
import com.fairgig.util.ValidationException;
import org.junit.jupiter.api.Test;

/** NFR-09: evidence must be a real PNG, JPG or PDF of at most 5 MB. */
class EvidenceFileValidatorTest {

    static byte[] png(int totalSize) {
        byte[] b = new byte[totalSize];
        byte[] header = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        System.arraycopy(header, 0, b, 0, Math.min(8, totalSize));
        return b;
    }

    static byte[] jpg() {
        byte[] b = new byte[50];
        b[0] = (byte) 0xFF;
        b[1] = (byte) 0xD8;
        b[2] = (byte) 0xFF;
        return b;
    }

    static byte[] pdf() {
        return "%PDF-1.4\nfake statement".getBytes();
    }

    private static String message(String name, byte[] content) {
        return assertThrows(ValidationException.class, () -> EvidenceFileValidator.validate(name, content)).getMessage();
    }

    @Test
    void realFilesAreAccepted() {
        assertEquals(FileType.PNG, EvidenceFileValidator.validate("shot.PNG", png(100)));
        assertEquals(FileType.JPG, EvidenceFileValidator.validate("photo.jpg", jpg()));
        assertEquals(FileType.JPG, EvidenceFileValidator.validate("photo.JPEG", jpg()));
        assertEquals(FileType.PDF, EvidenceFileValidator.validate("statement.pdf", pdf()));
    }

    @Test
    void otherFileTypesAreRefusedWithAClearMessage() {
        String message = message("virus.exe", png(100));
        assertTrue(message.contains("Only PNG, JPG or PDF"));
        assertTrue(message.contains("virus.exe"));
    }

    @Test
    void aFileThatIsNotReallyAnImageIsRefusedEvenWithTheRightName() {
        assertTrue(message("proof.png", "hello world".getBytes()).contains("does not look like a real PNG"));
        assertTrue(message("proof.png", jpg()).contains("PNG"), "JPG bytes named .png");
        assertTrue(message("proof.pdf", png(100)).contains("PDF"));
    }

    @Test
    void emptyAndMissingFilesAreRefused() {
        assertTrue(message("a.png", new byte[0]).contains("empty"));
        assertTrue(message("a.png", null).contains("empty"));
        assertTrue(message(null, png(10)).contains("Only PNG, JPG or PDF"));
    }

    @Test
    void sizeLimitIsExactlyFiveMegabytes() {
        int limit = (int) EvidenceFileValidator.MAX_BYTES;
        assertEquals(FileType.PNG, EvidenceFileValidator.validate("ok.png", png(limit)));
        String tooBig = message("big.png", png(limit + 1));
        assertTrue(tooBig.contains("too large") && tooBig.contains("5 MB"));
    }
}
