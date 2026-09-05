package pro.mneura.controller.api.v1;

import org.junit.jupiter.api.Test;
import pro.mneura.controller.api.ApiException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TranscriptionControllerTest {

    @Test
    void acceptsKnownAndOptionalCategories() {
        assertDoesNotThrow(() -> TranscriptionController.validateCategory(null));
        assertDoesNotThrow(() -> TranscriptionController.validateCategory(""));
        assertDoesNotThrow(() -> TranscriptionController.validateCategory("ALL"));
        assertDoesNotThrow(() -> TranscriptionController.validateCategory("help"));
    }

    @Test
    void rejectsUnknownCategories() {
        ApiException exception = assertThrows(ApiException.class,
                () -> TranscriptionController.validateCategory("sales"));

        assertEquals(400, exception.getStatus().getStatusCode());
        assertEquals("The requested transcript category is invalid.", exception.getMessage());
    }
}
