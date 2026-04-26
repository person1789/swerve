package org.firstinspires.ftc.teamcode.Swerve.Sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

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
        SwerveSimulatorServer server = new SwerveSimulatorServer();
        server.start(10000, false);
        System.out.println("Server started for testing...");
        // Run for 15 seconds to allow UI testing
        Thread.sleep(15000);
        server.stop();
    }

    @Test
    void sourceBackedTaggedAutoMovesInSimulation() {
        Path sourceFile = Paths.get(
                "C:\\Users\\ajayp\\Downloads\\FTCcode - Copy\\TeamCode\\src\\main\\java\\org\\firstinspires\\ftc\\teamcode\\pedroPathing\\TestAuto.java");
        assertTrue(Files.isRegularFile(sourceFile), "Expected generated Pedro auto source to exist");

        SwerveSimulator simulator = new SwerveSimulator();
        Pose startPose = simulator.getPoseInches();
        SimPedroTaggedAuto auto = new SimPedroTaggedAuto(
                "org.firstinspires.ftc.teamcode.pedroPathing.DoesNotExist",
                "TestAuto",
                sourceFile);

        auto.init(simulator);
        for (int i = 0; i < 80; i++) {
            auto.loop(new BrowserGamepadState(), 0.02);
        }

        Pose endPose = simulator.getPoseInches();
        assertTrue(Math.hypot(endPose.x - startPose.x, endPose.y - startPose.y) > 1.0,
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
