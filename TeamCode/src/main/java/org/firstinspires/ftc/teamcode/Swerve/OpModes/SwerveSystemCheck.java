package org.firstinspires.ftc.teamcode.Swerve.OpModes;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.teamcode.Swerve.Core.Logger;
import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Vector;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.HWMap;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.SwerveDrivetrain;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.SwerveModule;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@Config
@TeleOp(name = "Swerve System Check")
public class SwerveSystemCheck extends LinearOpMode {

    public static double commandScale = 0.35;
    public static boolean csvLoggingEnabled = true;

    private enum TestCase {
        STOP("Stop", new Vector(0.0, 0.0, 0.0)),
        FORWARD("Forward", new Vector(1.0, 0.0, 0.0)),
        BACKWARD("Backward", new Vector(-1.0, 0.0, 0.0)),
        STRAFE_LEFT("Strafe Left", new Vector(0.0, 1.0, 0.0)),
        STRAFE_RIGHT("Strafe Right", new Vector(0.0, -1.0, 0.0)),
        ROTATE_CCW("Rotate CCW", new Vector(0.0, 0.0, 1.0)),
        ROTATE_CW("Rotate CW", new Vector(0.0, 0.0, -1.0));

        final String label;
        final Vector normalizedCommand;

        TestCase(String label, Vector normalizedCommand) {
            this.label = label;
            this.normalizedCommand = normalizedCommand;
        }
    }

    private final ElapsedTime loopTimer = new ElapsedTime();
    private final ElapsedTime runtimeTimer = new ElapsedTime();

    private int selectedIndex = 0;
    private boolean previousLeftBumper;
    private boolean previousRightBumper;
    private boolean previousDpadUp;
    private boolean previousDpadDown;
    private boolean previousB;

    private File csvFile;
    private FileWriter csvWriter;
    private int pendingCsvRows;

    @Override
    public void runOpMode() throws InterruptedException {
        if (SwerveConfig.DASHBOARD_ENABLED) {
            telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());
        }

        HWMap hwMap = new HWMap(hardwareMap);
        Logger logger = new Logger(telemetry);
        SwerveDrivetrain drivetrain = new SwerveDrivetrain(hwMap, logger);

        telemetry.addLine("Swerve System Check ready");
        telemetry.addLine("LB/RB: change test");
        telemetry.addLine("A: hold to run selected test");
        telemetry.addLine("DPAD UP/DOWN: adjust command scale");
        telemetry.addLine("B: stop and reset smoother");
        telemetry.addLine("CSV logging writes to the FTC settings folder");
        telemetry.update();

        waitForStart();
        loopTimer.reset();
        runtimeTimer.reset();
        startCsvLogging();

