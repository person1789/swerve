package org.firstinspires.ftc.teamcode.pedroPathing;

import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.Swerve.Core.LoopTimeEstimator;
import org.firstinspires.ftc.teamcode.Swerve.Core.Logger;
import org.firstinspires.ftc.teamcode.Swerve.Core.MathUtil;
import org.firstinspires.ftc.teamcode.Swerve.Core.PoseStorage;
import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Vector;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.HWMap;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.SwerveDrivetrain;
import org.firstinspires.ftc.teamcode.Swerve.Input.MotionSmoother;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Control.SwerveController;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Localization.SwerveLocalizer;

/**
 * Shared robot-motion stack used by both teleop and Pedro follower adapters.
 */
public class PedroUnifiedSwerveStack {
    private final HWMap hwMap;
    private final Logger logger;
    private final SwerveLocalizer localizer;
    private final SwerveDrivetrain drivetrain;
    private final SwerveController controller;
    private final MotionSmoother fieldInputSmoother;
    private final LoopTimeEstimator loopTimeEstimator;

    private double lastDtSec = SwerveConfig.LOOP_TIME_SEC;
    private long lastPedroLoopNanos = 0L;
    private boolean pedroLoopOpen = false;
    private double totalHeadingRad = 0.0;
    private double previousHeadingRad = 0.0;
    private double pedroVelocityConstraintScale = 1.0;

    public PedroUnifiedSwerveStack(HardwareMap hardwareMap, Logger logger) {
        this.hwMap = new HWMap(hardwareMap);
        this.logger = logger;
        this.localizer = new SwerveLocalizer(hwMap);
        this.drivetrain = new SwerveDrivetrain(hwMap, logger);
        this.controller = new SwerveController();
        this.fieldInputSmoother = new MotionSmoother();
        this.loopTimeEstimator = new LoopTimeEstimator();
        this.previousHeadingRad = localizer.getHeading();
    }

    public void restorePoseFromStorage() {
        Pose storedPose = PoseStorage.getCurrentPose();
        if (storedPose != null) {
            localizer.setPose(storedPose.toVector());
            PoseStorage.clear();
            previousHeadingRad = localizer.getHeading();
            totalHeadingRad = previousHeadingRad;
        }
    }

    public void setPose(Pose pose) {
        if (pose == null) {
            return;
        }
        localizer.setPose(pose.toVector());
        previousHeadingRad = localizer.getHeading();
        totalHeadingRad = previousHeadingRad;
    }

    public void setPedroPose(com.pedropathing.geometry.Pose pose) {
        if (pose == null) {
            return;
        }
        setPose(new Pose(pose.getX(), pose.getY(), pose.getHeading()));
    }

    public void resetForStart() {
        loopTimeEstimator.reset();
        lastDtSec = SwerveConfig.LOOP_TIME_SEC;
        lastPedroLoopNanos = 0L;
        pedroLoopOpen = false;
        previousHeadingRad = localizer.getHeading();
        totalHeadingRad = previousHeadingRad;
        drivetrain.resetSmoother();
        fieldInputSmoother.reset();
    }

    public double beginTeleOpLoop(double measuredDtSec) {
        pedroLoopOpen = false;
        lastDtSec = loopTimeEstimator.update(measuredDtSec);
        updateLocalization();
        return lastDtSec;
    }

    public void resetHeadingForTeleOp() {
        localizer.resetHeading();
        controller.resetHeading(0.0);
        drivetrain.resetSmoother();
        fieldInputSmoother.reset();
        previousHeadingRad = localizer.getHeading();
        totalHeadingRad = previousHeadingRad;
    }

    public void driveTeleOp(double rawFieldForward, double rawFieldStrafe, double rawTurn) {
        double heading = localizer.getHeading();
        Vector fieldTarget = fieldInputSmoother.smooth(new Vector(rawFieldForward, rawFieldStrafe, rawTurn), lastDtSec);
        Vector processedFieldTarget = controller.update(
                fieldTarget.x(),
                fieldTarget.y(),
                fieldTarget.omega(),
                heading,
                lastDtSec);

        double cos = Math.cos(-heading);
        double sin = Math.sin(-heading);
        double robotForward = processedFieldTarget.x() * cos - processedFieldTarget.y() * sin;
        double robotStrafe = processedFieldTarget.x() * sin + processedFieldTarget.y() * cos;

        drivetrain.setVelocity(new Vector(robotForward, robotStrafe, processedFieldTarget.omega()), lastDtSec);
    }

    public void advancePedroLoop() {
        if (pedroLoopOpen) {
            return;
        }

        long now = System.nanoTime();
        double measuredDtSec = (lastPedroLoopNanos == 0L)
                ? SwerveConfig.LOOP_TIME_SEC
                : (now - lastPedroLoopNanos) * 1e-9;
        lastPedroLoopNanos = now;
        lastDtSec = loopTimeEstimator.update(measuredDtSec);
        updateLocalization();
        pedroLoopOpen = true;
    }

    public void drivePedroRobotCentric(double forward, double strafe, double rotation) {
        advancePedroLoop();
        drivetrain.setAutonomousVelocity(
                new Vector(
                        forward * pedroVelocityConstraintScale,
                        strafe * pedroVelocityConstraintScale,
                        rotation * pedroVelocityConstraintScale),
                lastDtSec);
        pedroLoopOpen = false;
    }

    public void stopPedroDrive() {
        drivePedroRobotCentric(0.0, 0.0, 0.0);
    }

    public com.pedropathing.geometry.Pose getPedroPose() {
        Vector pose = localizer.getPose();
        return new com.pedropathing.geometry.Pose(pose.x(), pose.y(), pose.omega());
    }

    public com.pedropathing.geometry.Pose getPedroVelocityPose() {
        Vector velocity = drivetrain.getActualVelocity();
        return new com.pedropathing.geometry.Pose(
                metersToInches(velocity.x()),
                metersToInches(velocity.y()),
                velocity.omega());
    }

    public com.pedropathing.math.Vector getPedroVelocityVector() {
        return getPedroVelocityPose().getAsVector();
    }

    public boolean isPoseFinite() {
        Vector pose = localizer.getPose();
        return Double.isFinite(pose.x()) && Double.isFinite(pose.y()) && Double.isFinite(pose.omega());
    }

    public double getLastDtSec() {
        return lastDtSec;
    }

    public double getTotalHeadingRad() {
        return totalHeadingRad;
    }

    public SwerveLocalizer getLocalizer() {
        return localizer;
    }

    public SwerveDrivetrain getDrivetrain() {
        return drivetrain;
    }

    public SwerveController getController() {
        return controller;
    }

    public Logger getLogger() {
        return logger;
    }

    public void setPedroVelocityConstraintScale(double scale) {
        pedroVelocityConstraintScale = MathUtil.clamp(scale, 0.0, 1.0);
    }

    public double getPedroVelocityConstraintScale() {
        return pedroVelocityConstraintScale;
    }

    private void updateLocalization() {
        localizer.update(drivetrain.getActualVelocity(), lastDtSec);
        double heading = localizer.getHeading();
        totalHeadingRad += MathUtil.angleError(previousHeadingRad, heading);
        previousHeadingRad = heading;
    }

    private static double metersToInches(double meters) {
        return meters / 0.0254;
    }
}
