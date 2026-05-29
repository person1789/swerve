package org.firstinspires.ftc.teamcode.OpModes;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.HWMap;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.SwerveDrivetrain;

@Config
@TeleOp
public class MainTeleOp extends LinearOpMode {
    private final ElapsedTime loopTimer = new ElapsedTime();

    private HWMap hwMap;
    private SwerveDrivetrain drivetrain;
    private boolean previousStartPressed = false;

    @Override
    public void runOpMode() throws InterruptedException {
        if (SwerveConfig.DASHBOARD_ENABLED) {
            telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());
        }

        hwMap = new HWMap(hardwareMap);
        drivetrain = new SwerveDrivetrain(hwMap);

        waitForStart();
        loopTimer.reset();

        while (opModeIsActive()) {
            hwMap.clearBulkCache();
            drivetrain.read();

            double dt = Math.max(1e-3, loopTimer.seconds());
            loopTimer.reset();

            if (gamepad1.start && !previousStartPressed) {
                hwMap.imu.resetYaw();
            }
            previousStartPressed = gamepad1.start;

            driveFromGamepad(hwMap.imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.RADIANS), dt);

            telemetry.addData("state", drivetrain.getState());
            telemetry.addData("loop ms", dt * 1000.0);
            telemetry.update();
        }
    }

    private void driveFromGamepad(double headingRadians, double dt) {
        double fieldForward = applyDeadband(-gamepad1.left_stick_y);
        double fieldStrafe = applyDeadband(-gamepad1.left_stick_x);
        double turn = applyDeadband(-gamepad1.right_stick_x);

        double cos = Math.cos(-headingRadians);
        double sin = Math.sin(-headingRadians);
        double robotForward = fieldForward * cos - fieldStrafe * sin;
        double robotStrafe = fieldForward * sin + fieldStrafe * cos;

        drivetrain.set(robotForward, robotStrafe, turn, dt);
        drivetrain.write(dt);
    }

    private static double applyDeadband(double value) {
        return Math.abs(value) < SwerveConfig.INPUT_DEADBAND ? 0.0 : value;
    }
}
