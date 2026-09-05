package pro.mneura.controller.api;

import jakarta.ws.rs.core.Response;

public final class ApiException extends RuntimeException {
    private final Response.Status status;

    private ApiException(Response.Status status, String message) {
        super(message);
        this.status = status;
    }

    public static ApiException badRequest(String message) {
        return new ApiException(Response.Status.BAD_REQUEST, message);
    }

    public static ApiException unauthorized(String message) {
        return new ApiException(Response.Status.UNAUTHORIZED, message);
    }

    public static ApiException conflict(String message) {
        return new ApiException(Response.Status.CONFLICT, message);
    }

    public static ApiException serverError(String message) {
        return new ApiException(Response.Status.INTERNAL_SERVER_ERROR, message);
    }

    public Response.Status getStatus() {
        return status;
    }
}
