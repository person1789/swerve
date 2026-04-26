package org.firstinspires.ftc.teamcode.pedroPathing;

import com.acmerobotics.dashboard.config.Config;
import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.BezierLine;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.Path;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import org.firstinspires.ftc.teamcode.Swerve.Core.PoseStorage;

/**
 * First Pedro-based autonomous sanity test for the current swerve robot.
 */
@Config
@Autonomous(name = "Pedro Swerve Line Auto", group = "Pedro")
public class PedroLineAuto extends LinearOpMode {
    public static double forwardDistanceIn = 24.0;

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

        robot.stack.resetForStart();
        follower.setStartingPose(startPose);
        follower.setPose(startPose);

        Pose endPose = new Pose(startPose.getX() + forwardDistanceIn, startPose.getY(), startPose.getHeading());
        Path line = new Path(new BezierLine(startPose, endPose));
        line.setConstantHeadingInterpolation(startPose.getHeading());

        telemetry.addLine("Pedro swerve auto ready");
        telemetry.addData("Start", startPose);
        telemetry.addData("End", endPose);
        telemetry.update();

        waitForStart();
        if (isStopRequested()) {
            return;
        }

        follower.followPath(line, true);

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
