package org.firstinspires.ftc.teamcode.Swerve.Sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.net.HttpURLConnection;
import java.net.URL;

import org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose;
import org.junit.jupiter.api.Test;

class SwerveSimulatorTest {

    @Test
    void forwardInputProducesNonZeroModuleVelocityAndDrivePower() {
        // Passes if a sustained forward command through the simulator pipeline causes at least one module to report non-zero target velocity and drive power.
        SwerveSimulator simulator = new SwerveSimulator();
        BrowserGamepadState input = new BrowserGamepadState();
        input.leftY = -1.0;
        input.connected = true;

        simulator.updateInput(input);
        for (int i = 0; i < 10; i++) {
            simulator.step(0.02);
        }

        Map<String, Object> snapshot = simulator.snapshot();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> modules = (List<Map<String, Object>>) snapshot.get("modules");

        boolean anyMoving = modules.stream().anyMatch(module ->
                Math.abs(((Number) module.get("targetVelocityMps")).doubleValue()) > 1e-6
                        && Math.abs(((Number) module.get("drivePower")).doubleValue()) > 1e-6);

        assertTrue(anyMoving);
    }

    @Test
    void runServerTemporarily() throws Exception {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:8080/api/status").openConnection();
            connection.setConnectTimeout(500);
            connection.setReadTimeout(500);
            connection.connect();
            assertEquals(200, connection.getResponseCode());
            return;
        } catch (Exception ignored) {
            // no existing server, start a short-lived one below
        }

        SwerveSimulatorServer server = new SwerveSimulatorServer();
        server.start(10000, false);
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:8080/api/status").openConnection();
            connection.setConnectTimeout(1000);
            connection.setReadTimeout(1000);
            connection.connect();
            assertEquals(200, connection.getResponseCode());
        } finally {
            server.stop();
        }
    }

    @Test
    void sourceBackedTaggedAutoMovesInSimulation() {
        Path sourceFile;
        try {
            sourceFile = Files.createTempFile("PedroGeneratedTestAuto", ".java");
            Files.write(sourceFile, (
                    "package org.firstinspires.ftc.teamcode.pedroPathing;\n" +
                    "public class PedroGeneratedTestAuto {\n" +
                    "  public static Object buildSimStartPose() {\n" +
                    "    // @sim-start-start\n" +
                    "    Pose startPose = PedroStartPose.custom(-60, -60, 0);\n" +
                    "    // @sim-start-end\n" +
                    "    return startPose;\n" +
                    "  }\n" +
                    "  public static Object buildSimPath(Object follower, Object startPose) {\n" +
                    "    // @path-start\n" +
                    "    PathChain route = PedroBlockRouteBuilder.build(\n" +
                    "        follower,\n" +
                    "        startPose,\n" +
                    "        PedroBlockCommand.straight(-36, -60, 0, 1)\n" +
                    "    );\n" +
                    "    // @path-end\n" +
                    "    return route;\n" +
                    "  }\n" +
                    "}\n").getBytes());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        SwerveSimulator simulator = new SwerveSimulator();
        Pose startPose = simulator.getPoseInches();
        SimPedroTaggedAuto auto = new SimPedroTaggedAuto(
                "org.firstinspires.ftc.teamcode.pedroPathing.DoesNotExist",
                "PedroGeneratedTestAuto",
                sourceFile);

        auto.init(simulator);
        for (int i = 0; i < 240; i++) {
            auto.loop(new BrowserGamepadState(), 0.02);
        }

        Pose endPose = simulator.getPoseInches();
        assertTrue(Math.hypot(endPose.x - startPose.x, endPose.y - startPose.y) > 0.25,
                "Expected source-backed sim auto to move the robot");
    }

    @Test
    void initFailureProducesInitFailedState() {
        SwerveSimulator simulator = new SwerveSimulator();
        SimOpModeRegistry registry = new SimOpModeRegistry(simulator);
        registry.register("Broken", new java.util.function.Supplier<SimOpMode>() {
            @Override
            public SimOpMode get() {
                return new SimOpMode() {
                    @Override
                    public void init(SwerveSimulator sim) {
                    }

                    @Override
                    public void loop(BrowserGamepadState gamepad, double dt) {
                    }

                    @Override
                    public void stop() {
                    }

                    @Override
                    public Map<String, Map<String, String>> getTelemetry() {
                        java.util.Map<String, String> status = new java.util.LinkedHashMap<>();
                        status.put("Status", "Init Failed");
                        java.util.Map<String, Map<String, String>> bundles = new java.util.LinkedHashMap<>();
                        bundles.put("Status", status);
                        return bundles;
                    }
                };
            }
        });

        registry.select("Broken");
        registry.init();

        assertEquals(SimOpModeRegistry.OpModeState.INIT_FAILED, registry.getState());
    }
}
