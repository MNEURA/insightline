package pro.mneura.service;

import org.junit.jupiter.api.Test;
import pro.mneura.controller.api.ApiException;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranscriptionServiceTest {

    @Test
    void extractsTheFirstNonEmptyCategoryLine() {
        assertEquals("TECHNICAL", TranscriptionService.extractCategory("""
                # category

                technical
                """));
        assertEquals("ACCOUNT_BILLING", TranscriptionService.extractCategory("""
                # CATEGORY

                  account_billing
                """));
    }

    @Test
    void returnsUnknownForMissingOrInvalidCategories() {
        assertEquals("UNKNOWN", TranscriptionService.extractCategory("No category section"));
        assertEquals("UNKNOWN", TranscriptionService.extractCategory("""
                # category
                unsupported
                """));
        assertEquals("UNKNOWN", TranscriptionService.extractCategory("""
                # category
                # summary
                """));
    }

    @Test
    void resolvesOnlyAllowedPlainRecordingNames() {
        Path resolved = TranscriptionService.resolveRecordingPath("customer-call.M4A");

        assertEquals("customer-call.M4A", resolved.getFileName().toString());
        assertThrows(IllegalArgumentException.class,
                () -> TranscriptionService.resolveRecordingPath("../customer-call.mp3"));
        assertThrows(IllegalArgumentException.class,
                () -> TranscriptionService.resolveRecordingPath("nested/customer-call.mp3"));
        assertThrows(IllegalArgumentException.class,
                () -> TranscriptionService.resolveRecordingPath("customer-call.txt"));
        assertThrows(IllegalArgumentException.class,
                () -> TranscriptionService.resolveRecordingPath(null));
    }

    @Test
    void rejectsConcurrentTranscriptionBeforeAccessingDependencies() throws Exception {
        AtomicBoolean transcriptionRunning = transcriptionRunning();
        assertTrue(transcriptionRunning.compareAndSet(false, true));

        try {
            ApiException exception = assertThrows(ApiException.class,
                    () -> new TranscriptionService().transcribeAllPending());
            assertEquals(409, exception.getStatus().getStatusCode());
        } finally {
            transcriptionRunning.set(false);
        }
    }

    private static AtomicBoolean transcriptionRunning() throws ReflectiveOperationException {
        Field field = TranscriptionService.class.getDeclaredField("TRANSCRIPTION_RUNNING");
        field.setAccessible(true);
        return (AtomicBoolean) field.get(null);
    }
}
