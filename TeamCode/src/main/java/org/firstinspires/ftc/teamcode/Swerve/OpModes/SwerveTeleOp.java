package org.firstinspires.ftc.teamcode.Swerve.OpModes;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.Swerve.Core.LowPassFilter;
import org.firstinspires.ftc.teamcode.Swerve.Core.PIDController;
import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Point;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose;
import org.firstinspires.ftc.teamcode.Swerve.Input.JoystickScaling;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.SwerveDrivetrain;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Localization.SwerveLocalizer;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.HWMap;
import org.firstinspires.ftc.teamcode.Swerve.Core.Logger;

/**
 * SwerveTeleOp
 * 
 * High-performance Field-Centric drive OpMode with input filtering and Heading Retention.
 */
@TeleOp(name = "Swerve TeleOp", group = "Swerve")
public class SwerveTeleOp extends LinearOpMode {

    private SwerveDrivetrain drivetrain;
    private SwerveLocalizer localizer;
    private JoystickScaling scaling;
    
    private PIDController snapController;
    private PIDController maintainPID;
    
    private LowPassFilter driveFilter;
    private LowPassFilter strafeFilter;
    private LowPassFilter turnFilter;

    private double targetHeading = 0.0;
    private boolean isSnapping = false;
    private boolean isMaintaining = false;

    private final ElapsedTime timer = new ElapsedTime();

    @Override
    public void runOpMode() throws InterruptedException {
        HWMap hwMap = new HWMap(hardwareMap);
        Logger logger = new Logger(telemetry);
        
        drivetrain = new SwerveDrivetrain(hwMap, logger);
        localizer = new SwerveLocalizer(hwMap);
        scaling = new JoystickScaling();
        
        driveFilter  = new LowPassFilter(SwerveConfig.TRANSLATION_LPF_GAIN);
        strafeFilter = new LowPassFilter(SwerveConfig.TRANSLATION_LPF_GAIN);
        turnFilter   = new LowPassFilter(SwerveConfig.ROTATION_LPF_GAIN);
        
        snapController = new PIDController(SwerveConfig.SNAP_P, SwerveConfig.SNAP_I, SwerveConfig.SNAP_D);
        maintainPID = new PIDController(SwerveConfig.HEADING_P, SwerveConfig.HEADING_I, SwerveConfig.HEADING_D);
        
        telemetry.addData("Status", "Initialized (Heading Lock Active)");
        telemetry.update();

        waitForStart();
        timer.reset();

        while (opModeIsActive()) {
            double dt = timer.seconds();
            timer.reset();

            localizer.update();
            double currentHeading = localizer.getHeading();

            // 1. Read & Filter Inputs
            double rawDrive  = -gamepad1.left_stick_y;
            double rawStrafe = -gamepad1.left_stick_x;
            double rawTurn   = -gamepad1.right_stick_x;

            double drive  = driveFilter.calculate(rawDrive);
            double strafe = strafeFilter.calculate(rawStrafe);
            double turn   = turnFilter.calculate(rawTurn);

            // 2. Control State Transitions
            if (gamepad1.options) {
                localizer.resetHeading();
                targetHeading = 0;
                isSnapping = false;
            }

            if (gamepad1.dpad_up)    { targetHeading = 0; isSnapping = true; }
            if (gamepad1.dpad_left)  { targetHeading = Math.PI/2.0; isSnapping = true; }
            if (gamepad1.dpad_down)  { targetHeading = Math.PI; isSnapping = true; }
            if (gamepad1.dpad_right) { targetHeading = -Math.PI/2.0; isSnapping = true; }

            if (Math.abs(rawTurn) > 0.1) {
                isSnapping = false;
                isMaintaining = false;
            }

            // Heading Retention Logic
            boolean isMoving = Math.hypot(rawDrive, rawStrafe) > 0.1;
            boolean noTurnInput = Math.abs(rawTurn) < 0.05;

            double turnV;
            if (isSnapping) {
                turnV = snapController.calculate(currentHeading, targetHeading, dt);
            } else if (isMoving && noTurnInput) {
                if (!isMaintaining) {
                    targetHeading = currentHeading;
                    isMaintaining = true;
                    maintainPID.reset();
                }
                turnV = maintainPID.calculate(currentHeading, targetHeading, dt);
            } else {
                isMaintaining = false;
                turnV = turn * SwerveConfig.MAX_ANGULAR_VELOCITY_RAD_S;
            }

            // 3. Transformation & Command
            Point scaledVector = scaling.ScaleVector(new Point(drive, strafe));
            double cos = Math.cos(-currentHeading);
            double sin = Math.sin(-currentHeading);
            double fieldDrive = scaledVector.x * cos - scaledVector.y * sin;
            double fieldStrafe = scaledVector.x * sin + scaledVector.y * cos;

            Pose target = new Pose(
                fieldDrive * SwerveConfig.MAX_SPEED_MPS, 
                fieldStrafe * SwerveConfig.MAX_SPEED_MPS, 
                turnV
            );
            
            drivetrain.setPose(target, dt);

            // 4. Telemetry
            drivetrain.log();
            telemetry.addData("Heading", Math.toDegrees(currentHeading));
            telemetry.addData("LockMode", isSnapping ? "SNAP" : (isMaintaining ? "MAINTAIN" : "MANUAL"));
            telemetry.update();
        }
    }
}
