package org.firstinspires.ftc.teamcode.Swerve.OpModes;

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
import org.firstinspires.ftc.teamcode.Swerve.Logic.Control.SwerveController;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Localization.SwerveLocalizer;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Vector;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.SwerveDrivetrain;

@Config
@TeleOp
public class MainTeleOp extends LinearOpMode {
    private SwerveDrivetrain swerveDrivetrain;
    private SwerveController swerveController;
    private SwerveLocalizer localizer;
    
    private Logger logger;
    private HWMap hwMap;
    private GamepadEx gamepadE1;
    private ElapsedTime timer = new ElapsedTime();

    @Override
    public void runOpMode() throws InterruptedException {
        telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());
        gamepadE1 = new GamepadEx(gamepad1);
        logger = new Logger(telemetry);
        hwMap = new HWMap(hardwareMap);
        
        // Initialize Core Systems
        localizer = new SwerveLocalizer(hwMap);
        swerveDrivetrain = new SwerveDrivetrain(hwMap, logger);
        swerveController = new SwerveController();

        waitForStart();
        timer.reset();

        while (opModeIsActive()) {
            double dt = timer.seconds();
            timer.reset();

            // 1. Update Input Handling
            logger.updateLoggingLevel(gamepadE1.getButton(GamepadKeys.Button.LEFT_BUMPER));
            if (gamepadE1.getButton(GamepadKeys.Button.START)) {
                localizer.resetHeading();
            }

            // 2. Localization (Fail-Safe Fusion)
            localizer.update(swerveDrivetrain.getActualVelocity(), dt);
            Vector currentPose = localizer.getPose(); // 3D Vector [x, y, heading]
            double heading = currentPose.omega();

            // 3. Process Driver Intent (Field-Centric)
            // Raw joystick values (-1 to 1) are passed. SwerveController handles deadbands/curves.
            double vx = -gamepad1.left_stick_y;
            double vy = -gamepad1.left_stick_x;
            double turn = -gamepad1.right_stick_x;

            // Rotate translation to be field-centric
            Vector rawTranslation = new Vector(vx, vy).rotate(-heading);
            
            // 4. Run Control Brain (Heading Hold / Snap)
            Vector chassisSpeeds = swerveController.update(
                rawTranslation.x(), 
                rawTranslation.y(), 
                turn, 
                heading, 
                dt
            );

            // 5. Execute Drivetrain Pipeline (Smoothing -> Kinematics -> HW)
            swerveDrivetrain.setVelocity(chassisSpeeds, dt);

            // 6. Telemetry
            logUpdate(heading, dt);
            telemetry.update();
        }
    }

    private void logUpdate(double heading, double dt) {
        logger.log("Loop Time (ms)", dt * 1000.0, Logger.LogLevels.PRODUCTION);
        logger.log("Heading (deg)", Math.toDegrees(heading), Logger.LogLevels.PRODUCTION);
        swerveDrivetrain.log();
    }
}
