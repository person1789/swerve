package org.firstinspires.ftc.teamcode.OpModes;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.HWMap;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.SwerveDrivetrain;
import org.firstinspires.ftc.teamcode.auto.DriveContext;
import org.firstinspires.ftc.teamcode.auto.DriveScheduler;
import org.firstinspires.ftc.teamcode.auto.PinpointLocalizer;
import org.firstinspires.ftc.teamcode.auto.StopDriveCommand;

@Config
@Autonomous(name = "Mini Drive Auto")
public class MiniDriveAuto extends LinearOpMode {
    public static double TARGET_X_IN = 24.0;
    public static double TARGET_Y_IN = 0.0;
    public static double TARGET_HEADING_RAD = 0.0;

    private final ElapsedTime loopTimer = new ElapsedTime();

    @Override
    public void runOpMode() throws InterruptedException {
        if (SwerveConfig.DASHBOARD_ENABLED) {
            telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());
        }

        HWMap hwMap = new HWMap(hardwareMap);
        SwerveDrivetrain drivetrain = new SwerveDrivetrain(hwMap);
        PinpointLocalizer localizer = new PinpointLocalizer(hwMap);
        DriveContext context = new DriveContext(drivetrain, localizer);
        DriveScheduler scheduler = new DriveScheduler(4)
                .addMoveToPose(TARGET_X_IN, TARGET_Y_IN, TARGET_HEADING_RAD, true)
                .add(new StopDriveCommand());

        loopTimer.reset();
        while (!isStarted() && !isStopRequested()) {
            hwMap.clearBulkCache();
            drivetrain.read();
            localizer.update();
            double dt = Math.max(1e-3, loopTimer.seconds());
            loopTimer.reset();
            drivetrain.pointModulesForCommand(1.0, 0.0, 0.0, dt);
            drivetrain.write(dt);

            telemetry.addData("pose", "%.1f %.1f %.1f",
                    localizer.getXInches(),
                    localizer.getYInches(),
                    Math.toDegrees(localizer.getHeadingRadians()));
            telemetry.addData("pinpoint", localizer.isReady());
            telemetry.addData("azimuthReady", drivetrain.areModulesAzimuthReady(SwerveConfig.AUTO_AZIMUTH_TOLERANCE_RAD));
            telemetry.update();
        }

        loopTimer.reset();
        int loopCounter = 0;
        while (opModeIsActive()) {
            hwMap.clearBulkCache();
            drivetrain.read();
            localizer.update();
            double dt = Math.max(1e-3, loopTimer.seconds());
            loopTimer.reset();

            scheduler.tick(context, dt);
            drivetrain.write(dt);

            if ((loopCounter++ & 3) == 0) {
                telemetry.addData("cmd", scheduler.getCurrentCommandName());
                telemetry.addData("aborted", scheduler.wasAborted());
                telemetry.addData("pose", "%.1f %.1f %.1f",
                        localizer.getXInches(),
                        localizer.getYInches(),
                        Math.toDegrees(localizer.getHeadingRadians()));
                telemetry.addData("loopMs", dt * 1000.0);
                telemetry.update();
            }
        }
    }
}
