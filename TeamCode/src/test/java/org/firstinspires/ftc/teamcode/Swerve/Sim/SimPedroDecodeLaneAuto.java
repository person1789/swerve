package org.firstinspires.ftc.teamcode.Swerve.Sim;

import java.util.Map;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.PathChain;

import org.firstinspires.ftc.teamcode.pedroPathing.PedroDecodeLaneAuto;
import org.firstinspires.ftc.teamcode.pedroPathing.PedroDecodeRoute;
import org.firstinspires.ftc.teamcode.pedroPathing.PedroStartPose;
import org.firstinspires.ftc.teamcode.pedroPathing.PedroSwerveFactory;

public class SimPedroDecodeLaneAuto implements SimOpMode {
    private final TelemetryBundler bundler = new TelemetryBundler();
    private SwerveSimulator sim;
    private Follower follower;
    private SimPedroDrivetrain drivetrain;
    private boolean started;
    private double elapsedSec;

    @Override
    public void init(SwerveSimulator sim) {
        this.sim = sim;
        this.drivetrain = new SimPedroDrivetrain(sim);
        SimPedroLocalizer localizer = new SimPedroLocalizer(sim);
        this.follower = new Follower(PedroSwerveFactory.createFollowerConstants(), localizer, drivetrain);

        Pose startPose = PedroDecodeRoute.startPose(
                PedroStartPose.fromName(
                        PedroDecodeLaneAuto.configuredStartPose,
                        PedroStartPose.RED_BASE_CORNER));
        PathChain routine = PedroDecodeRoute.buildLaneAuto(follower, startPose);

        sim.setPoseInches(new org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose(
                startPose.getX(),
                startPose.getY(),
                startPose.getHeading()));
        follower.setStartingPose(startPose);
        follower.setPose(startPose);
        follower.followPath(routine, true);

        started = true;
        elapsedSec = 0.0;
        bundler.clear();
        bundler.add("Status", "Status", "Initialized");
        bundler.add("Status", "OpMode", "Pedro DECODE Lane Auto");
    }

    @Override
    public void loop(BrowserGamepadState gamepad, double dt) {
        if (!started) {
            return;
        }

        elapsedSec += dt;
        follower.update();
        sim.step(dt);

        Pose pose = follower.getPose();
        bundler.clear();
        bundler.add("Status", "OpMode", "Pedro DECODE Lane Auto");
        bundler.add("Status", "Status", follower.isBusy() ? "Running" : "Complete");
        bundler.add("Status", "Elapsed", String.format("%.1fs", elapsedSec));
        bundler.add("Pose", "X", String.format("%.2f in", pose.getX()));
        bundler.add("Pose", "Y", String.format("%.2f in", pose.getY()));
        bundler.add("Pose", "Heading", String.format("%.1f deg", Math.toDegrees(pose.getHeading())));
        bundler.add("Path", "Distance Remaining", String.format("%.2f", follower.getDistanceRemaining()));
        bundler.add("Path", "At Parametric End", String.valueOf(follower.atParametricEnd()));
        bundler.add("Drive", "Busy", String.valueOf(follower.isBusy()));
        bundler.add("Drive", "Debug", drivetrain.debugString());
    }

    @Override
    public void stop() {
        if (drivetrain != null) {
            drivetrain.breakFollowing();
        }
        bundler.clear();
        bundler.add("Status", "Status", "Stopped");
        started = false;
        elapsedSec = 0.0;
    }

    @Override
    public Map<String, Map<String, String>> getTelemetry() {
        return bundler.getBundles();
    }
}
