package org.firstinspires.ftc.teamcode.OpModes;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.teamcode.Swerve.Core.PIDController;
import org.firstinspires.ftc.teamcode.Swerve.Core.SlewRateLimiter;
import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Core.MathUtil;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.HWMap;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.SwerveDrivetrain;

@Config
@TeleOp
public class MainTeleOp extends LinearOpMode {
    private final ElapsedTime loopTimer = new ElapsedTime();

    // Joystick scaling exponent. 1.0 = linear, 2.0 = squared, 3.0 = cubic.
    // Higher values give finer control at low stick deflection.
    public static double JOYSTICK_SCALAR = 2.0;

    // Slew rate limit: maximum change in power per second.
    // 3.0 means 0 to full power takes ~0.33 seconds.
    public static double TRANSLATION_SLEW_RATE = 3.0;
    public static double TURN_SLEW_RATE = 4.0;

    private HWMap hwMap;
    private SwerveDrivetrain drivetrain;
    private boolean previousStartPressed = false;

    private SlewRateLimiter forwardLimiter;
    private SlewRateLimiter strafeLimiter;
    private SlewRateLimiter turnLimiter;
    
    private double teleopHeadingOffset = 0.0;

    private PIDController teleopHeadingController;
    private double targetHeadingRadians = 0.0;
    private boolean isHeadingLocked = false;

    @Override
    public void runOpMode() throws InterruptedException {
        if (SwerveConfig.DASHBOARD_ENABLED) {
            telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());
        }

        hwMap = new HWMap(hardwareMap);
        drivetrain = new SwerveDrivetrain(hwMap);

        forwardLimiter = new SlewRateLimiter(TRANSLATION_SLEW_RATE);
        strafeLimiter = new SlewRateLimiter(TRANSLATION_SLEW_RATE);
        turnLimiter = new SlewRateLimiter(TURN_SLEW_RATE);

        teleopHeadingController = new PIDController(
                SwerveConfig.TELEOP_HEADING_P, 0.0, SwerveConfig.TELEOP_HEADING_D);
        teleopHeadingController.enableContinuousInput(-Math.PI, Math.PI);

        waitForStart();
        loopTimer.reset();

        while (opModeIsActive()) {
            hwMap.clearBulkCache();
            drivetrain.read();

            double dt = Math.max(1e-3, loopTimer.seconds());
            loopTimer.reset();

            hwMap.odo.update();
            double currentHeading = hwMap.odo.getHeading(AngleUnit.RADIANS);
            
            // Fallback to IMU if Pinpoint isn't returning valid data
            if (Double.isNaN(currentHeading)) {
                currentHeading = hwMap.imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.RADIANS);
            }

            if (gamepad1.start && !previousStartPressed) {
                teleopHeadingOffset = -currentHeading;
            }
            previousStartPressed = gamepad1.start;

            driveFromGamepad(currentHeading + teleopHeadingOffset, dt);

            telemetry.addData("state", drivetrain.getState());
            telemetry.addData("loop ms", dt * 1000.0);
            telemetry.update();
        }
    }

    private void driveFromGamepad(double headingRadians, double dt) {
        double rawForward = applyDeadband(-gamepad1.left_stick_y);
        double rawStrafe = applyDeadband(-gamepad1.left_stick_x);
        double rawTurn = applyDeadband(-gamepad1.right_stick_x);

        // Joystick scaling: raises the input to the configured power while preserving sign.
        // This gives finer control at low stick deflections while still allowing full power.
        double scaledForward = joystickScalar(rawForward, JOYSTICK_SCALAR);
        double scaledStrafe = joystickScalar(rawStrafe, JOYSTICK_SCALAR);
        double scaledTurn = joystickScalar(rawTurn, JOYSTICK_SCALAR);

        double turn;
        if (Math.abs(rawTurn) > 0.0) {
            // Driver is manually turning
            isHeadingLocked = false;
            targetHeadingRadians = headingRadians;
            turn = turnLimiter.calculate(scaledTurn);
        } else {
            // Driver released turn stick - engage heading lock if settled
            turnLimiter.calculate(0.0); // Keep limiter synced
            double angularVelocity = hwMap.imu.getRobotAngularVelocity(AngleUnit.RADIANS).zRotationRate;
            
            if (!isHeadingLocked && Math.abs(angularVelocity) < SwerveConfig.TELEOP_HEADING_MAX_ANGULAR_VELOCITY_RAD_S) {
                isHeadingLocked = true;
                targetHeadingRadians = headingRadians;
                teleopHeadingController.reset();
            }

            if (isHeadingLocked) {
                teleopHeadingController.setPID(SwerveConfig.TELEOP_HEADING_P, 0.0, SwerveConfig.TELEOP_HEADING_D);
                turn = teleopHeadingController.calculate(headingRadians, targetHeadingRadians, dt);
            } else {
                turn = 0.0;
            }
        }

        double fieldForward = forwardLimiter.calculate(scaledForward);
        double fieldStrafe = strafeLimiter.calculate(scaledStrafe);

        // Field-centric rotation
        double cos = Math.cos(-headingRadians);
        double sin = Math.sin(-headingRadians);
        double robotForward = fieldForward * cos - fieldStrafe * sin;
        double robotStrafe = fieldForward * sin + fieldStrafe * cos;

        drivetrain.set(robotForward, robotStrafe, turn, dt);
        drivetrain.write(dt);
    }

    /**
     * Applies a power curve to the joystick input while preserving its sign.
     * @param input Raw joystick value in [-1, 1].
     * @param scalar Exponent for the curve.
     * @return Scaled value in [-1, 1].
     */
    private static double joystickScalar(double input, double scalar) {
        return Math.signum(input) * Math.pow(Math.abs(input), scalar);
    }

    private static double applyDeadband(double value) {
        return Math.abs(value) < SwerveConfig.INPUT_DEADBAND ? 0.0 : value;
    }
}
