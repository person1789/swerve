package org.firstinspires.ftc.teamcode.Swerve.Hardware;

import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.teamcode.Swerve.Core.MathUtil;
import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Vector;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics.SwerveKinematics;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics.SwerveModuleState;
import com.qualcomm.robotcore.hardware.VoltageSensor;

/**
 * Kooky-style drivetrain coordinator: read once, calculate once, command modules.
 * The only retained extension is second-order kinematics plus the idle lock FSM.
 */
public class SwerveDrivetrain {

    public enum States {
        DRIVING,
        WAITING_TO_LOCK,
        LOCKED
    }

    public final SwerveModule frontLeftModule;
    public final SwerveModule frontRightModule;
    public final SwerveModule backRightModule;
    public final SwerveModule backLeftModule;
    public final SwerveModule[] modules;

    private final SwerveKinematics kinematics = new SwerveKinematics();
    private final SwerveModuleState[] states = zeroStates();

    private States state = States.DRIVING;
    private double lockTimerMs = 0.0;

    private final VoltageSensor voltageSensor;
    private double currentVoltage = SwerveConfig.NOMINAL_VOLTAGE;

    public SwerveDrivetrain(HWMap hwMap) {
        this(new SwerveModule[] {
                new SwerveModule(hwMap.FLM, hwMap.FLS, hwMap.FLE, SwerveConfig.OFFSETS[0], SwerveConfig.INVERSIONS[0]),
                new SwerveModule(hwMap.FRM, hwMap.FRS, hwMap.FRE, SwerveConfig.OFFSETS[1], SwerveConfig.INVERSIONS[1]),
                new SwerveModule(hwMap.BRM, hwMap.BRS, hwMap.BRE, SwerveConfig.OFFSETS[2], SwerveConfig.INVERSIONS[2]),
                new SwerveModule(hwMap.BLM, hwMap.BLS, hwMap.BLE, SwerveConfig.OFFSETS[3], SwerveConfig.INVERSIONS[3])
        }, hwMap.voltageSensor);
    }

    public SwerveDrivetrain(SwerveModule[] modules) {
        this(modules, null);
    }

    public SwerveDrivetrain(SwerveModule[] modules, VoltageSensor voltageSensor) {
        if (modules == null || modules.length != 4) {
            throw new IllegalArgumentException("SwerveDrivetrain requires exactly 4 modules.");
        }

        this.frontLeftModule = modules[0];
        this.frontRightModule = modules[1];
        this.backRightModule = modules[2];
        this.backLeftModule = modules[3];
        this.modules = modules;

        for (SwerveModule module : modules) {
            module.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        }
        this.voltageSensor = voltageSensor;
    }

    public void read() {
        if (voltageSensor != null && SwerveConfig.VOLTAGE_COMPENSATION_ENABLED) {
            currentVoltage = voltageSensor.getVoltage();
            if (currentVoltage < 8.0) currentVoltage = 8.0; // Prevent massive unsafe scaling
        } else {
            currentVoltage = SwerveConfig.NOMINAL_VOLTAGE;
        }

        for (int i = 0; i < modules.length; i++) {
            modules[i].setOffset(SwerveConfig.OFFSETS[i]);
            modules[i].setInversion(SwerveConfig.INVERSIONS[i]);
            modules[i].read();
        }
    }

    public void set(Vector normalizedCommand, double dt) {
        set(normalizedCommand.x(), normalizedCommand.y(), normalizedCommand.omega(), dt);
    }

    public void set(double x, double y, double omega, double dt) {
        boolean hasInput = Math.hypot(x, y) > SwerveConfig.INPUT_DEADBAND
                || Math.abs(omega) > SwerveConfig.INPUT_DEADBAND;

        if (hasInput) {
            state = States.DRIVING;
            lockTimerMs = 0.0;
            drive(x, y, omega, dt);
            return;
        }

        switch (state) {
            case DRIVING:
                state = States.WAITING_TO_LOCK;
                lockTimerMs = 0.0;
                drive(0.0, 0.0, 0.0, dt);
                break;

            case WAITING_TO_LOCK:
                lockTimerMs += dt * 1000.0;
                if (lockTimerMs >= SwerveConfig.LOCK_DELAY_MS) {
                    state = States.LOCKED;
                    lock();
                } else {
                    drive(0.0, 0.0, 0.0, dt);
                }
                break;

            case LOCKED:
                lock();
                break;
        }
    }

    public void write(double dt) {
        double voltageScale = SwerveConfig.NOMINAL_VOLTAGE / currentVoltage;

        for (int i = 0; i < modules.length; i++) {
            SwerveModuleState scaledState = new SwerveModuleState();
            scaledState.angleRadians = states[i].angleRadians;
            scaledState.speedMetersPerSecond = states[i].speedMetersPerSecond * voltageScale;
            modules[i].update(scaledState, dt);
        }
    }

    public void pointModulesForCommand(double x, double y, double omega, double dt) {
        drive(x, y, omega, dt);
        for (SwerveModuleState state : states) {
            state.speedMetersPerSecond = 0.0;
        }
    }

    public boolean areModulesAzimuthReady(double toleranceRad) {
        for (SwerveModule module : modules) {
            if (!module.isAzimuthReady(toleranceRad)) {
                return false;
            }
        }
        return true;
    }

    public States getState() {
        return state;
    }

    SwerveModuleState[] getCommandedStates() {
        SwerveModuleState[] copy = zeroStates();
        for (int i = 0; i < states.length; i++) {
            copy[i].speedMetersPerSecond = states[i].speedMetersPerSecond;
            copy[i].angleRadians = states[i].angleRadians;
        }
        return copy;
    }

    private void drive(double x, double y, double omega, double dt) {
        kinematics.setLoopTimeSec(Math.max(1e-3, dt));
        kinematics.inverseKinematics(
                x * SwerveConfig.getMaxLinearSpeedMPS(),
                y * SwerveConfig.getMaxLinearSpeedMPS(),
                omega * SwerveConfig.MAX_ANGULAR_VELOCITY_RAD_S,
                states);
        sanitize(states);
        desaturate(states);
    }

    private void lock() {
        for (int i = 0; i < states.length; i++) {
            states[i].speedMetersPerSecond = 0.0;
            states[i].angleRadians = MathUtil.normalizeAngle(SwerveConfig.LOCKED_STANCE_ANGLES_RAD[i]);
        }
    }

    private static void sanitize(SwerveModuleState[] states) {
        for (SwerveModuleState state : states) {
            if (!Double.isFinite(state.speedMetersPerSecond)) {
                state.speedMetersPerSecond = 0.0;
            }
            if (!Double.isFinite(state.angleRadians)) {
                state.angleRadians = 0.0;
            } else {
                state.angleRadians = MathUtil.normalizeAngle(state.angleRadians);
            }
        }
    }

    private static void desaturate(SwerveModuleState[] states) {
        double max = 0.0;
        for (SwerveModuleState state : states) {
            max = Math.max(max, Math.abs(state.speedMetersPerSecond));
        }

        double maxAllowed = SwerveConfig.getMaxLinearSpeedMPS();
        if (max <= maxAllowed || max < 1e-9) {
            return;
        }

        double scale = maxAllowed / max;
        for (SwerveModuleState state : states) {
            state.speedMetersPerSecond *= scale;
        }
    }

    private static SwerveModuleState[] zeroStates() {
        return new SwerveModuleState[] {
                new SwerveModuleState(),
                new SwerveModuleState(),
                new SwerveModuleState(),
                new SwerveModuleState()
        };
    }
}
