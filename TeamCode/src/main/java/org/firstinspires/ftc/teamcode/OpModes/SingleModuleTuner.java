package org.firstinspires.ftc.teamcode.OpModes;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.HWMap;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.SwerveDrivetrain;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.SwerveModule;

import java.io.File;

@Config
@TeleOp(name = "Single Module Tuner", group = "Tuning")
public class SingleModuleTuner extends LinearOpMode {

    public static int TUNING_MODULE_INDEX = 0; // 0=FL, 1=FR, 2=BR, 3=BL
    public static double TARGET_ANGLE_DEGREES = 0.0;
    public static double TARGET_DRIVE_POWER = 0.0;
    public static double Loop_Time_Buffer_MS = 20.0;
    public static boolean SAVE_ON_STOP = false;

    @Override
    public void runOpMode() throws InterruptedException {
        telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());
        HWMap hwMap = new HWMap(hardwareMap);
        SwerveDrivetrain drivetrain = new SwerveDrivetrain(hwMap);

        telemetry.addLine("Ready");
        telemetry.update();

        waitForStart();
        ElapsedTime loopTimer = new ElapsedTime();

        try {
            while (opModeIsActive()) {
                hwMap.clearBulkCache();
                drivetrain.read();

                double dt = Math.max(1e-3, loopTimer.seconds());
                loopTimer.reset();

                int safeIndex = Math.max(0, Math.min(3, TUNING_MODULE_INDEX));

                for (int i = 0; i < drivetrain.modules.length; i++) {
                    SwerveModule module = drivetrain.modules[i];
                    if (i == safeIndex) {
                        module.update(Math.toRadians(TARGET_ANGLE_DEGREES), TARGET_DRIVE_POWER, dt);

                        telemetry.addData("--- TUNING MODULE", getModuleName(safeIndex) + " (" + safeIndex + ") ---");
                        telemetry.addData("Current Angle (Deg)", Math.toDegrees(module.getCurrentRotation()));
                        telemetry.addData("Target Angle (Deg)", TARGET_ANGLE_DEGREES);
                        telemetry.addData("Steer Error (Deg)", Math.toDegrees(module.getSteerErrorRadians()));
                        telemetry.addData("Current Velocity", module.getVelocityMps());
                        telemetry.addData("Steer Power", module.getLastSteerPower());
                        telemetry.addData("Drive Power", module.getLastDrivePower());
                    } else {
                        module.update(0.0, 0.0, dt);
                    }
                }

                drivetrain.write(dt);
                telemetry.addData("Actual Loop Time (ms)", dt * 1000.0);
                telemetry.update();

                if (Loop_Time_Buffer_MS > 0) {
                    sleep((long) Loop_Time_Buffer_MS);
                }
            }
        } finally {
            if (SAVE_ON_STOP) {
                saveConfigToSourceFile();
            }
        }
    }

    private void saveConfigToSourceFile() {
        File dir = new java.io.File(".").getAbsoluteFile();
        File configFile = null;
        while (dir != null) {
            File possible1 = new File(dir,
                    "TeamCode/src/main/java/org/firstinspires/ftc/teamcode/Swerve/Core/SwerveConfig.java");
            File possible2 = new File(dir,
                    "src/main/java/org/firstinspires/ftc/teamcode/Swerve/Core/SwerveConfig.java");
            if (possible1.exists()) {
                configFile = possible1;
                break;
            }
            if (possible2.exists()) {
                configFile = possible2;
                break;
            }
            dir = dir.getParentFile();
        }

        if (configFile == null) {
            telemetry.addData("Save Status", "SwerveConfig.java not found in workspace");
            telemetry.update();
            return;
        }

        try {
            java.util.List<String> lines = new java.util.ArrayList<>();
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(configFile));
            String line;
            while ((line = reader.readLine()) != null) {
                String lineTrimmed = line.trim();
                if (lineTrimmed.startsWith("public static double STEER_P =")) {
                    line = "        public static double STEER_P = " + SwerveConfig.STEER_P + ";";
                } else if (lineTrimmed.startsWith("public static double STEER_I =")) {
                    line = "        public static double STEER_I = " + SwerveConfig.STEER_I + ";";
                } else if (lineTrimmed.startsWith("public static double STEER_D =")) {
                    line = "        public static double STEER_D = " + SwerveConfig.STEER_D + ";";
                } else if (lineTrimmed.startsWith("public static double[] OFFSETS =")) {
                    line = "        public static double[] OFFSETS = { " +
                            SwerveConfig.OFFSETS[0] + ", " +
                            SwerveConfig.OFFSETS[1] + ", " +
                            SwerveConfig.OFFSETS[2] + ", " +
                            SwerveConfig.OFFSETS[3] + " };";
                } else if (lineTrimmed.startsWith("public static boolean[] INVERSIONS =")) {
                    line = "        public static boolean[] INVERSIONS = { " +
                            SwerveConfig.INVERSIONS[0] + ", " +
                            SwerveConfig.INVERSIONS[1] + ", " +
                            SwerveConfig.INVERSIONS[2] + ", " +
                            SwerveConfig.INVERSIONS[3] + " };";
                }
                lines.add(line);
            }
            reader.close();

            java.io.BufferedWriter writer = new java.io.BufferedWriter(new java.io.FileWriter(configFile));
            for (String l : lines) {
                writer.write(l);
                writer.newLine();
            }
            writer.close();
            telemetry.addData("Save Status", "Successfully saved config");
            telemetry.update();
        } catch (java.io.IOException e) {
            telemetry.addData("Save Status", "Error writing file: " + e.getMessage());
            telemetry.update();
        }
    }

    private String getModuleName(int index) {
        switch (index) {
            case 0:
                return "Front Left";
            case 1:
                return "Front Right";
            case 2:
                return "Back Right";
            case 3:
                return "Back Left";
            default:
                return "Unknown";
        }
    }
}
