package pro.mneura.service;

import pro.mneura.controller.api.ApiException;
import pro.mneura.data.dao.RecordingDao;
import pro.mneura.data.dao.TranscriptDao;
import pro.mneura.data.entity.Recording;
import pro.mneura.data.entity.Transcript;
import pro.mneura.status.TranscriptCategory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TranscriptionService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("mp3", "wav", "m4a");
    private static final Path RECORDINGS_DIRECTORY = Path.of("recordings").toAbsolutePath().normalize();
    private static final Path TRANSCRIPTS_DIRECTORY = Path.of("transcripts_tmp").toAbsolutePath().normalize();
    private static final Path PYTHON_SCRIPT = Path.of("scripts", "index.py").toAbsolutePath().normalize();
    private static final String PYTHON_EXECUTABLE = "python3";
    private static final long PROCESS_TIMEOUT_MINUTES = 10;
    private static final AtomicBoolean TRANSCRIPTION_RUNNING = new AtomicBoolean(false);
    private static final Logger LOGGER = Logger.getLogger(TranscriptionService.class.getName());

    private final RecordingDao recordingDao = new RecordingDao();
    private final TranscriptDao transcriptDao = new TranscriptDao();

    public TranscriptionResult transcribeAllPending() throws IOException, InterruptedException {
        if (!TRANSCRIPTION_RUNNING.compareAndSet(false, true)) {
            throw ApiException.conflict("A transcription request is already in progress.");
        }

        try {
            Files.createDirectories(TRANSCRIPTS_DIRECTORY);
            if (!Files.isDirectory(TRANSCRIPTS_DIRECTORY)) {
                throw new IOException("The transcription temporary location is not a directory.");
            }

            List<Recording> pending = recordingDao.getUntranscribed();
            List<Transcript> created = new ArrayList<>();
            int failedCount = 0;

            for (Recording recording : pending) {
                Path output = null;
                try {
                    Path audio = resolveRecordingPath(recording.getRecordingName());
                    output = resolveOutputPath(audio.getFileName().toString());
                    Files.deleteIfExists(output);

                    if (!Files.isRegularFile(audio) || !runTranscriptionScript(audio, output)) {
                        failedCount++;
                        continue;
                    }

                    String content = Files.readString(output);
                    Transcript transcript = new Transcript(
                            recording.getRecordingName(),
                            content,
                            extractCategory(content)
                    );

                    transcriptDao.save(transcript);
                    recordingDao.markTranscribed(recording.getRecordingName());
                    created.add(transcript);
                } catch (IOException | RuntimeException exception) {
                    failedCount++;
                    LOGGER.log(Level.WARNING, "A pending recording could not be transcribed: {0}",
                            exception.getClass().getName());
                } finally {
                    deleteTemporaryOutput(output);
                }
            }

            return new TranscriptionResult(created, failedCount);
        } finally {
            TRANSCRIPTION_RUNNING.set(false);
        }
    }

    private boolean runTranscriptionScript(Path audioFile, Path outputFile)
            throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(
                PYTHON_EXECUTABLE,
                PYTHON_SCRIPT.toString(),
                audioFile.toString(),
                "--out",
                outputFile.toString()
        );
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);

        Process process = processBuilder.start();
        try {
            boolean finished = process.waitFor(PROCESS_TIMEOUT_MINUTES, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (InterruptedException exception) {
            process.destroyForcibly();
            throw exception;
        }
    }

    static Path resolveRecordingPath(String recordingName) {
        if (recordingName == null || recordingName.isBlank()) {
            throw new IllegalArgumentException("Invalid recording name.");
        }

        Path fileName;
        try {
            fileName = Path.of(recordingName);
        } catch (InvalidPathException exception) {
            throw new IllegalArgumentException("Invalid recording name.", exception);
        }

        if (fileName.getFileName() == null
                || !fileName.getFileName().toString().equals(recordingName)
                || !ALLOWED_EXTENSIONS.contains(getExtension(recordingName))) {
            throw new IllegalArgumentException("Invalid recording name.");
        }

        Path resolved = RECORDINGS_DIRECTORY.resolve(fileName).normalize();
        if (!resolved.startsWith(RECORDINGS_DIRECTORY)) {
            throw new IllegalArgumentException("Invalid recording path.");
        }
        return resolved;
    }

    static String extractCategory(String content) {
        boolean categorySectionFound = false;
        for (String line : content.split("\\R")) {
            String trimmed = line.trim();

            if (trimmed.startsWith("#")) {
                String headerText = trimmed.replaceFirst("^#+\\s*", "").trim();

                if (headerText.equalsIgnoreCase("category")) {
                    categorySectionFound = true;
                    continue;
                }

                // Model sometimes merges the heading and the value into one line,
                // e.g. "# ORDER_SERVICE" instead of "# category" + "ORDER_SERVICE"
                TranscriptCategory inlineCategory = TranscriptCategory.from(headerText);
                if (inlineCategory != TranscriptCategory.UNKNOWN) {
                    return inlineCategory.name();
                }

                if (categorySectionFound) {
                    // header appears where a value was expected — nothing usable
                    return TranscriptCategory.UNKNOWN.name();
                }
                continue;
            }

            if (categorySectionFound && !trimmed.isEmpty()) {
                return TranscriptCategory.from(trimmed).name();
            }
        }
        return TranscriptCategory.UNKNOWN.name();
    }

    private static Path resolveOutputPath(String recordingName) {
        String outputName = stripExtension(recordingName) + "_keypoints.md";
        Path output = TRANSCRIPTS_DIRECTORY.resolve(outputName).normalize();
        if (!output.startsWith(TRANSCRIPTS_DIRECTORY)) {
            throw new IllegalArgumentException("Invalid transcription output path.");
        }
        return output;
    }

    private static void deleteTemporaryOutput(Path output) {
        if (output == null) {
            return;
        }
        try {
            Files.deleteIfExists(output);
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Unable to remove a temporary transcription file: {0}",
                    exception.getClass().getName());
        }
    }

    private static String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot == -1 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot == -1 ? filename : filename.substring(0, dot);
    }
}