        try {
            while (opModeIsActive()) {
                double dt = loopTimer.seconds();
                loopTimer.reset();
                if (dt <= 0.0) {
                    dt = 0.02;
                }

                if (gamepad1.left_bumper && !previousLeftBumper) {
                    selectedIndex = (selectedIndex - 1 + TestCase.values().length) % TestCase.values().length;
                    drivetrain.resetSmoother();
                }
                if (gamepad1.right_bumper && !previousRightBumper) {
                    selectedIndex = (selectedIndex + 1) % TestCase.values().length;
                    drivetrain.resetSmoother();
                }
                if (gamepad1.dpad_up && !previousDpadUp) {
                    commandScale = Math.min(1.0, commandScale + 0.05);
                }
                if (gamepad1.dpad_down && !previousDpadDown) {
                    commandScale = Math.max(0.05, commandScale - 0.05);
                }
                if (gamepad1.b && !previousB) {
                    drivetrain.resetSmoother();
                }

                previousLeftBumper = gamepad1.left_bumper;
                previousRightBumper = gamepad1.right_bumper;
                previousDpadUp = gamepad1.dpad_up;
                previousDpadDown = gamepad1.dpad_down;
                previousB = gamepad1.b;

                TestCase selected = TestCase.values()[selectedIndex];
                Vector commanded = gamepad1.a ? selected.normalizedCommand.scale(commandScale) : TestCase.STOP.normalizedCommand;
                drivetrain.setVelocity(commanded, dt);

                Vector observedVelocity = drivetrain.getActualVelocity();
                double runtimeSec = runtimeTimer.seconds();
                double maxLinearSpeedMps = SwerveConfig.getMaxLinearSpeedMetersPerSecond();

                telemetry.addData("Selected Test", selected.label);
                telemetry.addData("Running", gamepad1.a);
                telemetry.addData("Runtime (s)", runtimeSec);
                telemetry.addData("Loop Dt (ms)", dt * 1000.0);
                telemetry.addData("CSV Enabled", csvLoggingEnabled);
                telemetry.addData("CSV File", csvFile != null ? csvFile.getName() : "disabled");

                telemetry.addLine();
                telemetry.addData("Command Scale", commandScale);
                telemetry.addData("Cmd X Norm", commanded.x());
                telemetry.addData("Cmd Y Norm", commanded.y());
                telemetry.addData("Cmd W Norm", commanded.omega());
                telemetry.addData("Cmd X (in/s)", metersToInches(commanded.x() * maxLinearSpeedMps));
                telemetry.addData("Cmd Y (in/s)", metersToInches(commanded.y() * maxLinearSpeedMps));
                telemetry.addData("Cmd W (rad/s)", commanded.omega() * SwerveConfig.MAX_ANGULAR_VELOCITY_RAD_S);

                telemetry.addLine();
                telemetry.addData("Obs X (in/s)", metersToInches(observedVelocity.x()));
                telemetry.addData("Obs Y (in/s)", metersToInches(observedVelocity.y()));
                telemetry.addData("Obs W (rad/s)", observedVelocity.omega());
                telemetry.addData("Drive State", drivetrain.getState());
                telemetry.addData("Max Linear Speed (in/s)", SwerveConfig.MAX_LINEAR_SPEED_IN_S);
                telemetry.addData("Max Accel (in/s^2)", SwerveConfig.MAX_LINEAR_ACCEL_IN_S2);
                telemetry.addData("Max Jerk (in/s^3)", SwerveConfig.MAX_LINEAR_JERK_IN_S3);

                for (int i = 0; i < drivetrain.modules.length; i++) {
                    SwerveModule module = drivetrain.modules[i];
                    telemetry.addLine();
                    telemetry.addData("M" + i + " TargetDeg", Math.toDegrees(module.getLastTargetAngleRad()));
                    telemetry.addData("M" + i + " CurrentDeg", Math.toDegrees(module.getCurrentRotation()));
                    telemetry.addData("M" + i + " ErrorDeg", Math.toDegrees(module.getLastTargetAngleRad() - module.getCurrentRotation()));
                    telemetry.addData("M" + i + " TargetVel (in/s)", metersToInches(module.getLastTargetVelocityMps()));
                    telemetry.addData("M" + i + " ActualVel (in/s)", metersToInches(module.getVelocityMps()));
                    telemetry.addData("M" + i + " DrivePower", module.getLastDrivePower());
                    telemetry.addData("M" + i + " SteerPower", module.getLastSteeringPower());
                    telemetry.addData("M" + i + " Current (A)", module.getCurrentAmps());
                    telemetry.addData("M" + i + " Stalled", module.isStalled());
                }

                appendCsvRow(runtimeSec, dt, selected, gamepad1.a, commanded, observedVelocity, drivetrain);
                telemetry.update();
            }
        } finally {
            closeCsvLogging();
        }
    }

    private void startCsvLogging() {
        if (!csvLoggingEnabled) {
            return;
        }

        csvFile = AppUtil.getInstance().getSettingsFile("SwerveSystemCheck-" + System.currentTimeMillis() + ".csv");
        try {
            csvWriter = new FileWriter(csvFile);
            csvWriter.write(csvHeader());
            csvWriter.write("\n");
            csvWriter.flush();
        } catch (IOException e) {
            telemetry.addData("CSV Logging Error", e.getMessage());
            csvWriter = null;
        }
    }

    private void appendCsvRow(double runtimeSec, double dt, TestCase selected, boolean running, Vector commanded,
            Vector observedVelocity, SwerveDrivetrain drivetrain) {
        if (csvWriter == null) {
            return;
        }

        double maxLinearSpeedMps = SwerveConfig.getMaxLinearSpeedMetersPerSecond();
        StringBuilder row = new StringBuilder();
        row.append(runtimeSec).append(',')
                .append(dt).append(',')
                .append(selected.label).append(',')
                .append(running).append(',')
                .append(commandScale).append(',')
                .append(commanded.x()).append(',')
                .append(commanded.y()).append(',')
                .append(commanded.omega()).append(',')
                .append(metersToInches(commanded.x() * maxLinearSpeedMps)).append(',')
                .append(metersToInches(commanded.y() * maxLinearSpeedMps)).append(',')
                .append(commanded.omega() * SwerveConfig.MAX_ANGULAR_VELOCITY_RAD_S).append(',')
                .append(metersToInches(observedVelocity.x())).append(',')
                .append(metersToInches(observedVelocity.y())).append(',')
                .append(observedVelocity.omega()).append(',')
                .append(drivetrain.getState());

        for (int i = 0; i < drivetrain.modules.length; i++) {
            SwerveModule module = drivetrain.modules[i];
            row.append(',')
                    .append(Math.toDegrees(module.getLastTargetAngleRad()))
                    .append(',')
                    .append(Math.toDegrees(module.getCurrentRotation()))
                    .append(',')
                    .append(Math.toDegrees(module.getLastTargetAngleRad() - module.getCurrentRotation()))
                    .append(',')
                    .append(metersToInches(module.getLastTargetVelocityMps()))
                    .append(',')
                    .append(metersToInches(module.getVelocityMps()))
                    .append(',')
                    .append(module.getLastDrivePower())
                    .append(',')
                    .append(module.getLastSteeringPower())
                    .append(',')
                    .append(module.getCurrentAmps())
                    .append(',')
                    .append(module.isStalled());
        }

        try {
            csvWriter.write(row.append('\n').toString());
            pendingCsvRows++;
            if (pendingCsvRows >= 10) {
                csvWriter.flush();
                pendingCsvRows = 0;
            }
        } catch (IOException e) {
            telemetry.addData("CSV Write Error", e.getMessage());
        }
    }

    private void closeCsvLogging() {
        if (csvWriter == null) {
            return;
        }
        try {
            csvWriter.flush();
            csvWriter.close();
        } catch (IOException ignored) {
        } finally {
            csvWriter = null;
        }
    }

    private String csvHeader() {
        StringBuilder header = new StringBuilder("runtime_sec,dt_sec,test_name,running,command_scale,");
        header.append("cmd_x_norm,cmd_y_norm,cmd_w_norm,cmd_x_in_s,cmd_y_in_s,cmd_w_rad_s,");
        header.append("obs_x_in_s,obs_y_in_s,obs_w_rad_s,drive_state");
        for (int i = 0; i < 4; i++) {
            header.append(",m").append(i).append("_target_deg");
            header.append(",m").append(i).append("_current_deg");
            header.append(",m").append(i).append("_error_deg");
            header.append(",m").append(i).append("_target_in_s");
            header.append(",m").append(i).append("_actual_in_s");
            header.append(",m").append(i).append("_drive_power");
            header.append(",m").append(i).append("_steer_power");
            header.append(",m").append(i).append("_current_amps");
            header.append(",m").append(i).append("_stalled");
        }
        return header.toString();
    }

    private double metersToInches(double meters) {
        return meters / 0.0254;
    }
}
