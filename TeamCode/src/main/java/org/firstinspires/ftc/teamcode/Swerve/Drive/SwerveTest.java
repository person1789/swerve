package org.firstinspires.ftc.teamcode.Swerve.Drive;

import static org.firstinspires.ftc.robotcore.external.navigation.AngleUnit.RADIANS;
import static org.firstinspires.ftc.robotcore.external.navigation.AngleUnit.normalizeRadians;
import static java.lang.Math.abs;
import static java.lang.Math.hypot;
import static java.lang.Math.signum;
import static java.lang.Math.pow;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.arcrobotics.ftclib.controller.PIDFController;
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

import java.util.Locale;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D;
import org.firstinspires.ftc.teamcode.Swerve.Geo.Point;
import org.firstinspires.ftc.teamcode.Swerve.Geo.Pose;
import org.firstinspires.ftc.teamcode.core.HWMap;
import org.firstinspires.ftc.teamcode.core.Logger;

@Config
@TeleOp
public class SwerveTest extends LinearOpMode {
    private ElapsedTime timer = new ElapsedTime();
    Logger logger;
    private HWMap hwMap;
    private SwerveDrivetrain swerveDrivetrain;
    public double BotHeading;
    private GoBildaPinpointDriver odo;
    private Pose2D pos;

    private final PIDFController headingController = new PIDFController(0, 0, 0, 0);
    public static double P = -0.3, I = 0, D = -0.01, F = 0;
    private double targetHeading = 0;
    private boolean headingLocked = false;

    public static double TRANSLATION_SLEW = 1.5;
    public static double ROTATION_SLEW = 3.0;
    public static double PID_SLEW_RATE = 2.0;

    public static double MIN_TRANSLATION_POW = 0.05;
    public static double MIN_ROTATION_POW = 0.08;

    public static double STICK_SCALAR = 0.8;

    private double lastX = 0, lastY = 0, lastTurn = 0, lastPidOut = 0;

    public static double[] MotorScalars = new double[]{-1,1,-1,-1};
    public static double[] Zeros = new double[]{-0.2,3.9,1.4,3};


    @Override
    public void runOpMode() throws InterruptedException {
        telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());
        logger = new Logger(telemetry);

        hwMap = new HWMap(hardwareMap);
        odo = hwMap.getOdo();
        
        odo.setOffsets(10.5, 1, DistanceUnit.CM);
        odo.setEncoderResolution(GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_4_BAR_POD);
        odo.setEncoderDirections(GoBildaPinpointDriver.EncoderDirection.FORWARD, GoBildaPinpointDriver.EncoderDirection.REVERSED);
        odo.resetPosAndIMU();

        swerveDrivetrain = new SwerveDrivetrain(hwMap, logger);

        waitForStart();
        timer.reset();

        while (opModeIsActive()) {
            double voltage = hwMap.getVoltageSensor().getVoltage();
            double dt = timer.seconds();
            timer.reset();

            headingController.setPIDF(P, I, D, F);
            swerveDrivetrain.setOffsets(Zeros);
            swerveDrivetrain.setMotorScaling(MotorScalars);

            if (gamepad1.options) { odo.resetPosAndIMU(); targetHeading = 0; }

            odo.update();
            pos = odo.getPosition();
            BotHeading = -pos.getHeading(RADIANS);

            double rawX = abs(gamepad1.left_stick_x) < 0.05 ? 0 : gamepad1.left_stick_x;
            double rawY = abs(gamepad1.left_stick_y) < 0.05 ? 0 : -gamepad1.left_stick_y;
            double rawTurn = abs(gamepad1.right_stick_x) < 0.05 ? 0 : -gamepad1.right_stick_x;

            double scaledX = JoystickScaler(rawX, STICK_SCALAR);
            double scaledY = JoystickScaler(rawY, STICK_SCALAR);
            double scaledTurn = JoystickScaler(rawTurn, STICK_SCALAR);

            double driveX = SlewRateLimit(scaledX, lastX, TRANSLATION_SLEW, MIN_TRANSLATION_POW, dt);
            double driveY = SlewRateLimit(scaledY, lastY, TRANSLATION_SLEW, MIN_TRANSLATION_POW, dt);
            double driveTurn = SlewRateLimit(scaledTurn, lastTurn, ROTATION_SLEW, MIN_ROTATION_POW, dt);

            lastX = driveX;
            lastY = driveY;
            lastTurn = driveTurn;

            double rawPidCorrection = 0;
            double headingError = 0;
            double driveMag = hypot(driveX, driveY);

            if (abs(driveTurn) > 0) {
                headingLocked = false;
                lastPidOut = 0;
            } else if (driveMag > 0.05) {
                if (!headingLocked) { targetHeading = BotHeading; headingLocked = true; headingController.reset(); }
                headingError = normalizeRadians(targetHeading - BotHeading);
                if (abs(headingError) > 0.01) {
                    rawPidCorrection = headingController.calculate(0, headingError) * (12.4 / voltage);
                }
            }

            double slewedPid = SlewRateLimit(rawPidCorrection, lastPidOut, PID_SLEW_RATE, 0, dt);
            lastPidOut = slewedPid;

            double totalTurn = driveTurn + slewedPid;
            Point rotated = new Point(driveX, driveY).rotate(BotHeading);

            swerveDrivetrain.setPose(new Pose(rotated.x, rotated.y, totalTurn));

            telemetry.addLine("--- SYSTEM ---");
            telemetry.addData("Loop (ms)", String.format(Locale.ENGLISH, "%.2f", dt * 1000));
            telemetry.addData("Hz", String.format(Locale.ENGLISH, "%.1f", 1.0 / dt));

            telemetry.addLine("--- HEADING ---");
            telemetry.addData("Error (deg)", String.format(Locale.ENGLISH, "%.2f", Math.toDegrees(headingError)));
            telemetry.addData("PID Slewed Pwr", String.format(Locale.ENGLISH, "%.2f", slewedPid));

            telemetry.addLine("--- INPUTS ---");
            telemetry.addData("Raw T", String.format(Locale.ENGLISH, "%.2f", rawTurn));
            telemetry.addData("Scaled T", String.format(Locale.ENGLISH, "%.2f", scaledTurn));
            telemetry.addData("Final T", String.format(Locale.ENGLISH, "%.2f", totalTurn));

            swerveDrivetrain.log();
            telemetry.update();
        }
    }

    private double JoystickScaler(double input, double weight) {
        return weight * pow(input, 3) + (1 - weight) * input;
    }

    private double SlewRateLimit(double target, double current, double rate, double minPow, double dt) {
        if (abs(target) <= abs(current) || signum(target) != signum(current)) {
            return target;
        }

        if (current == 0 && target != 0) {
            current = signum(target) * minPow;
        }

        double maxStep = rate * dt;
        double error = target - current;
        return current + Range.clip(error, -maxStep, maxStep);
    }
}
