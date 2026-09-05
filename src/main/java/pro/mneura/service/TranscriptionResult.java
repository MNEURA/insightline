package pro.mneura.service;

import pro.mneura.data.entity.Transcript;

import java.util.List;

public record TranscriptionResult(List<Transcript> transcripts, int failedCount) {
    public TranscriptionResult {
        transcripts = List.copyOf(transcripts);
    }
}
