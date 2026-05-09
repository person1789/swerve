package org.firstinspires.ftc.teamcode.Swerve.Sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void simplePoseAutoMovesInSimulation() {
        // Passes if the new Kooky-style pose-step auto moves the robot in sim from its commanded sequence.
        SwerveSimulator simulator = new SwerveSimulator();
        Pose startPose = simulator.getPoseInches();
        SimSimplePoseAuto auto = new SimSimplePoseAuto();

        auto.init(simulator);
        for (int i = 0; i < 240; i++) {
            auto.loop(new BrowserGamepadState(), 0.02);
        }

        Pose endPose = simulator.getPoseInches();
        assertTrue(Math.hypot(endPose.x - startPose.x, endPose.y - startPose.y) > 0.25,
                "Expected the Kooky-style pose-step auto to move the robot");
    }

    @Test
    void resetPoseReturnsRobotToOriginAndClearsVelocity() {
        // Passes if resetting the simulator zeroes pose and chassis velocity after prior motion.
        SwerveSimulator simulator = new SwerveSimulator();
        BrowserGamepadState input = new BrowserGamepadState();
        input.leftY = -1.0;
        input.connected = true;

        simulator.updateInput(input);
        for (int i = 0; i < 10; i++) {
            simulator.step(0.02);
        }

        simulator.resetPose();

        Pose pose = simulator.getPoseInches();
        Pose velocity = simulator.getVelocityInches();
        assertEquals(0.0, pose.x, 1e-9);
        assertEquals(0.0, pose.y, 1e-9);
        assertEquals(0.0, pose.heading, 1e-9);
        assertEquals(0.0, velocity.x, 1e-9);
        assertEquals(0.0, velocity.y, 1e-9);
        assertEquals(0.0, velocity.heading, 1e-9);
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
