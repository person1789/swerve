package org.firstinspires.ftc.teamcode.Swerve.OpModes;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.Swerve.Hardware.SwerveDrivetrain;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Localization.SwerveLocalizer;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.HWMap;
import org.firstinspires.ftc.teamcode.Swerve.Core.Logger;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Control.SwerveController;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Vector;

/**
 * SwerveTeleOp
 * 
 * High-performance Field-Centric drive OpMode.
 * Delegating all processing to the SwerveController and SwerveDrivetrain.
 */
@TeleOp(name = "Swerve TeleOp", group = "Swerve")
public class SwerveTeleOp extends LinearOpMode {

    private SwerveDrivetrain drivetrain;
    private SwerveLocalizer localizer;
    private SwerveController controller;
    private final ElapsedTime timer = new ElapsedTime();

    @Override
    public void runOpMode() throws InterruptedException {
        HWMap hwMap = new HWMap(hardwareMap);
        Logger logger = new Logger(telemetry);
        
        drivetrain = new SwerveDrivetrain(hwMap, logger);
        localizer = new SwerveLocalizer(hwMap);
        controller = new SwerveController();
        
        waitForStart();
        timer.reset();

        while (opModeIsActive()) {
            double dt = timer.seconds();
            timer.reset();

            // 1. Update State
            localizer.update(drivetrain.getActualVelocity(), dt);
            double heading = localizer.getHeading();

            // 2. Control Logic (Options / Reset / Snapping)
            if (gamepad1.options) {
                localizer.resetHeading();
                controller.resetHeading(0);
            }

            if (gamepad1.dpad_up)    controller.setSnapTarget(0);
            if (gamepad1.dpad_left)  controller.setSnapTarget(Math.PI/2.0);
            if (gamepad1.dpad_down)  controller.setSnapTarget(Math.PI);
            if (gamepad1.dpad_right) controller.setSnapTarget(-Math.PI/2.0);

            // 3. Process Field-Centric Translation
            Vector rawTranslation = new Vector(-gamepad1.left_stick_y, -gamepad1.left_stick_x).rotate(-heading);
            double rawTurn = -gamepad1.right_stick_x;

            // 4. Update the "Brain" (Heading Hold / Snap / Smoothing)
            Vector chassisSpeeds = controller.update(
                rawTranslation.x(), 
                rawTranslation.y(), 
                rawTurn, 
                heading, 
                dt
            );

            // 5. Hardware Execution
            drivetrain.setVelocity(chassisSpeeds, dt);

            // 6. Telemetry
            telemetry.addData("Heading", Math.toDegrees(heading));
            telemetry.addData("State", controller.isSnapping() ? "SNAP" : (controller.isMaintaining() ? "HOLD" : "MANUAL"));
            telemetry.update();
        }
    }
}
