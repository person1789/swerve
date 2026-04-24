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
import org.firstinspires.ftc.teamcode.Swerve.Input.MotionSmoother;
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
    private MotionSmoother smoother;

    // Declared null; only assigned when SwerveConfig.LIMELIGHT_ENABLED = true.
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

        localizer = new SwerveLocalizer(hwMap);
        swerveDrivetrain = new SwerveDrivetrain(hwMap, logger);
        swerveController = new SwerveController();
        smoother = new MotionSmoother();

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
            double rawVx = -gamepad1.left_stick_y;
            double rawVy = -gamepad1.left_stick_x;
            double rawTurn = -gamepad1.right_stick_x;

            // Apply smoothing in the FIELD frame to prevent phase lag while rotating.
            Vector fieldTarget = smoother.smooth(new Vector(rawVx, rawVy, rawTurn), dt);

            // 4. Run Control Brain (Deadbands / Heading Hold)
            // Use the smoothed field target for heading lock calculations.
            Vector processedFieldTarget = swerveController.update(
                    fieldTarget.x(),
                    fieldTarget.y(),
                    fieldTarget.omega(),
                    heading,
                    dt);

            // 5. Rotate translation to be robot-centric
            double cos = Math.cos(-heading);
            double sin = Math.sin(-heading);
            double robotVx = processedFieldTarget.x() * cos - processedFieldTarget.y() * sin;
            double robotVy = processedFieldTarget.x() * sin + processedFieldTarget.y() * cos;

            // 6. Execute Drivetrain Pipeline (Kinematics -> HW)
            // We pass robotVx/Vy and the turn target. 
            // Note: swerveDrivetrain.setVelocity will still apply its internal smoother, 
            // but since we already smoothed the input, it will be transparent.
            // TODO: In a future cleanup, remove redundant smoothing from SwerveDrivetrain.
            swerveDrivetrain.setVelocity(new Vector(robotVx, robotVy, processedFieldTarget.omega()), dt);

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

        // Limelight telemetry
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
