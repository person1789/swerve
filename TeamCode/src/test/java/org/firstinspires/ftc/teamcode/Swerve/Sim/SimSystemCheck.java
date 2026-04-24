package org.firstinspires.ftc.teamcode.Swerve.Sim;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Diagnostic OpMode that sweeps each module through a full rotation
 * and tests drive power, useful for verifying PID tuning and hardware.
 */
public class SimSystemCheck implements SimOpMode {
    private SwerveSimulator sim;
    private final TelemetryBundler bundler = new TelemetryBundler();
    private double elapsedSeconds = 0;
    private int currentModule = 0;
    private double sweepAngle = 0;

    @Override
    public void init(SwerveSimulator sim) {
        this.sim = sim;
        bundler.clear();
        bundler.add("Status", "Status", "Initialized");
        bundler.add("Status", "OpMode", "SwerveSystemCheck");
        bundler.add("Phase", "Ready", "Ready");
    }

    @Override
    public void loop(BrowserGamepadState gamepad, double dt) {
        elapsedSeconds += dt;
        sweepAngle = Math.sin(elapsedSeconds * 1.5) * Math.PI; // sweep ±180°

        BrowserGamepadState synth = new BrowserGamepadState();

        bundler.clear();
        bundler.add("Status", "OpMode", "SwerveSystemCheck");
        bundler.add("Status", "Status", "Running");
        
        double phaseTime = elapsedSeconds % 5.0;
        if (phaseTime < 3.0) {
            synth.rightX = Math.sin(elapsedSeconds * 2.0) * 0.5;
            bundler.add("Phase", "Test", "Rotation Test (Module " + currentModule + ")");
        } else {
            synth.leftX = Math.cos(elapsedSeconds) * 0.5;
            synth.leftY = Math.sin(elapsedSeconds) * 0.5;
            bundler.add("Phase", "Test", "Translation Test");
        }

        if (elapsedSeconds % 5.0 < dt) {
            currentModule = (currentModule + 1) % 4;
        }

        sim.updateInput(synth);
        sim.step(dt);

        Map<String, Object> snapshot = sim.snapshot();
        bundler.add("Performance", "Elapsed", String.format("%.1fs", elapsedSeconds));
        bundler.add("Diagnostics", "Sweep Angle", String.format("%.1f°", Math.toDegrees(sweepAngle)));
        bundler.add("Diagnostics", "Active Module", String.valueOf(currentModule));
        bundler.add("Drivetrain", "State", String.valueOf(snapshot.get("drivetrainState")));
    }

    @Override
    public void stop() {
        bundler.clear();
        bundler.add("Status", "Status", "Stopped");
        bundler.add("Phase", "Phase", "Complete");
        elapsedSeconds = 0;
        currentModule = 0;
    }

    @Override
    public Map<String, Map<String, String>> getTelemetry() {
        return bundler.getBundles();
    }
}
