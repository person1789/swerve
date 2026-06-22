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

    public static double TRACK_WIDTH = 9.921, WHEEL_BASE = 9.927;
    private double R;
    public static double frontLeftOffset = 2, frontRightOffset = 3.3, backLeftOffset = -1.9, backRightOffset = 2;

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
            ws = new double[]{hypot(b, d), hypot(b, c), hypot(a, c), hypot(a, d)};
            wa = new double[]{atan2(b, d), atan2(b, c), atan2(a, c), atan2(a, d)};
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
    public void updateOffsets() {
        frontLeftModule.setOffset(frontLeftOffset);
        frontRightModule.setOffset(frontRightOffset);
        backLeftModule.setOffset(backLeftOffset);
        backRightModule.setOffset(backRightOffset);
    }


    public void updateModules() {for (SwerveModule m : modules) m.update();}
    public void stop() {
        for (SwerveModule m : modules) {
            m.stop();
        }
    }
    public void setLocked(boolean locked) {this.locked = locked;}

    public boolean isLocked() {
        return locked;
    }

}
