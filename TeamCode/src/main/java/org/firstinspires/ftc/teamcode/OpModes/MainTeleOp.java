package org.firstinspires.ftc.teamcode.OpModes;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.arcrobotics.ftclib.gamepad.GamepadEx;
import com.arcrobotics.ftclib.gamepad.GamepadKeys;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.Swerve.Core.Logger;
import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Localization.SwerveLocalizer;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Vector;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.SwerveDrivetrain;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.Limelight.LimelightLocalizer;
import org.firstinspires.ftc.teamcode.pedroPathing.PedroUnifiedSwerveStack;

@Config
@TeleOp
public class MainTeleOp extends LinearOpMode {
    private PedroUnifiedSwerveStack sharedStack;

    // Declared null; only assigned when SwerveConfig.LIMELIGHT_ENABLED = true.
    private LimelightLocalizer limelightLocalizer = null;

    private Logger logger;
    private GamepadEx gamepadE1;
    private ElapsedTime timer = new ElapsedTime();
    private boolean previousStartPressed;

    @Override
    public void runOpMode() throws InterruptedException {
        if (SwerveConfig.DASHBOARD_ENABLED) {
            telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());
        }
        gamepadE1 = new GamepadEx(gamepad1);
        logger = new Logger(telemetry);
        sharedStack = new PedroUnifiedSwerveStack(hardwareMap, logger);
        sharedStack.restorePoseFromStorage();

        if (SwerveConfig.LIMELIGHT_ENABLED) {
            limelightLocalizer = new LimelightLocalizer(hardwareMap);
        }

        waitForStart();
        timer.reset();
        sharedStack.resetForStart();
        previousStartPressed = false;

        while (opModeIsActive()) {
            double measuredDt = timer.seconds();
            timer.reset();
            double dt = sharedStack.beginTeleOpLoop(measuredDt);

            // 1. Update Input Handling
            logger.updateLoggingLevel(gamepadE1.getButton(GamepadKeys.Button.LEFT_BUMPER));
            boolean startPressed = gamepadE1.getButton(GamepadKeys.Button.START);
            if (startPressed && !previousStartPressed) {
                sharedStack.resetHeadingForTeleOp();
            }
            previousStartPressed = startPressed;
            SwerveLocalizer localizer = sharedStack.getLocalizer();
            SwerveDrivetrain swerveDrivetrain = sharedStack.getDrivetrain();

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
            double rawVx = -gamepad1.left_stick_x;
            double rawVy = -gamepad1.left_stick_y;
            double rawTurn = -gamepad1.right_stick_x;

            // 4. Run the shared teleop motion path on the common stack.
            sharedStack.driveTeleOp(rawVx, rawVy, rawTurn);

            // 6. Telemetry
            logUpdate(heading, dt);
            telemetry.update();
        }
    }

    private void logUpdate(double heading, double dt) {
        SwerveLocalizer localizer = sharedStack.getLocalizer();
        SwerveDrivetrain swerveDrivetrain = sharedStack.getDrivetrain();
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
