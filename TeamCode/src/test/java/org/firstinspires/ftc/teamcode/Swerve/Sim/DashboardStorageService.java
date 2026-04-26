package org.firstinspires.ftc.teamcode.Swerve.Sim;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class DashboardStorageService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final Path storageDir;

    public DashboardStorageService() {
        this.storageDir = resolveStorageDir();
    }

    private static Path resolveStorageDir() {
        Path cwd = Paths.get("").toAbsolutePath().normalize();
        Path root = cwd.getFileName() != null && "TeamCode".equals(cwd.getFileName().toString())
                ? cwd.getParent()
                : cwd;
        if (root == null) {
            root = cwd;
        }
        return root.resolve(".swervescope").normalize();
    }

    public synchronized Map<String, Object> load(String bucket) throws IOException {
        Path file = resolveBucket(bucket);
        if (!Files.exists(file)) {
            return new HashMap<String, Object>();
        }
        byte[] bytes = Files.readAllBytes(file);
        if (bytes.length == 0) {
            return new HashMap<String, Object>();
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> value = OBJECT_MAPPER.readValue(bytes, Map.class);
        return value == null ? new HashMap<String, Object>() : value;
    }

    public synchronized Map<String, Object> save(String bucket, Map<String, Object> payload) throws IOException {
        Files.createDirectories(storageDir);
        Path file = resolveBucket(bucket);
        Map<String, Object> normalized = payload == null ? Collections.<String, Object>emptyMap() : new TreeMap<String, Object>(payload);
        Files.write(file, OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsBytes(normalized));
        return normalized;
    }

    public synchronized void delete(String bucket) throws IOException {
        Path file = resolveBucket(bucket);
        Files.deleteIfExists(file);
    }

    private Path resolveBucket(String bucket) throws IOException {
        String safe = bucket == null ? "" : bucket.trim().toLowerCase();
        if (!safe.matches("[a-z0-9_-]+")) {
            throw new IOException("Invalid storage bucket: " + bucket);
        }
        return storageDir.resolve(safe + ".json").normalize();
    }
}
