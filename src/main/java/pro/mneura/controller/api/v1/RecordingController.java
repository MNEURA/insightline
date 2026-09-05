package pro.mneura.controller.api.v1;

import com.google.gson.Gson;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pro.mneura.controller.api.ApiException;
import pro.mneura.data.dao.RecordingDao;
import pro.mneura.service.RecordingService;


@Path("/v1")
public class RecordingController {

    private final RecordingService recordingService = new RecordingService();
    private final RecordingDao recordingDao = new RecordingDao();
    private static final Gson GSON = new Gson();

    @Path("/loadAllRecording")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response loadAllRecordings() {
        return Response.ok(recordingService.loadAllRecordings()).build();
    }

    @Path("/filterRecordings")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response filterRecordings(
            @QueryParam("from") Long from,
            @QueryParam("to") Long to) {
        validateDateRange(from, to);
        long fromEpoch = from != null ? from : 0L;
        long toEpoch = to != null ? to : Long.MAX_VALUE;
        return Response.ok(GSON.toJson(recordingDao.getFiltered(fromEpoch, toEpoch))).build();
    }

    static void validateDateRange(Long from, Long to) {
        if ((from != null && from < 0) || (to != null && to < 0)) {
            throw ApiException.badRequest("Date filters must be valid timestamps.");
        }
        if (from != null && to != null && from > to) {
            throw ApiException.badRequest("The start date must not be after the end date.");
        }
    }
}
