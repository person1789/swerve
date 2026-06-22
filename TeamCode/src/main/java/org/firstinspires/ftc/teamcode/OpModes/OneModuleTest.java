package org.firstinspires.ftc.teamcode.OpModes;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.Range;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import com.arcrobotics.ftclib.gamepad.GamepadEx;
import com.arcrobotics.ftclib.gamepad.GamepadKeys;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;




import org.firstinspires.ftc.teamcode.Swerve.Hardware.SwerveModule;

@TeleOp
public class OneModuleTest extends LinearOpMode {

    private SwerveModule module;
    @Override
    public void runOpMode() throws InterruptedException {
        module = new SwerveModule(
                hardwareMap,
                "FLM",
                "FLS",
                "FLE",
                0.0,
                false
        );

        waitForStart();

        if (isStopRequested()) return;

        while (opModeIsActive()) {
            double x = gamepad1.left_stick_x;
            double y = -gamepad1.left_stick_y;
            double manualSteer = gamepad1.right_stick_x;

            double magnitude = Range.clip(Math.hypot(x, y), -1.0, 1.0);

            if (magnitude > 0.05) {
                double targetAngle = Math.atan2(x, y);
                module.setTargetAngle(targetAngle);
            }

            if (Math.abs(manualSteer) > 0.05) {
                module.setManualSteerPower(manualSteer * 0.5);
            } else {
                module.update();
            }

            module.setDrivePower(magnitude * 0.5);
        }

        module.stop();
    }
}
