package org.firstinspires.ftc.teamcode.Swerve.Sim;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simulated TeleOp that mirrors the real MainTeleOp logic.
 * Drives the simulator using gamepad input with field-centric control.
 */
public class SimMainTeleOp implements SimOpMode {
    private SwerveSimulator sim;
    private final TelemetryBundler bundler = new TelemetryBundler();
    private long loopCount = 0;
    private long startTimeMs = 0;

    @Override
    public void init(SwerveSimulator sim) {
        this.sim = sim;
        bundler.clear();
        bundler.add("Status", "Status", "Initialized");
        bundler.add("Status", "OpMode", "MainTeleOp");
    }

    @Override
    public void loop(BrowserGamepadState gamepad, double dt) {
        if (startTimeMs == 0) startTimeMs = System.currentTimeMillis();
        loopCount++;

        sim.updateInput(gamepad);
        sim.step(dt);

        Map<String, Object> snapshot = sim.snapshot();

        bundler.clear();
        bundler.add("Status", "OpMode", "MainTeleOp");
        bundler.add("Status", "Status", "Running");
        
        bundler.add("Performance", "Loop #", String.valueOf(loopCount));
        bundler.add("Performance", "Loop Hz", String.format("%.1f", 1.0 / dt));
        bundler.add("Performance", "Elapsed", String.format("%.1fs", (System.currentTimeMillis() - startTimeMs) / 1000.0));

        @SuppressWarnings("unchecked")
        Map<String, Object> pose = (Map<String, Object>) snapshot.get("pose");
        if (pose != null) {
            bundler.add("Pose", "Heading", String.format("%.1f°", pose.get("headingDegrees")));
            bundler.add("Pose", "X", String.format("%.3f m", pose.get("xMeters")));
            bundler.add("Pose", "Y", String.format("%.3f m", pose.get("yMeters")));
        }

        bundler.add("Drivetrain", "State", String.valueOf(snapshot.get("drivetrainState")));
        bundler.add("Drivetrain", "Heading Hold", String.valueOf(snapshot.get("headingHold")));
        bundler.add("Drivetrain", "Snap", String.valueOf(snapshot.get("activeSnap")));
    }

    @Override
    public void stop() {
        bundler.clear();
        bundler.add("Status", "Status", "Stopped");
        loopCount = 0;
        startTimeMs = 0;
    }

    @Override
    public Map<String, Map<String, String>> getTelemetry() {
        return bundler.getBundles();
    }
}
