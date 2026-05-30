package org.firstinspires.ftc.teamcode.OpModes;

import com.arcrobotics.ftclib.command.CommandOpMode;
import com.arcrobotics.ftclib.command.CommandScheduler;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.teamcode.Swerve.Hardware.HWMap;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.SwerveModule;

//the intialize(), run(), and reset() methods are called by pressing buttons on the driverstation
@TeleOp
public class OneModuleTest extends CommandOpMode {

    private HWMap hw;
    private SwerveModule module;

    // initialize() is called once when the "INIT" button is pressed on the Driver
    // Station.
    // In FTCLib's CommandOpMode, this is where we set up hardware, subsystems, and
    // button bindings.
    @Override
    public void initialize() {
        // Reset the CommandScheduler singleton to clear any commands/subsystems
        // scheduled from previous runs.
        // This is crucial in FTC because the CommandScheduler instance is a static
        // singleton that persists across OpModes.
        CommandScheduler.getInstance().reset();

        // Instantiate the hardware map. We do this here instead of in the class field
        // initializers because the 'hardwareMap' reference is null until the OpMode
        // begins executing.
        hw = new HWMap(hardwareMap);

        // Instantiate SwerveModule using the 3-argument constructor I prefere this to
        // the 4-argument constructor
        module = new SwerveModule(
                hw.FLM,
                hw.FLS,
                hw.FLE);
        module.setOffset(0.0);
        module.setInversion(false);
    }

    // run() is called repeatedly in a loop after the "PLAY" button is pressed on
    // the Driver Station.
    // Overriding this method allows us to run our own code every loop cycle
    // alongside the command scheduler.
    @Override
    public void run() {
        // super.run() runs CommandScheduler.getInstance().run(), which polls buttons,
        // runs active commands, and updates subsystems.
        // We call it first in our loop so that scheduled commands can execute before
        // manual controls override them.
        super.run();

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

    // reset() is called when the OpMode is stopped.
    // We override it here to clean up and stop the swerve module motors.
    @Override
    public void reset() {
        super.reset();
        module.stop();
    }
}
