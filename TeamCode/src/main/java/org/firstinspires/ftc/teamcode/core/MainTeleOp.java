package org.firstinspires.ftc.teamcode.core;

import static org.firstinspires.ftc.robotcore.external.navigation.AngleUnit.RADIANS;
import static org.firstinspires.ftc.robotcore.external.navigation.AngleUnit.normalizeRadians;

import static java.lang.Math.abs;
import static java.lang.Math.hypot;
import static java.lang.Math.pow;
import static java.lang.Math.signum;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import org.firstinspires.ftc.robotcore.external.Telemetry;

import com.arcrobotics.ftclib.controller.PIDFController;
import com.arcrobotics.ftclib.gamepad.GamepadEx;
import com.arcrobotics.ftclib.gamepad.GamepadKeys;
import com.arcrobotics.ftclib.util.Timing;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Point;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.SwerveDrivetrain;

import java.util.concurrent.TimeUnit;

@Config
@TeleOp
public class MainTeleOp extends LinearOpMode {
    private SwerveDrivetrain swerveDrivetrain;

    private Pose2D pos;

    private Logger logger;
    private HWMap hwMap;
    private RobotSettings robotSettings;
    private GamepadEx gamepadE1;

    private Pinpoint pinpoint;

    private Timing.Timer loopTimer;

    private ElapsedTime timer = new ElapsedTime();
    public double BotHeading;

    private final PIDFController headingController = new PIDFController(0, 0, 0, 0);
    public static double P = -0.3, I = 0, D = -0.01, F = 0;
    private double targetHeading = 0;
    private boolean headingLocked = false;

    public static double TRANSLATION_SLEW = 1.5;
    public static double ROTATION_SLEW = 3.0;
    public static double PID_SLEW_RATE = 100.0; // Effectively disabled at this high rate

    public static double MIN_TRANSLATION_POW = 0.05;
    public static double MIN_ROTATION_POW = 0.08;

    public static double STICK_SCALAR = 0.9;

    private double lastX = 0, lastY = 0, lastTurn = 0, lastPidOut = 0;


    public static double[] MotorScalars = new double[]{-1,1,-1,-1};
    public static double[] Zeros = new double[]{-0.2,3.9,1.4,3};

    @Override
    public void runOpMode() throws InterruptedException{

        telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());
        telemetry.setDisplayFormat(Telemetry.DisplayFormat.HTML);
        gamepadE1 = new GamepadEx(gamepad1);

        logger = new Logger(telemetry);
        hwMap = new HWMap(hardwareMap);
        robotSettings = RobotSettings.load();
        pinpoint = new Pinpoint(hwMap, robotSettings);

        if(PoseStorage.currentPose != null) {
            pinpoint.setPosition(new Pose2D(DistanceUnit.INCH, PoseStorage.currentPose.getX(),PoseStorage.currentPose.getY(), AngleUnit.DEGREES, PoseStorage.currentPose.getHeading()));
        }

        loopTimer = new Timing.Timer(300000000, TimeUnit.MILLISECONDS); double botHeading;
        if(robotSettings.alliance == RobotSettings.Alliance.RED) {
            logger.log("<b><u><font color='red'>ALLIANCE</font></u></b>", robotSettings.alliance, Logger.LogLevels.PRODUCTION);
        }else{
            logger.log("<b><u><font color='blue'>ALLIANCE</font></u></b>", robotSettings.alliance, Logger.LogLevels.PRODUCTION);
        }

        hwMap = new HWMap(hardwareMap);
        swerveDrivetrain = new SwerveDrivetrain(hwMap, logger);

        waitForStart();
        timer.reset();

        telemetry.update();
        waitForStart();
        while (opModeIsActive()) {
            loopTimer.start();

            double voltage = hwMap.getVoltageSensor().getVoltage();

            logger.updateLoggingLevel(gamepadE1.getButton(GamepadKeys.Button.LEFT_BUMPER));

            if (gamepadE1.getButton(GamepadKeys.Button.START)) {
                pinpoint.resetIMU();
            }

            pinpoint.update();
            pos = pinpoint.getPos();
            if(robotSettings.alliance.getGoalPos().equals(RobotSettings.Alliance.BLUE.getGoalPos())) {
                botHeading = -pos.getHeading(RADIANS);
            }
            else {
                botHeading = -pos.getHeading(RADIANS);
            }

            double dt = timer.seconds();
            timer.reset();

            headingController.setPIDF(P, I, D, F);
            swerveDrivetrain.setOffsets(Zeros);
            swerveDrivetrain.setMotorScaling(MotorScalars);


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
            Point drivevector = new Point(driveX, driveY);

            swerveDrivetrain.setPose(new Pose(new Point(drivevector.x, drivevector.y).rotate(botHeading), totalTurn), dt);

            logUpdate(botHeading, voltage);

            telemetry.update();
        }

    }
    private void logUpdate(double botHeading, double voltage){
        logger.log("battery voltage", voltage, Logger.LogLevels.DEBUG);
        logger.log("loop time", loopTimer.elapsedTime(), Logger.LogLevels.DEBUG);
        logger.log("Bot Heading", botHeading, Logger.LogLevels.DEBUG);
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