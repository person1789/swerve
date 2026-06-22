package org.firstinspires.ftc.teamcode.OpModes;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.SwerveDrivetrain;

@TeleOp
public class DT extends LinearOpMode {

    private SwerveDrivetrain drivetrain;

    @Override
    public void runOpMode() throws InterruptedException {
        drivetrain = new SwerveDrivetrain(hardwareMap);

        waitForStart();



        if (isStopRequested()) return;

        while (opModeIsActive()) {
            double x = gamepad1.left_stick_x;
            double y = -gamepad1.left_stick_y;
            double turn = gamepad1.right_stick_x;

            x = Range.clip(x, -1.0, 1.0);
            y = Range.clip(y, -1.0, 1.0);
            turn = Range.clip(turn, -1.0, 1.0);


            boolean noInput =
                    Math.abs(x) < 0.05 &&
                            Math.abs(y) < 0.05 &&
                            Math.abs(turn) < 0.05;

            if (noInput) drivetrain.setLocked(true);
            else drivetrain.setLocked(false);

            drivetrain.updateOffsets();
            drivetrain.set(new Pose(x, y, turn));
            drivetrain.write();
            drivetrain.updateModules();
        }

        drivetrain.stop();
    }
}