package org.firstinspires.ftc.teamcode.Swerve.Drive;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.Swerve.Geo.MathUtil;
import org.firstinspires.ftc.teamcode.Swerve.Geo.Point;
import org.firstinspires.ftc.teamcode.Swerve.Geo.Pose;
import org.firstinspires.ftc.teamcode.Swerve.Limiters.JoystickScaling;
import org.firstinspires.ftc.teamcode.Swerve.Limiters.MotionSmoother;
import org.firstinspires.ftc.teamcode.Swerve.Util.PIDController;
import org.firstinspires.ftc.teamcode.core.HWMap;
import org.firstinspires.ftc.teamcode.core.Logger;

/**
 * SwerveTeleOp — Phase D
 * 
 * Standard driver control OpMode.
 * Controls:
 * - Left Stick: Translation (Forward/Backward, Strafe)
 * - Right Stick: Rotation
 * - D-Pad: Heading Snap (Up=0°, Left=90°, Down=180°, Right=270°)
 * - Button A: Reset Odometry/Heading
 */
@TeleOp(name = "Swerve TeleOp", group = "Swerve")
public class SwerveTeleOp extends LinearOpMode {

    private SwerveDrivetrain drivetrain;
    private MotionSmoother smoother;
    private JoystickScaling scaling;
    private PIDController snapController;
    
    private double targetHeading = 0.0;
    private boolean isSnapping = false;
    private boolean fieldCentric = true;

    private final ElapsedTime timer = new ElapsedTime();

    @Override
    public void runOpMode() throws InterruptedException {
        // Initialization
        HWMap hwMap = new HWMap(hardwareMap);
        Logger logger = new Logger(telemetry);
        
        drivetrain = new SwerveDrivetrain(hwMap, logger);
        smoother = new MotionSmoother();
        scaling = new JoystickScaling();
        
        snapController = new PIDController(SwerveConfig.SNAP_P, SwerveConfig.SNAP_I, SwerveConfig.SNAP_D);
        
        telemetry.addData("Status", "Initialized - Phase D");
        telemetry.update();

        waitForStart();
        timer.reset();

        while (opModeIsActive()) {
            double dt = timer.seconds();
            timer.reset();

            // 1. Read Inputs
            double drive = -gamepad1.left_stick_y; // Reverse Y
            double strafe = -gamepad1.left_stick_x; // Reverse X for intuitive strafing
            double turn = -gamepad1.right_stick_x;

            // 2. Button Actions
            if (gamepad1.a) {
                // Reset logic would go here (e.g. odo.setPosition)
                targetHeading = 0;
                isSnapping = false;
            }

            // 3. Heading Snap (D-pad)
            if (gamepad1.dpad_up)    { targetHeading = 0; isSnapping = true; }
            if (gamepad1.dpad_left)  { targetHeading = Math.PI/2.0; isSnapping = true; }
            if (gamepad1.dpad_down)  { targetHeading = Math.PI; isSnapping = true; }
            if (gamepad1.dpad_right) { targetHeading = -Math.PI/2.0; isSnapping = true; }

            // Release snap if driver uses right stick manually
            if (Math.abs(turn) > 0.1) {
                isSnapping = false;
            }

            // 4. Transform Inputs (Scaling)
            Point rawVector = new Point(drive, strafe);
            Point scaledVector = scaling.ScaleVector(rawVector);
            
            double driveV = scaledVector.x * SwerveConfig.MAX_SPEED_MPS;
            double strafeV = scaledVector.y * SwerveConfig.MAX_SPEED_MPS;
            double turnV;

            if (isSnapping) {
                // Use PID to converge to target heading
                // Note: Requires current heading from odometry in Phase E. 
                // For now, we simulate or use a placeholder.
                double currentHeading = 0; // Replace with ODO heading in Phase E
                double error = MathUtil.angleError(currentHeading, targetHeading);
                turnV = snapController.calculate(currentHeading, dt);
            } else {
                turnV = turn * SwerveConfig.MAX_ANGULAR_VELOCITY_RAD_S;
            }

            // 5. Smoothing (Phase D)
            Pose target = new Pose(driveV, strafeV, turnV);
            Pose smoothed = smoother.calculate(target, dt);

            // 6. Command Drivetrain
            drivetrain.setPose(smoothed);

            // Logging
            drivetrain.log();
            telemetry.addData("State", drivetrain.getState());
            telemetry.addData("Snapping", isSnapping);
            telemetry.addData("Target Heading", Math.toDegrees(targetHeading));
            telemetry.update();
        }
    }
}
