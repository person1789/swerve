package org.firstinspires.ftc.teamcode.OpModes;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.Range;
import com.qualcomm.robotcore.hardware.IMU;
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.SwerveDrivetrain;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.SlewRateLimiter;
import com.qualcomm.robotcore.util.ElapsedTime;

@TeleOp
public class FieldCentricDT extends LinearOpMode {

    private SwerveDrivetrain drivetrain;
    private SlewRateLimiter xLimiter;
    private SlewRateLimiter yLimiter;
    private SlewRateLimiter turnLimiter;

    private ElapsedTime loopTimer = new ElapsedTime();

    @Override
    public void runOpMode() throws InterruptedException {
        drivetrain = new SwerveDrivetrain(hardwareMap);

        xLimiter = new SlewRateLimiter(4.0);
        yLimiter = new SlewRateLimiter(4.0);
        turnLimiter = new SlewRateLimiter(4.0);

        IMU imu = hardwareMap.get(IMU.class, "imu");

        IMU.Parameters parameters = new IMU.Parameters(new RevHubOrientationOnRobot(
                RevHubOrientationOnRobot.LogoFacingDirection.LEFT,
                RevHubOrientationOnRobot.UsbFacingDirection.DOWN));
        imu.initialize(parameters);

        waitForStart();

        if (gamepad1.options) {
            imu.resetYaw();
        }

        if (isStopRequested()) return;

        while (opModeIsActive()) {

            double loopTime = loopTimer.milliseconds();
            loopTimer.reset();

            double x = gamepad1.left_stick_x;
            double y = -gamepad1.left_stick_y;
            double turn = gamepad1.right_stick_x;

            x = Range.clip(x, -1.0, 1.0);
            y = Range.clip(y, -1.0, 1.0);
            turn = Range.clip(turn, -1.0, 1.0);

            x = xLimiter.calculate(x);
            y = yLimiter.calculate(y);
            turn = turnLimiter.calculate(turn);

            double botHeading = imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.RADIANS);

            double rotX = x * Math.cos(-botHeading) - y * Math.sin(-botHeading);
            double rotY = x * Math.sin(-botHeading) + y * Math.cos(-botHeading);

            boolean noInput =
                    Math.abs(x) < 0.05 &&
                            Math.abs(y) < 0.05 &&
                            Math.abs(turn) < 0.05;

            if (noInput) drivetrain.setLocked(true);
            else drivetrain.setLocked(false);

            drivetrain.updateOffsets();
            drivetrain.set(new Pose(rotX, rotY, turn));
            drivetrain.write();
            drivetrain.updateModules();


            telemetry.addData("Loop Time (ms)", loopTime);
            telemetry.update();
        }


        drivetrain.stop();
    }
}