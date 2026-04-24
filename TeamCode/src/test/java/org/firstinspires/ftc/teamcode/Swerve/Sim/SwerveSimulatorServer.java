package org.firstinspires.ftc.teamcode.Swerve.Sim;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.iki.elonen.NanoWSD;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.HashMap;

public class SwerveSimulatorServer extends NanoWSD {
    private static final int PORT = 8080;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final SwerveSimulator simulator;
    private final SimOpModeRegistry opModeRegistry;
    private final ScheduledExecutorService executor;
    private final Set<SwerveWebSocket> connections = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public SwerveSimulatorServer() throws IOException {
        super(PORT);
        this.simulator = new SwerveSimulator();
        this.opModeRegistry = new SimOpModeRegistry(simulator);
        this.executor = Executors.newSingleThreadScheduledExecutor();
        this.executor.scheduleAtFixedRate(this::tick, 0, 20, TimeUnit.MILLISECONDS);
    }

    public static void main(String[] args) throws Exception {
        SwerveSimulatorServer server = new SwerveSimulatorServer();
        server.start(SOCKET_READ_TIMEOUT, false);

        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
        System.out.println("Swerve simulator (WS) running at ws://localhost:" + PORT + "/ws");
        System.out.println("Swerve simulator API running at http://localhost:" + PORT + "/api/status");
    }

    private void tick() {
        BrowserGamepadState currentGamepad = simulator.getGamepadState();

        // If an OpMode is running, let it drive the sim
        if (opModeRegistry.getState() == SimOpModeRegistry.OpModeState.RUNNING) {
            opModeRegistry.loop(currentGamepad, 0.02);
        } else {
            // Default: direct gamepad control (backwards compat)
            simulator.step(0.02);
        }

        if (!connections.isEmpty()) {
            try {
                Map<String, Object> state = simulator.snapshot();

                // Add OpMode info
                state.put("opModeState", opModeRegistry.getState().name());
                state.put("activeOpMode", opModeRegistry.getActiveOpModeName());
                state.put("availableOpModes", opModeRegistry.getAvailableOpModes());
                state.put("opModeTelemetry", opModeRegistry.getTelemetry());

                Map<String, Object> message = new HashMap<>();
                message.put("type", "state");
                message.put("data", state);
                String payload = OBJECT_MAPPER.writeValueAsString(message);
                for (SwerveWebSocket ws : connections) {
                    if (ws.isOpen()) {
                        ws.send(payload);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected WebSocket openWebSocket(IHTTPSession handshake) {
        return new SwerveWebSocket(handshake);
    }

    @Override
    public Response serveHttp(IHTTPSession session) {
        String uri = session.getUri();
        Response res;

        if ("/api/status".equals(uri)) {
            res = newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\":\"ok\",\"mode\":\"sim\"}");
        } else if ("/api/opmodes".equals(uri)) {
            try {
                String json = OBJECT_MAPPER.writeValueAsString(opModeRegistry.getAvailableOpModes());
                res = newFixedLengthResponse(Response.Status.OK, "application/json", json);
            } catch (Exception e) {
                res = newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Error");
            }
        } else if ("/api/config".equals(uri)) {
            try {
                String json = OBJECT_MAPPER.writeValueAsString(ConfigReflector.readConfig());
                res = newFixedLengthResponse(Response.Status.OK, "application/json", json);
            } catch (Exception e) {
                res = newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Error");
            }
        } else {
            return super.serveHttp(session);
        }

        res.addHeader("Access-Control-Allow-Origin", "*");
        return res;
    }

    @SuppressWarnings("unchecked")
    private class SwerveWebSocket extends WebSocket {
        public SwerveWebSocket(IHTTPSession handshakeRequest) {
            super(handshakeRequest);
        }

        @Override
        protected void onOpen() {
            connections.add(this);
            System.out.println("WebSocket connection opened.");

            // Send initial config and opmode list
            try {
                Map<String, Object> configMsg = new HashMap<>();
                configMsg.put("type", "config");
                configMsg.put("data", ConfigReflector.readConfig());
                send(OBJECT_MAPPER.writeValueAsString(configMsg));

                Map<String, Object> opmodesMsg = new HashMap<>();
                opmodesMsg.put("type", "opmodes");
                opmodesMsg.put("data", opModeRegistry.getAvailableOpModes());
                send(OBJECT_MAPPER.writeValueAsString(opmodesMsg));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onClose(WebSocketFrame.CloseCode code, String reason, boolean initiatedByRemote) {
            connections.remove(this);
            System.out.println("WebSocket connection closed.");
        }

        @Override
        protected void onMessage(WebSocketFrame message) {
            try {
                Map<String, Object> msg = OBJECT_MAPPER.readValue(message.getTextPayload(), Map.class);
                String type = (String) msg.get("type");

                switch (type) {
                    case "gamepad":
                        BrowserGamepadState gamepad = OBJECT_MAPPER.convertValue(msg.get("data"), BrowserGamepadState.class);
                        simulator.updateInput(gamepad);
                        break;

                    case "opmode_select":
                        String name = (String) msg.get("name");
                        opModeRegistry.select(name);
                        System.out.println("OpMode selected: " + name);
                        break;

                    case "opmode_control":
                        String action = (String) msg.get("action");
                        switch (action) {
                            case "INIT": opModeRegistry.init(); break;
                            case "START": opModeRegistry.start(); break;
                            case "STOP": opModeRegistry.stop(); break;
                        }
                        System.out.println("OpMode control: " + action);
                        break;

                    case "config_update":
                        String key = (String) msg.get("key");
                        Object value = msg.get("value");
                        boolean ok = ConfigReflector.writeConfig(key, value);
                        System.out.println("Config update: " + key + " = " + value + " → " + (ok ? "OK" : "FAILED"));

                        // Broadcast updated config to all clients
                        Map<String, Object> configMsg = new HashMap<>();
                        configMsg.put("type", "config");
                        configMsg.put("data", ConfigReflector.readConfig());
                        String configPayload = OBJECT_MAPPER.writeValueAsString(configMsg);
                        for (SwerveWebSocket ws : connections) {
                            if (ws.isOpen()) ws.send(configPayload);
                        }
                        break;

                    default:
                        System.out.println("Unknown message type: " + type);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onPong(WebSocketFrame pong) {}

        @Override
        protected void onException(IOException exception) {
            connections.remove(this);
        }
    }

    private void shutdown() {
        stop();
        executor.shutdownNow();
    }
}
