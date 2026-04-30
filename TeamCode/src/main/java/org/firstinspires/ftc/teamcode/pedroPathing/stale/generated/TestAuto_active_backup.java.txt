package org.firstinspires.ftc.teamcode.pedroPathing;

import com.acmerobotics.dashboard.config.Config;
import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.PathChain;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import org.firstinspires.ftc.teamcode.Swerve.Core.PoseStorage;

// @sim
@Config
@Autonomous(name = "test Auto", group = "Pedro")
public class TestAuto extends LinearOpMode {
    public static String SIM_OPMODE_NAME = "TestAuto";
    public static boolean useStoredPose = true;

    public static Pose buildSimStartPose() {
        // @sim-start-start
        Pose startPose = PedroDecodeRoute.startPose(PedroStartPose.RED_BASE_CORNER);
// @sim-start-end
        return startPose;
    }

    public static PathChain buildSimPath(Follower follower, Pose startPose) {
        // @path-start
PathChain route = PedroBlockRouteBuilder.build(
                follower,
                startPose,
                PedroBlockCommand.straight(30, 40.55, 0, 0.9)
        );
// @path-end
        return route;
    }

    @Override
    public void runOpMode() throws InterruptedException {
        PedroSwerveFactory.PedroRobot robot = PedroSwerveFactory.createRobot(hardwareMap, null);
        Follower follower = robot.follower;

        PedroSwerveFactory.applyLiveTuning(follower);

        Pose startPose = buildSimStartPose();
        org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose storedPose = PoseStorage.getCurrentPose();
        if (useStoredPose && storedPose != null) {
            startPose = new Pose(storedPose.x, storedPose.y, storedPose.heading);
        }

        PathChain route = buildSimPath(follower, startPose);
        robot.stack.resetForStart();
        follower.setStartingPose(startPose);
        follower.setPose(startPose);

        telemetry.addLine("Pedro auto ready");
        telemetry.addData("Start", startPose);
        telemetry.update();

        waitForStart();
        if (isStopRequested()) {
            return;
        }

        follower.followPath(route, true);

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
