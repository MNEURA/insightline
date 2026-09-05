package pro.mneura.controller.api.v1;

import com.google.gson.Gson;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pro.mneura.controller.api.ApiException;
import pro.mneura.data.dao.TranscriptDao;
import pro.mneura.service.TranscriptionResult;
import pro.mneura.service.TranscriptionService;
import pro.mneura.status.TranscriptCategory;

import java.io.IOException;

@Path("/v1")
public class TranscriptionController {

    private final TranscriptionService transcriptionService = new TranscriptionService();
    private final TranscriptDao transcriptDao = new TranscriptDao();
    private static final Gson GSON = new Gson();

    @Path("/transcribeAll")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response transcribeAll()
            throws IOException, InterruptedException {
        TranscriptionResult result = transcriptionService.transcribeAllPending();

        if (result.failedCount() > 0 && result.transcripts().isEmpty()) {
            throw ApiException.serverError("Unable to transcribe the pending recordings.");
        }

        Response.ResponseBuilder response = Response.ok(GSON.toJson(result.transcripts()));
        if (result.failedCount() > 0) {
            response.header("X-Insightline-Failed", result.failedCount());
        }
        return response.build();
    }

    @Path("/loadAllTranscripts")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response loadAllTranscripts() {
        return Response.ok(GSON.toJson(transcriptDao.getAll())).build();
    }

    @Path("/filterTranscripts")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response filterTranscripts(
            @QueryParam("from") Long from,
            @QueryParam("to") Long to,
            @QueryParam("category") String category) {
        RecordingController.validateDateRange(from, to);
        validateCategory(category);
        long fromEpoch = from != null ? from : 0L;
        long toEpoch = to != null ? to : Long.MAX_VALUE;
        return Response.ok(GSON.toJson(transcriptDao.getFiltered(fromEpoch, toEpoch, category))).build();
    }

    static void validateCategory(String category) {
        if (category != null && !category.isBlank() && !category.equalsIgnoreCase("ALL")
                && !TranscriptCategory.isKnown(category)) {
            throw ApiException.badRequest("The requested transcript category is invalid.");
        }
    }

}
