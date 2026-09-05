package pro.mneura.service;

import com.google.gson.Gson;
import pro.mneura.data.dao.RecordingDao;
import pro.mneura.data.entity.Recording;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

public class RecordingService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("mp3", "wav", "m4a");
    private static final Path RECORDINGS_DIRECTORY = Path.of("recordings").toAbsolutePath().normalize();
    private static final ReentrantLock RECORDING_SCAN_LOCK = new ReentrantLock();

    private final RecordingDao recordingDao = new RecordingDao();
    private static final Gson GSON = new Gson();

    public String loadAllRecordings() {
        RECORDING_SCAN_LOCK.lock();
        try {
            Files.createDirectories(RECORDINGS_DIRECTORY);
            if (!Files.isDirectory(RECORDINGS_DIRECTORY)) {
                throw new IllegalStateException("The recordings location is not a directory.");
            }

            File[] files = RECORDINGS_DIRECTORY.toFile().listFiles((directory, name) ->
                    ALLOWED_EXTENSIONS.contains(getExtension(name)));
            Set<String> existingNames = recordingDao.getAllRecordingNames();

            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && !existingNames.contains(file.getName())) {
                        recordingDao.saveNewRecording(file.getName());
                    }
                }
            }

            List<Recording> allRecordings = recordingDao.getAll();
            return GSON.toJson(allRecordings);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to access the recordings directory.", exception);
        } finally {
            RECORDING_SCAN_LOCK.unlock();
        }
    }

    private static String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }
}
