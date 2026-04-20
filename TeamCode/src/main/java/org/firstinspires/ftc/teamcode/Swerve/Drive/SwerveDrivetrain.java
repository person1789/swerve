package org.firstinspires.ftc.teamcode.Swerve.Drive;

import static java.lang.Math.abs;
import static java.lang.Math.atan2;
import static java.lang.Math.hypot;
import static java.lang.Math.pow;
import static java.lang.Math.signum;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.teamcode.Swerve.Geo.Pose;
import org.firstinspires.ftc.teamcode.core.HWMap;
import org.firstinspires.ftc.teamcode.core.Logger;

@Config
public class SwerveDrivetrain {
    public enum States { DRIVING, WAITING_TO_LOCK, LOCKED }
    public SwerveModule frontLeftModule, frontRightModule, backRightModule, backLeftModule;
    public SwerveModule[] modules;
    private Logger logger;

    private double[] ws = new double[4], wa = new double[4], lastwa = new double[4];

    private double[] MotorScaling = new double[]{-1,1,-1,-1};
    private double trackwidth = 9.921, wheelbase = 9.927, R = hypot(trackwidth, wheelbase);

    private States state = States.DRIVING;
    public static double LOCK_DELAY = 200;
    private final ElapsedTime lockTimer = new ElapsedTime();
    private long lastInputTime = 0;

    public SwerveDrivetrain(HWMap hwMap, Logger logger) {
        this.logger = logger;
        frontLeftModule = new SwerveModule(hwMap.FLM, hwMap.FLS, hwMap.FLE, -0.2, false, logger);
        frontRightModule = new SwerveModule(hwMap.FRM, hwMap.FRS, hwMap.FRE, 3.9, false, logger);
        backRightModule = new SwerveModule(hwMap.BRM, hwMap.BRS, hwMap.BRE, 1.4, false, logger);
        backLeftModule = new SwerveModule(hwMap.BLM, hwMap.BLS, hwMap.BLE, 3, false, logger);
        modules = new SwerveModule[]{frontLeftModule, frontRightModule, backRightModule, backLeftModule};
        for (SwerveModule m : modules) m.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        R = hypot(trackwidth, wheelbase);
    }

    public void setPose(Pose pose) {
        double driveMagnitude = hypot(pose.x, pose.y);
        long currentTime = System.currentTimeMillis();
        double dt = (lastInputTime == 0) ? 30 : (currentTime - lastInputTime);
        lastInputTime = currentTime;

        boolean hasInput = (driveMagnitude > 0.01 || Math.abs(pose.heading) > 0.01);

        switch (state) {
            case DRIVING:
                drive(pose.x, pose.y, pose.heading, dt);
                if (!hasInput) { lockTimer.reset(); state = States.WAITING_TO_LOCK; }
                break;
            case WAITING_TO_LOCK:
                ws = new double[]{0,0,0,0}; System.arraycopy(lastwa, 0, wa, 0, 4);
                if (hasInput) state = States.DRIVING;
                else if (lockTimer.milliseconds() > LOCK_DELAY) state = States.LOCKED;
                break;
            case LOCKED:
                ws = new double[]{0,0,0,0}; wa = new double[]{0,0,0,0};
                if (hasInput) state = States.DRIVING;
                break;
        }
        normalizeWheelSpeeds();
        updateModules();
    }

    private void drive(double x, double y, double heading, double dt) {
        R = hypot(trackwidth, wheelbase);
        double dtSeconds = dt / 1000.0;
        double rotCorr = heading * dtSeconds / 2.0;
        double cosC = Math.cos(rotCorr), sinC = Math.sin(rotCorr);
        double xComp = x * cosC - y * sinC, yComp = x * sinC + y * cosC;

        double a = xComp - heading * (wheelbase / R), b = xComp + heading * (wheelbase / R),
                c = yComp - heading * (trackwidth / R), d = yComp + heading * (trackwidth / R);

        ws = new double[]{hypot(a, c), hypot(a, d), hypot(b, d), hypot(b, c)};
        wa = new double[]{atan2(a, c), atan2(a, d), atan2(b, d), atan2(b, c)};
    }

    public void updateModules() {
        for (int i = 0; i < 4; i++) {
            modules[i].update(wa[i], (ws[i] * MotorScaling[i]));
            lastwa[i] = wa[i];
        }
    }

    public void setOffsets(double[] offsets) { for (int i = 0; i < modules.length; i++) modules[i].setOffset(offsets[i]); }
    public void setInverses(boolean[] inverses) { for (int i = 0; i < modules.length; i++) modules[i].setInverse(inverses[i]); }
    public void setMotorScaling(double[] scalers) { if (scalers.length == 4) System.arraycopy(scalers, 0, this.MotorScaling, 0, 4); }

    public void log() { for (int i = 0; i < modules.length; i++) modules[i].log(i); }

    private void normalizeWheelSpeeds() {
        double max = 0;
        for (double speed : ws) max = Math.max(max, speed);
        if (max > 1.0) { for (int i = 0; i < 4; i++) ws[i] /= max; }
    }


}