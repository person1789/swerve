package org.firstinspires.ftc.teamcode.Swerve.OpModes;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.Swerve.Core.LowPassFilter;
import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Point;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose;
import org.firstinspires.ftc.teamcode.Swerve.Input.JoystickScaling;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.SwerveDrivetrain;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Localization.SwerveLocalizer;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.HWMap;
import org.firstinspires.ftc.teamcode.Swerve.Core.Logger;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Control.SwerveController;

/**
 * SwerveTeleOp
 * 
 * High-performance Field-Centric drive OpMode delegating logic to SwerveController.
 */
@TeleOp(name = "Swerve TeleOp", group = "Swerve")
public class SwerveTeleOp extends LinearOpMode {

    private SwerveDrivetrain drivetrain;
    private SwerveLocalizer localizer;
    private SwerveController controller;
    private JoystickScaling scaling;
    
    private LowPassFilter driveFilter;
    private LowPassFilter strafeFilter;
    private LowPassFilter turnFilter;

    private final ElapsedTime timer = new ElapsedTime();

    @Override
    public void runOpMode() throws InterruptedException {
        HWMap hwMap = new HWMap(hardwareMap);
        Logger logger = new Logger(telemetry);
        
        drivetrain = new SwerveDrivetrain(hwMap, logger);
        localizer = new SwerveLocalizer(hwMap);
        controller = new SwerveController();
        scaling = new JoystickScaling();
        
        driveFilter  = new LowPassFilter(SwerveConfig.TRANSLATION_LPF_GAIN);
        strafeFilter = new LowPassFilter(SwerveConfig.TRANSLATION_LPF_GAIN);
        turnFilter   = new LowPassFilter(SwerveConfig.ROTATION_LPF_GAIN);
        
        telemetry.addData("Status", "Initialized // SITL-Ready Architecture");
        telemetry.update();

        waitForStart();
        timer.reset();

        while (opModeIsActive()) {
            double dt = timer.seconds();
            timer.reset();

            localizer.update();
            double currentHeading = localizer.getHeading();

            // 1. Read & Filter Inputs
            double drive  = driveFilter.calculate(-gamepad1.left_stick_y);
            double strafe = strafeFilter.calculate(-gamepad1.left_stick_x);
            double turn   = turnFilter.calculate(-gamepad1.right_stick_x);

            // 2. Control Intercepts (Options/Reset)
            if (gamepad1.options) {
                localizer.resetHeading();
                controller.resetHeading(0);
            }

            // 3. Cardinal Snapping
            if (gamepad1.dpad_up)    controller.setSnapTarget(0);
            if (gamepad1.dpad_left)  controller.setSnapTarget(Math.PI/2.0);
            if (gamepad1.dpad_down)  controller.setSnapTarget(Math.PI);
            if (gamepad1.dpad_right) controller.setSnapTarget(-Math.PI/2.0);

            // 4. Update Controller (The "Brain")
            // Pass inputs as raw percentages; the controller handles m/s and rad/s conversion
            Pose targetSpeeds = controller.update(drive, strafe, turn, currentHeading, dt);

            // 5. Field-Centric Transformation
            Point scaledVector = scaling.ScaleVector(new Point(targetSpeeds.x, targetSpeeds.y));
            double cos = Math.cos(-currentHeading);
            double sin = Math.sin(-currentHeading);
            
            // Re-applying field centricity after controller logic ensures pure relative scaling
            Pose fieldSpeeds = new Pose(
                scaledVector.x * cos - scaledVector.y * sin,
                scaledVector.x * sin + scaledVector.y * cos,
                targetSpeeds.heading
            );

            // 6. Execute Hardware Command
            drivetrain.setPose(fieldSpeeds, dt);

            // 7. Telemetry
            telemetry.addData("Heading", Math.toDegrees(currentHeading));
            telemetry.addData("Mode", controller.isSnapping() ? "SNAP" : (controller.isMaintaining() ? "LOCK" : "MANUAL"));
            telemetry.update();
        }
    }
}
