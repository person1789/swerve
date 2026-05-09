package org.firstinspires.ftc.teamcode.OpModes;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.Swerve.Core.LoopTimeEstimator;
import org.firstinspires.ftc.teamcode.Swerve.Core.Logger;
import org.firstinspires.ftc.teamcode.Swerve.Core.PoseStorage;
import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Localization.SwerveLocalizer;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.HWMap;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.SwerveDrivetrain;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.Limelight.LimelightLocalizer;

@Config
@TeleOp
public class MainTeleOp extends LinearOpMode {
    private HWMap hwMap;
    private SwerveLocalizer localizer;
    private SwerveDrivetrain drivetrain;
    private LimelightLocalizer limelightLocalizer = null;
    private Logger logger;
    private final ElapsedTime timer = new ElapsedTime();
    private final LoopTimeEstimator loopTimeEstimator = new LoopTimeEstimator();
    private boolean previousStartPressed;
    private double lastDt = SwerveConfig.LOOP_TIME_SEC;
    private int loopCounter = 0;

    @Override
    public void runOpMode() throws InterruptedException {
        if (SwerveConfig.DASHBOARD_ENABLED) {
            telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());
        }
        logger = new Logger(telemetry);
        hwMap = new HWMap(hardwareMap);
        localizer = new SwerveLocalizer(hwMap);
        drivetrain = new SwerveDrivetrain(hwMap, logger);
        restorePoseFromStorage();

        if (SwerveConfig.LIMELIGHT_ENABLED) {
            limelightLocalizer = new LimelightLocalizer(hardwareMap);
        }

        waitForStart();
        timer.reset();
        loopTimeEstimator.reset();
        previousStartPressed = false;

        while (opModeIsActive()) {
            double dt = beginLoop();
            boolean loggingStateChanged = logger.updateLoggingLevel(gamepad1.left_bumper);
            boolean startPressed = gamepad1.start;
            if (startPressed && !previousStartPressed) {
                localizer.resetHeading();
            }
            previousStartPressed = startPressed;

            if (SwerveConfig.LIMELIGHT_ENABLED && limelightLocalizer != null) {
                double angularVelDegS = Math.toDegrees(drivetrain.getActualVelocity().omega());
                limelightLocalizer.update(Math.toDegrees(localizer.getHeading()), angularVelDegS);
                if (limelightLocalizer.hasVisionUpdate()) {
                    localizer.applyVisionUpdate(
                            limelightLocalizer.getVisionPose(),
                            limelightLocalizer.getTrustFactor());
                }
            }

            double heading = localizer.getHeading();
            driveFromGamepad(heading, dt);
            loopCounter++;
            if (loggingStateChanged || shouldEmitTelemetry(loopCounter, SwerveConfig.TELEOP_TELEMETRY_INTERVAL_LOOPS)) {
                logUpdate(heading, dt);
                telemetry.update();
            }
        }
    }

    private double beginLoop() {
        hwMap.clearBulkCache();
        drivetrain.refreshSensors();
        localizer.refreshSensors();
        double measuredDt = timer.seconds();
        timer.reset();
        lastDt = loopTimeEstimator.update(measuredDt);
        localizer.update(drivetrain.getActualVelocity(), lastDt);
        return lastDt;
    }

    private void restorePoseFromStorage() {
        Pose storedPose = PoseStorage.getCurrentPose();
        if (storedPose != null) {
            localizer.setPose(storedPose.toVector());
            PoseStorage.clear();
        }
    }

    private void driveFromGamepad(double heading, double dt) {
        double fieldForward = -gamepad1.left_stick_y;
        double fieldStrafe = -gamepad1.left_stick_x;
        double turn = -gamepad1.right_stick_x;

        double translationMagnitude = Math.hypot(fieldForward, fieldStrafe);
        if (translationMagnitude < SwerveConfig.INPUT_DEADBAND) {
            fieldForward = 0.0;
            fieldStrafe = 0.0;
        } else {
            double scaledMagnitude = (translationMagnitude - SwerveConfig.INPUT_DEADBAND)
                    / (1.0 - SwerveConfig.INPUT_DEADBAND);
            double ratio = scaledMagnitude / translationMagnitude;
            fieldForward *= ratio;
            fieldStrafe *= ratio;
        }

        if (Math.abs(turn) < SwerveConfig.INPUT_DEADBAND) {
            turn = 0.0;
        }

        double cos = Math.cos(-heading);
        double sin = Math.sin(-heading);
        double robotForward = fieldForward * cos - fieldStrafe * sin;
        double robotStrafe = fieldForward * sin + fieldStrafe * cos;
        drivetrain.setVelocity(robotForward, robotStrafe, turn, dt);
    }

    private void logUpdate(double heading, double dt) {
        if (!(logger.PRODUCTION() || logger.DEBUG() || logger.DRIVER_DATA())) {
            return;
        }

        if (!logger.DEBUG() && !logger.PRODUCTION()) {
            return;
        }

        logger.log("Loop Time (ms)", dt * 1000.0, Logger.LogLevels.PRODUCTION);
        logger.log("Heading (deg)", Math.toDegrees(heading), Logger.LogLevels.PRODUCTION);
        logger.log("Pinpoint Raw X (in)", localizer.getRawPinpointXInches(), Logger.LogLevels.PRODUCTION);
        logger.log("Pinpoint Raw Y (in)", localizer.getRawPinpointYInches(), Logger.LogLevels.PRODUCTION);
        logger.log("Pinpoint Raw Heading (deg)", Math.toDegrees(localizer.getRawPinpointHeadingRadians()), Logger.LogLevels.PRODUCTION);
        logger.log("Pinpoint Used", localizer.isUsingPinpoint() ? 1.0 : 0.0, Logger.LogLevels.PRODUCTION);
        logger.log("Pinpoint Invalid Loops", localizer.getConsecutiveInvalidPinpointLoops(), Logger.LogLevels.PRODUCTION);
        drivetrain.log();

        if (SwerveConfig.LIMELIGHT_ENABLED && limelightLocalizer != null) {
            logger.log("LL Tag Count",  limelightLocalizer.getLastTagCount(),              Logger.LogLevels.PRODUCTION);
            logger.log("LL Has Update", limelightLocalizer.hasVisionUpdate() ? 1.0 : 0.0, Logger.LogLevels.PRODUCTION);
            logger.log("LL Trust",      limelightLocalizer.getTrustFactor(),               Logger.LogLevels.PRODUCTION);
            if (limelightLocalizer.getVisionPose() != null) {
                logger.log("LL Vision X (in)", limelightLocalizer.getVisionPose().x(), Logger.LogLevels.PRODUCTION);
                logger.log("LL Vision Y (in)", limelightLocalizer.getVisionPose().y(), Logger.LogLevels.PRODUCTION);
            }
        }
    }

    private boolean shouldEmitTelemetry(int currentLoop, int intervalLoops) {
        return currentLoop % Math.max(1, intervalLoops) == 0;
    }
}
