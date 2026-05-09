package org.firstinspires.ftc.teamcode.OpModes;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.Range;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;



import org.firstinspires.ftc.teamcode.Swerve.Hardware.SwerveModule;


public class OneModuleTest extends OpMode{

    private SwerveModule module;

    public void init() {
         module = new SwerveModule(
                 hardwareMap,
                 "frontLeftMotor",
                 "frontLeftServo",
                 "frontLeftEncoder",
                 0.0,
                 false

         );
    }

    public void loop(){
        double x = gamepad1.left_stick_x;
        double y = gamepad1.left_stick_y;
        double rotate = gamepad1.right_stick_x;

        if (rotate > 0.05) {
            module.setTargetAngle(rotate * 0.5);
            module.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        } else {
            double targetAngle = Math.atan2(x, y);
            module.setTargetAngle(targetAngle);

            double magnitude = Range.clip(Math.hypot(x, y), 0, 0);
            module.setDrivePower(magnitude * 0.5);
        }
    }
    public void stop() {
        module.stop();
    }
}

