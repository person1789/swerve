package org.firstinspires.ftc.teamcode.pedroPathing;

import com.acmerobotics.dashboard.config.Config;
import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.BezierCurve;
import com.pedropathing.geometry.BezierLine;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.Path;
import com.pedropathing.paths.PathChain;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import org.firstinspires.ftc.teamcode.Swerve.Core.PoseStorage;

/**
 * Multi-segment Pedro autonomous that exercises both line and curve following.
 */
@Config
@Autonomous(name = "Pedro Path Chain Auto", group = "Pedro")
public class PedroPathChainAuto extends LinearOpMode {
    public static double leg1ForwardIn = 30.0;
    public static double curveControlXIn = 38.0;
    public static double curveControlYIn = 16.0;
    public static double curveEndXIn = 24.0;
    public static double curveEndYIn = 32.0;
    public static double leg3EndXIn = 0.0;
    public static double leg3EndYIn = 32.0;

    @Override
    public void runOpMode() throws InterruptedException {
        PedroSwerveFactory.PedroRobot robot = PedroSwerveFactory.createRobot(hardwareMap, null);
        Follower follower = robot.follower;
        PedroSwerveFactory.applyLiveTuning(follower);

        Pose startPose = new Pose(0.0, 0.0, 0.0);
        org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose storedPose = PoseStorage.getCurrentPose();
        if (storedPose != null) {
            startPose = new Pose(storedPose.x, storedPose.y, storedPose.heading);
        }

        Pose leg1End = new Pose(startPose.getX() + leg1ForwardIn, startPose.getY(), startPose.getHeading());
        Pose leg2End = new Pose(curveEndXIn, curveEndYIn, Math.toRadians(90.0));
        Pose leg3End = new Pose(leg3EndXIn, leg3EndYIn, Math.toRadians(180.0));

        PathChain routine = follower.pathBuilder()
                .addPath(new Path(new BezierLine(startPose, leg1End)))
                .setConstantHeadingInterpolation(startPose.getHeading())
                .addPath(new Path(new BezierCurve(
                        leg1End,
                        new Pose(curveControlXIn, curveControlYIn, leg1End.getHeading()),
                        leg2End)))
                .setLinearHeadingInterpolation(leg1End.getHeading(), leg2End.getHeading())
                .addPath(new Path(new BezierLine(leg2End, leg3End)))
                .setLinearHeadingInterpolation(leg2End.getHeading(), leg3End.getHeading())
                .build();

        robot.stack.resetForStart();
        follower.setStartingPose(startPose);
        follower.setPose(startPose);

        telemetry.addLine("Pedro path chain auto ready");
        telemetry.addData("Start", startPose);
        telemetry.addData("Leg1End", leg1End);
        telemetry.addData("Leg2End", leg2End);
        telemetry.addData("Leg3End", leg3End);
        telemetry.update();

        waitForStart();
        if (isStopRequested()) {
            return;
        }

        follower.followPath(routine, true);

        while (opModeIsActive() && follower.isBusy()) {
            PedroSwerveFactory.applyLiveTuning(follower);
            follower.update();
            Pose pose = follower.getPose();
            telemetry.addData("X", pose.getX());
            telemetry.addData("Y", pose.getY());
            telemetry.addData("HeadingDeg", Math.toDegrees(pose.getHeading()));
            telemetry.addData("DistanceRemaining", follower.getDistanceRemaining());
            telemetry.update();
        }

        PoseStorage.setFromPedroPose(follower.getPose());
        robot.stack.stopPedroDrive();
    }
}
