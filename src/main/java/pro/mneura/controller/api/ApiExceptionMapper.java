package pro.mneura.controller.api;

import com.google.gson.Gson;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public final class ApiExceptionMapper implements ExceptionMapper<Exception> {
    private static final Logger LOGGER = Logger.getLogger(ApiExceptionMapper.class.getName());
    private static final Gson GSON = new Gson();

    @Override
    public Response toResponse(Exception exception) {
        if (exception instanceof ApiException apiException) {
            return error(apiException.getStatus(), apiException.getMessage());
        }

        if (exception instanceof WebApplicationException webException
                && webException.getResponse() != null) {
            Response.StatusType status = webException.getResponse().getStatusInfo();
            return error(status, status.getFamily() == Response.Status.Family.SERVER_ERROR
                    ? "Unable to complete the request."
                    : "The request could not be completed.");
        }

        LOGGER.log(Level.SEVERE, "Unhandled API exception: {0}", exception.getClass().getName());
        return error(Response.Status.INTERNAL_SERVER_ERROR, "Unable to complete the request.");
    }

    private Response error(Response.StatusType status, String message) {
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(GSON.toJson(Map.of("error", message)))
                .build();
    }
}
