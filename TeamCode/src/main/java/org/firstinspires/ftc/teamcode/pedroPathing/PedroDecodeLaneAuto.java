package org.firstinspires.ftc.teamcode.pedroPathing;

import com.acmerobotics.dashboard.config.Config;
import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.PathChain;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import org.firstinspires.ftc.teamcode.Swerve.Core.PoseStorage;

/**
 * DECODE season navigation skeleton:
 * leave BASE, approach the alliance GOAL / CLASSIFIER side, then return to BASE.
 *
 * Assumption: start pose is in the alliance-side corner of BASE, facing downfield.
 * This is a path-only autonomous and does not yet actuate intake / scoring mechanisms.
 */
@Config
@Autonomous(name = "Pedro DECODE Lane Auto", group = "Pedro")
public class PedroDecodeLaneAuto extends LinearOpMode {
    public static double startXIn = -60.0;
    public static double startYIn = -60.0;
    public static double startHeadingDeg = 0.0;

    @Override
    public void runOpMode() throws InterruptedException {
        PedroSwerveFactory.PedroRobot robot = PedroSwerveFactory.createRobot(hardwareMap, null);
        Follower follower = robot.follower;

        Pose startPose = new Pose(startXIn, startYIn, Math.toRadians(startHeadingDeg));
        org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose storedPose = PoseStorage.getCurrentPose();
        if (storedPose != null) {
            startPose = new Pose(storedPose.x, storedPose.y, storedPose.heading);
        }

        Pose laneExit = PedroDecodeRoute.laneExit(startPose);
        Pose goalApproach = PedroDecodeRoute.goalApproach();
        Pose classifierBypass = PedroDecodeRoute.classifierBypass();
        Pose baseReturn = PedroDecodeRoute.baseReturn();
        PathChain routine = PedroDecodeRoute.buildLaneAuto(follower, startPose);

        robot.stack.resetForStart();
        follower.setStartingPose(startPose);
        follower.setPose(startPose);

        telemetry.addLine("DECODE lane auto ready");
        telemetry.addData("Start", startPose);
        telemetry.addData("LaneExit", laneExit);
        telemetry.addData("GoalApproach", goalApproach);
        telemetry.addData("ClassifierBypass", classifierBypass);
        telemetry.addData("BaseReturn", baseReturn);
        telemetry.update();

        waitForStart();
        if (isStopRequested()) {
            return;
        }

        follower.followPath(routine, true);

        while (opModeIsActive() && follower.isBusy()) {
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
