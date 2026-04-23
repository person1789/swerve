package org.firstinspires.ftc.teamcode.Swerve.OpModes;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.arcrobotics.ftclib.util.Timing;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.AnalogInput;
import com.qualcomm.robotcore.hardware.CRServoImplEx;

import org.firstinspires.ftc.teamcode.Swerve.Core.MathUtil;
import org.firstinspires.ftc.teamcode.Swerve.Core.PIDController;
import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.HWMap;

import java.util.concurrent.TimeUnit;

@Config
@TeleOp
public class SwerveModulePIDTune extends LinearOpMode {

    HWMap hwMap;
    public static double targetAngleDeg = 0;

    private PIDController steerController;

    public static double direction = -1.0;

    private CRServoImplEx FLS;
    private AnalogInput FLE;

    Timing.Timer timer;
    public static long sleepTime = 25;


    @Override
    public void runOpMode() throws InterruptedException {

        timer = new Timing.Timer(3000000, TimeUnit.MILLISECONDS);
        hwMap = new HWMap(hardwareMap);
        FLS = hwMap.FLS;
        FLE = hwMap.FLE;

        // Setup Telemetry
        telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());

        // Setup PID
        steerController = new PIDController(SwerveConfig.STEER_P, SwerveConfig.STEER_I, SwerveConfig.STEER_D);

        waitForStart();

        while (opModeIsActive()) {
            steerController.setPID(SwerveConfig.STEER_P, SwerveConfig.STEER_I, SwerveConfig.STEER_D);

            double currentRad = MathUtil.normalizeAngle((FLE.getVoltage() / 3.3) * 2.0 * Math.PI - SwerveConfig.OFFSETS[0]);
            double targetRad = Math.toRadians(targetAngleDeg);
            double error = MathUtil.angleError(currentRad, targetRad);
            double power = steerController.calculateFromError(error, 0.025);

            double safePower = Math.max(-1, Math.min(1, power * direction));

            FLS.setPower(safePower);

            telemetry.addData("Target (deg)", targetAngleDeg);
            telemetry.addData("Current (deg)", Math.toDegrees(currentRad));
            telemetry.addData("Error (deg)", Math.toDegrees(error));
            telemetry.addData("Power", safePower);
            telemetry.update();
            sleep(sleepTime);

        }
    }
}
