package org.firstinspires.ftc.teamcode.Swerve.Sim;

import java.util.Map;

import org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose;
import org.firstinspires.ftc.teamcode.auto.AutoPoseStep;
import org.firstinspires.ftc.teamcode.auto.KookyAutoController;
import org.firstinspires.ftc.teamcode.auto.SimpleAutoSequence;

class SimSimplePoseAuto implements SimOpMode {
    private final TelemetryBundler bundler = new TelemetryBundler();
    private final double[] command = new double[3];
    private SwerveSimulator sim;
    private SimpleAutoSequence sequence;

    @Override
    public void init(SwerveSimulator sim) {
        this.sim = sim;
        this.sim.resetPose();
        this.sequence = new SimpleAutoSequence(
                new KookyAutoController(),
                new AutoPoseStep(new Pose(24.0, 0.0, 0.0), 0.15, 3.0),
                new AutoPoseStep(new Pose(24.0, 24.0, Math.PI / 2.0), 0.15, 3.0),
                new AutoPoseStep(new Pose(0.0, 24.0, Math.PI), 0.15, 3.0));
        bundler.clear();
        bundler.add("Status", "Status", "Initialized");
        bundler.add("Status", "OpMode", "SimplePoseAuto");
    }

    @Override
    public void loop(BrowserGamepadState gamepad, double dt) {
        Pose currentPose = sim.getPoseInches();
        sequence.update(currentPose.x, currentPose.y, currentPose.heading, dt, command);
        sim.setDirectDriveCommand(command[0], command[1], command[2]);
        sim.step(dt);

        bundler.clear();
        bundler.add("Status", "Status", sequence.isFinished() ? "Finished" : "Running");
        bundler.add("Status", "OpMode", "SimplePoseAuto");
        bundler.add("Auto", "Step", String.valueOf(sequence.getCurrentStepIndex()));
        bundler.add("Auto", "X", String.format("%.2f in", currentPose.x));
        bundler.add("Auto", "Y", String.format("%.2f in", currentPose.y));
        bundler.add("Auto", "Heading", String.format("%.1f deg", Math.toDegrees(currentPose.heading)));
    }

    @Override
    public void stop() {
        if (sim != null) {
            sim.clearDirectDriveCommand();
        }
    }

    @Override
    public Map<String, Map<String, String>> getTelemetry() {
        return bundler.getBundles();
    }
}
