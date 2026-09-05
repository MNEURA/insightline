package pro.mneura.controller.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiExceptionMapperTest {

    private static final Gson GSON = new Gson();

    @Test
    void encodesApplicationErrorsAsJson() {
        Response response = new ApiExceptionMapper()
                .toResponse(ApiException.badRequest("A \"quoted\" value is invalid."));

        try {
            JsonObject body = GSON.fromJson(response.getEntity().toString(), JsonObject.class);
            assertEquals(400, response.getStatus());
            assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
            assertEquals("A \"quoted\" value is invalid.", body.get("error").getAsString());
        } finally {
            response.close();
        }
    }

    @Test
    void hidesUnexpectedExceptionDetails() {
        Response response = new ApiExceptionMapper()
                .toResponse(new IllegalStateException("database password leaked"));

        try {
            JsonObject body = GSON.fromJson(response.getEntity().toString(), JsonObject.class);
            assertEquals(500, response.getStatus());
            assertEquals("Unable to complete the request.", body.get("error").getAsString());
        } finally {
            response.close();
        }
    }
}
