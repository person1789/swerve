package org.firstinspires.ftc.teamcode.Swerve.Hardware;

import static java.lang.Math.atan2;
import static java.lang.Math.hypot;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.Swerve.Core.MathUtil;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose;


@Config
public class SwerveDrivetrain {
    public SwerveModule frontLeftModule, backLeftModule, backRightModule, frontRightModule;
    public SwerveModule[] modules;

    public static double TRACK_WIDTH = 0.0, WHEEL_BASE = 0.0;
    private double R;
    public static double frontLeftOffset = 5.1, frontRightOffset = 0.2, backLeftOffset = -1.2, backRightOffset = 1.3;

    double[] ws = new double[4];
    double[] wa = new double[4];
    double max = 0.0;

    private boolean locked = false;

    public SwerveDrivetrain(HardwareMap hardwareMap) {
        frontLeftModule = new SwerveModule(hardwareMap, "FLM", "FLS", "FLE", frontLeftOffset, false);
        backLeftModule = new SwerveModule(hardwareMap, "BLM", "BLS", "BLE", backLeftOffset, false);
        backRightModule = new SwerveModule(hardwareMap, "BRM", "BRS", "BRE", backRightOffset, false);
        frontRightModule = new SwerveModule(hardwareMap, "FRM", "FRS", "FRE", frontRightOffset, false);

        modules = new SwerveModule[]{frontLeftModule, frontRightModule, backRightModule, backLeftModule};
        for (SwerveModule m : modules) m.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        R = hypot(TRACK_WIDTH, WHEEL_BASE);
    }

    public void set(Pose pose) {
        double x = pose.x, y = pose.y, head = pose.heading;

        double a = x - head * (WHEEL_BASE / R),
                b = x + head * (WHEEL_BASE / R),
                c = y - head * (TRACK_WIDTH / R),
                d = y + head * (TRACK_WIDTH / R);

        if (locked) {
            ws = new double[]{0, 0, 0, 0};
            wa = new double[]{Math.PI / 4, -Math.PI / 4, Math.PI / 4, -Math.PI / 4};
        } else {
            ws = new double[]{hypot(b, c), hypot(b, d), hypot(a, d), hypot(a, c)};
            wa = new double[]{atan2(b, c), atan2(b, d), atan2(a, d), atan2(a, c)};
        }

        max = MathUtil.maxAbs(ws);
    }

    public void write() {
        for (int i = 0; i < 4; i++) {
            SwerveModule m = modules[i];
            if (Math.abs(max) > 1) ws[i] /= max;
            m.setDrivePower(Math.abs(ws[i]));
            m.setTargetAngle(MathUtil.normalizeAngle(wa[i]));
        }
    }

    public void updateModules() {for (SwerveModule m : modules) m.update();}

    public void setLocked(boolean locked) {this.locked = locked;}

    public boolean isLocked() {
        return locked;
    }
}