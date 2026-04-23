package org.firstinspires.ftc.teamcode.Swerve.Sim;

import com.fasterxml.jackson.databind.ObjectMapper;

import fi.iki.elonen.NanoHTTPD;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SwerveSimulatorServer extends NanoHTTPD {
    private static final int PORT = 8080;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final SwerveSimulator simulator;
    private final ScheduledExecutorService executor;

    public SwerveSimulatorServer() throws IOException {
        super(PORT);
        this.simulator = new SwerveSimulator();
        this.executor = Executors.newSingleThreadScheduledExecutor();
        this.executor.scheduleAtFixedRate(() -> simulator.step(0.02), 0, 20, TimeUnit.MILLISECONDS);
    }

    public static void main(String[] args) throws Exception {
        SwerveSimulatorServer server = new SwerveSimulatorServer();
        server.start(SOCKET_READ_TIMEOUT, false);

        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
        System.out.println("Swerve simulator running at http://localhost:" + PORT);
    }

    @Override
    public Response serve(IHTTPSession session) {
        try {
            if (Method.GET.equals(session.getMethod()) && "/".equals(session.getUri())) {
                return newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8",
                        new String(loadIndexHtml(), "UTF-8"));
            }

            if (Method.POST.equals(session.getMethod()) && "/api/input".equals(session.getUri())) {
                BrowserGamepadState input = OBJECT_MAPPER.readValue(readRequestBody(session), BrowserGamepadState.class);
                simulator.updateInput(input);
                return newFixedLengthResponse(Response.Status.NO_CONTENT, MIME_PLAINTEXT, "");
            }

            if (Method.GET.equals(session.getMethod()) && "/api/state".equals(session.getUri())) {
                String payload = OBJECT_MAPPER.writeValueAsString(simulator.snapshot());
                return newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", payload);
            }

            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found");
        } catch (Exception e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, e.getMessage());
        }
    }

    private byte[] readRequestBody(IHTTPSession session) throws Exception {
        int contentLength = 0;
        String header = session.getHeaders().get("content-length");
        if (header != null) {
            try {
                contentLength = Integer.parseInt(header);
            } catch (NumberFormatException ignored) {
                contentLength = 0;
            }
        }

        if (contentLength <= 0) {
            return new byte[0];
        }

        byte[] body = new byte[contentLength];
        InputStream input = session.getInputStream();
        int offset = 0;
        while (offset < contentLength) {
            int read = input.read(body, offset, contentLength - offset);
            if (read < 0) {
                break;
            }
            offset += read;
        }
        if (offset == body.length) {
            return body;
        }
        return new String(body, 0, offset, StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8);
    }

    private byte[] loadIndexHtml() throws IOException {
        InputStream classpathResource = SwerveSimulatorServer.class.getResourceAsStream("/swerve-sim/index.html");
        if (classpathResource != null) {
            try (InputStream inputStream = classpathResource; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
                return output.toByteArray();
            }
        }

        Path fallback = Paths.get("TeamCode", "src", "test", "resources", "swerve-sim", "index.html");
        return Files.readAllBytes(fallback);
    }

    private void shutdown() {
        stop();
        executor.shutdownNow();
    }
}
