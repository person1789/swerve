package org.firstinspires.ftc.teamcode.OpModes;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.arcrobotics.ftclib.gamepad.GamepadEx;
import com.arcrobotics.ftclib.gamepad.GamepadKeys;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.Swerve.Hardware.HWMap;
import org.firstinspires.ftc.teamcode.Swerve.Core.Logger;
import org.firstinspires.ftc.teamcode.Swerve.Core.LoopTimeEstimator;
import org.firstinspires.ftc.teamcode.Swerve.Core.PoseStorage;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose;
import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Control.SwerveController;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Localization.SwerveLocalizer;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Vector;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.SwerveDrivetrain;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.Limelight.LimelightLocalizer;

@Config
@TeleOp
public class MainTeleOp extends LinearOpMode {
    private SwerveDrivetrain swerveDrivetrain;
    private SwerveController swerveController;
    private SwerveLocalizer localizer;

    // Declared null; only assigned when SwerveConfig.LIMELIGHT_ENABLED = true.
    // This ensures no hardware map lookup occurs if the camera is absent.
    private LimelightLocalizer limelightLocalizer = null;

    private Logger logger;
    private HWMap hwMap;
    private GamepadEx gamepadE1;
    private ElapsedTime timer = new ElapsedTime();
    private final LoopTimeEstimator loopTimeEstimator = new LoopTimeEstimator();
    private boolean previousStartPressed;

    @Override
    public void runOpMode() throws InterruptedException {
        if (SwerveConfig.DASHBOARD_ENABLED) {
            telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());
        }
        gamepadE1 = new GamepadEx(gamepad1);
        logger = new Logger(telemetry);
        hwMap = new HWMap(hardwareMap);

        // Initialize Core Systems
        localizer = new SwerveLocalizer(hwMap);
        swerveDrivetrain = new SwerveDrivetrain(hwMap, logger);
        swerveController = new SwerveController();

        // Optional: Limelight vision relocalization.
        // Completely skipped (no hardware map call) if LIMELIGHT_ENABLED = false.
        if (SwerveConfig.LIMELIGHT_ENABLED) {
            limelightLocalizer = new LimelightLocalizer(hardwareMap);
        }

        Pose storedPose = PoseStorage.getCurrentPose();
        if (storedPose != null) {
            localizer.setPose(storedPose.toVector());
            PoseStorage.clear();
        }

        waitForStart();
        timer.reset();
        loopTimeEstimator.reset();
        previousStartPressed = false;

        while (opModeIsActive()) {
            double measuredDt = timer.seconds();
            timer.reset();
            double dt = loopTimeEstimator.update(measuredDt);

            // 1. Update Input Handling
            logger.updateLoggingLevel(gamepadE1.getButton(GamepadKeys.Button.LEFT_BUMPER));
            boolean startPressed = gamepadE1.getButton(GamepadKeys.Button.START);
            if (startPressed && !previousStartPressed) {
                localizer.resetHeading();
                swerveController.resetHeading(0.0);
                swerveDrivetrain.resetSmoother();
            }
            previousStartPressed = startPressed;

            // 2. Localization (Fail-Safe Fusion)
            localizer.update(swerveDrivetrain.getActualVelocity(), dt);

            // 2a. Vision relocalization (only runs when LIMELIGHT_ENABLED = true).
            // All Limelight code is isolated in this block — nothing outside it
            // references LimelightLocalizer, preserving full subsystem isolation.
            if (SwerveConfig.LIMELIGHT_ENABLED && limelightLocalizer != null) {
                // omega() from the velocity observer is rad/s; convert for MT2 gate.
                double angularVelDegS = Math.toDegrees(swerveDrivetrain.getActualVelocity().omega());
                limelightLocalizer.update(Math.toDegrees(localizer.getHeading()), angularVelDegS);
                if (limelightLocalizer.hasVisionUpdate()) {
                    localizer.applyVisionUpdate(
                            limelightLocalizer.getVisionPose(),
                            limelightLocalizer.getTrustFactor());
                }
            }

            Vector currentPose = localizer.getPose(); // [x, y, heading]
            double heading = currentPose.omega();

            // 3. Process Driver Intent (Field-Centric)
            // Raw joystick values (-1 to 1) are passed. SwerveController handles
            // deadbands/curves.
            double vx = -gamepad1.left_stick_y;
            double vy = -gamepad1.left_stick_x;
            double turn = -gamepad1.right_stick_x;

            // Rotate translation to be field-centric
            double cos = Math.cos(-heading);
            double sin = Math.sin(-heading);
            double rawTranslationX = vx * cos - vy * sin;
            double rawTranslationY = vx * sin + vy * cos;

            // 4. Run Control Brain (Heading Hold / Snap)
            Vector chassisSpeeds = swerveController.update(
                    rawTranslationX,
                    rawTranslationY,
                    turn,
                    heading,
                    dt);

            // 5. Execute Drivetrain Pipeline (Smoothing -> Kinematics -> HW)
            swerveDrivetrain.setVelocity(chassisSpeeds, dt);

            // 6. Telemetry
            logUpdate(heading, dt);
            telemetry.update();
        }
    }

    private void logUpdate(double heading, double dt) {
        Vector rawPinpointPose = localizer.getRawPinpointPose();
        logger.log("Loop Time (ms)", dt * 1000.0, Logger.LogLevels.PRODUCTION);
        logger.log("Heading (deg)", Math.toDegrees(heading), Logger.LogLevels.PRODUCTION);
        logger.log("Pinpoint Raw X (in)", rawPinpointPose.x(), Logger.LogLevels.PRODUCTION);
        logger.log("Pinpoint Raw Y (in)", rawPinpointPose.y(), Logger.LogLevels.PRODUCTION);
        logger.log("Pinpoint Raw Heading (deg)", Math.toDegrees(rawPinpointPose.omega()), Logger.LogLevels.PRODUCTION);
        logger.log("Pinpoint Used", localizer.isUsingPinpoint() ? 1.0 : 0.0, Logger.LogLevels.PRODUCTION);
        logger.log("Pinpoint Invalid Loops", localizer.getConsecutiveInvalidPinpointLoops(), Logger.LogLevels.PRODUCTION);
        swerveDrivetrain.log();

        // Limelight telemetry — only emitted when the subsystem is active.
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
}
