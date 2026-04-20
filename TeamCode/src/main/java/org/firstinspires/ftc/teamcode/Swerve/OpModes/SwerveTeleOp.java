package org.firstinspires.ftc.teamcode.Swerve.OpModes;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.Swerve.Core.MathUtil;
import org.firstinspires.ftc.teamcode.Swerve.Core.PIDController;
import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Point;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose;
import org.firstinspires.ftc.teamcode.Swerve.Input.JoystickScaling;
import org.firstinspires.ftc.teamcode.Swerve.Input.MotionSmoother;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.SwerveDrivetrain;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Localization.SwerveLocalizer;
import org.firstinspires.ftc.teamcode.core.Logger;

/**
 * SwerveTeleOp — Phase E
 * 
 * High-performance Field-Centric drive OpMode.
 * 
 * Controls:
 * - Left Stick: Translation (Field-Centric)
 * - Right Stick: Rotation
 * - D-Pad: Heading Snap (Up=0, Left=90, Down=180, Right=270)
 * - Options Button: Reset Field-Forward Orientation
 */
@TeleOp(name = "Swerve TeleOp", group = "Swerve")
public class SwerveTeleOp extends LinearOpMode {

    private SwerveDrivetrain drivetrain;
    private SwerveLocalizer localizer;
    private MotionSmoother smoother;
    private JoystickScaling scaling;
    private PIDController snapController;
    
    private double targetHeading = 0.0;
    private boolean isSnapping = false;

    private final ElapsedTime timer = new ElapsedTime();

    @Override
    public void runOpMode() throws InterruptedException {
        // Initialization
        HWMap hwMap = new HWMap(hardwareMap);
        Logger logger = new Logger(telemetry);
        
        drivetrain = new SwerveDrivetrain(hwMap, logger);
        localizer = new SwerveLocalizer(hwMap);
        smoother = new MotionSmoother();
        scaling = new JoystickScaling();
        
        snapController = new PIDController(SwerveConfig.SNAP_P, SwerveConfig.SNAP_I, SwerveConfig.SNAP_D);
        
        telemetry.addData("Status", "Initialized - Phase E (Localization)");
        telemetry.update();

        waitForStart();
        timer.reset();

        while (opModeIsActive()) {
            double dt = timer.seconds();
            timer.reset();

            // Update localizer
            localizer.update();
            double currentHeading = localizer.getHeading();

            // 1. Read Inputs
            double drive = -gamepad1.left_stick_y;
            double strafe = -gamepad1.left_stick_x;
            double turn = -gamepad1.right_stick_x;

            // 2. Heading Reset (Options Button)
            if (gamepad1.options) {
                localizer.resetHeading();
                targetHeading = 0;
                isSnapping = false;
            }

            // 3. Heading Snap (D-pad)
            if (gamepad1.dpad_up)    { targetHeading = 0; isSnapping = true; }
            if (gamepad1.dpad_left)  { targetHeading = Math.PI/2.0; isSnapping = true; }
            if (gamepad1.dpad_down)  { targetHeading = Math.PI; isSnapping = true; }
            if (gamepad1.dpad_right) { targetHeading = -Math.PI/2.0; isSnapping = true; }

            if (Math.abs(turn) > 0.1) isSnapping = false;

            // 4. Transform Inputs (Scaling + Field Centric)
            Point rawVector = new Point(drive, strafe);
            Point scaledVector = scaling.ScaleVector(rawVector);
            
            // ROTATE translation for Field-Centric mode
            // We rotate by -currentHeading to transform from field frame to robot frame
            double cos = Math.cos(-currentHeading);
            double sin = Math.sin(-currentHeading);
            double fieldDrive = scaledVector.x * cos - scaledVector.y * sin;
            double fieldStrafe = scaledVector.x * sin + scaledVector.y * cos;

            double driveV = fieldDrive * SwerveConfig.MAX_SPEED_MPS;
            double strafeV = fieldStrafe * SwerveConfig.MAX_SPEED_MPS;
            double turnV;

            if (isSnapping) {
                turnV = snapController.calculate(currentHeading, dt);
            } else {
                turnV = turn * SwerveConfig.MAX_ANGULAR_VELOCITY_RAD_S;
            }

            // 5. Smoothing
            Pose target = new Pose(driveV, strafeV, turnV);
            Pose smoothed = smoother.calculate(target, dt);

            // 6. Command Drivetrain
            drivetrain.setPose(smoothed);

            // Logging
            drivetrain.log();
            telemetry.addData("Field Heading", Math.toDegrees(currentHeading));
            telemetry.addData("Snapping", isSnapping);
            telemetry.update();
        }
    }
}
