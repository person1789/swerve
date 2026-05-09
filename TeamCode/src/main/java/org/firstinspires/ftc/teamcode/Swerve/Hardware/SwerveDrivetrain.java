package org.firstinspires.ftc.teamcode.Swerve.Hardware;

import com.qualcomm.robotcore.hardware.DcMotor;

import java.util.function.DoubleSupplier;

import org.firstinspires.ftc.teamcode.Swerve.Core.Logger;
import org.firstinspires.ftc.teamcode.Swerve.Core.MathUtil;
import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Vector;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics.SwerveKinematics;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics.SwerveModuleState;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Localization.SwerveVelocityObserver;

/**
 * Central coordinator for the swerve drive system.
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

    private final SwerveKinematics kinematics;
    private final SwerveVelocityObserver velocityObserver;
    private final DoubleSupplier batteryVoltageSupplier;
    private final Logger logger;
    private final double[] currentAngles = new double[4];
    private final SwerveModuleState[] workingStates = zeroStates();
    private final SwerveModuleState[] gatedStates = zeroStates();
    private final SwerveModuleState[] xStates = zeroStates();

    private States state = States.DRIVING;
    private double lockTimerMs = 0.0;
    private double lastDriverTargetX = 0.0;
    private double lastDriverTargetY = 0.0;
    private double lastDriverTargetOmega = 0.0;
    private double lastVelocityX = 0.0;
    private double lastVelocityY = 0.0;
    private double lastVelocityOmega = 0.0;
    private double lastTranslationAuthority = 1.0;
    private final SwerveModuleState[] lastRawStates = zeroStates();
    private final SwerveModuleState[] lastOptimizedStates = zeroStates();
    private boolean steerReadyForDrive = true;

    public SwerveDrivetrain(HWMap hwMap, Logger logger) {
        this(new SwerveModule[] {
                new SwerveModule(hwMap.FLM, hwMap.FLS, hwMap.FLE, SwerveConfig.OFFSETS[0], SwerveConfig.INVERSIONS[0], logger),
                new SwerveModule(hwMap.FRM, hwMap.FRS, hwMap.FRE, SwerveConfig.OFFSETS[1], SwerveConfig.INVERSIONS[1], logger),
                new SwerveModule(hwMap.BRM, hwMap.BRS, hwMap.BRE, SwerveConfig.OFFSETS[2], SwerveConfig.INVERSIONS[2], logger),
                new SwerveModule(hwMap.BLM, hwMap.BLS, hwMap.BLE, SwerveConfig.OFFSETS[3], SwerveConfig.INVERSIONS[3], logger)
        }, hwMap.getVoltageSensor()::getVoltage, logger);
    }

    public SwerveDrivetrain(SwerveModule[] modules, DoubleSupplier batteryVoltageSupplier, Logger logger) {
        if (modules == null || modules.length != 4) {
            throw new IllegalArgumentException("SwerveDrivetrain requires exactly 4 modules.");
        }

        this.frontLeftModule = modules[0];
        this.frontRightModule = modules[1];
        this.backRightModule = modules[2];
        this.backLeftModule = modules[3];
        this.modules = modules;
        this.batteryVoltageSupplier = batteryVoltageSupplier != null ? batteryVoltageSupplier : () -> 12.0;
        this.logger = logger;
        this.kinematics = new SwerveKinematics();
        this.velocityObserver = new SwerveVelocityObserver(kinematics);

        for (SwerveModule module : modules) {
            // Keep motor output open-loop while still reading encoder velocity for the observer.
            // RUN_WITHOUT_ENCODER disables the hub's built-in closed-loop mode, not encoder access.
            module.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        }
    }

    public void setVelocity(Vector driverTarget, double dt) {
        setVelocityInternal(driverTarget.x(), driverTarget.y(), driverTarget.omega(), dt);
    }

    public void setVelocity(double x, double y, double omega, double dt) {
        setVelocityInternal(x, y, omega, dt);
    }

    public void refreshSensors() {
        for (SwerveModule module : modules) {
            module.refreshSensors();
        }
        velocityObserver.update(modules);
    }

    /**
     * Applies an autonomous chassis target without the teleop-oriented motion
     * smoother/input shaping layer.
     */
    public void setAutonomousVelocity(Vector normalizedTarget, double dt) {
        setVelocityInternal(normalizedTarget.x(), normalizedTarget.y(), normalizedTarget.omega(), dt);
    }

    public void setAutonomousVelocity(double x, double y, double omega, double dt) {
        setVelocityInternal(x, y, omega, dt);
    }

    private void setVelocityInternal(double driverX, double driverY, double driverOmega, double dt) {
        lastDriverTargetX = driverX;
        lastDriverTargetY = driverY;
        lastDriverTargetOmega = driverOmega;
        kinematics.setLoopTimeSec(dt);

        boolean hasInput = Math.hypot(driverX, driverY) > 0.01 || Math.abs(driverOmega) > 0.01;
        Vector chassisVelocity = toPhysicalChassisVelocity(driverX, driverY, driverOmega);
        lastVelocityX = chassisVelocity.x();
        lastVelocityY = chassisVelocity.y();
        lastVelocityOmega = chassisVelocity.omega();

        switch (state) {
            case DRIVING:
                driveWithPipeline(chassisVelocity, dt);
                if (!hasInput) {
                    lockTimerMs = 0.0;
                    state = States.WAITING_TO_LOCK;
                }
                break;

            case WAITING_TO_LOCK:
                if (hasInput) {
                    lockTimerMs = 0.0;
                    state = States.DRIVING;
                    driveWithPipeline(chassisVelocity, dt);
                } else {
                    driveWithPipeline(chassisVelocity, dt);
                    lockTimerMs += dt * 1000.0;
                    if (lockTimerMs > SwerveConfig.LOCK_DELAY_MS && chassisVelocity.magnitude() < 0.01) {
                        state = States.LOCKED;
                        resetSmoother();
                    }
                }
                break;

            case LOCKED:
                if (hasInput) {
                    lockTimerMs = 0.0;
                    state = States.DRIVING;
                    driveWithPipeline(chassisVelocity, dt);
                } else {
                    if (SwerveConfig.ENABLE_IDLE_X_STANCE) {
                        applyXStance(dt);
                    } else {
                        driveWithPipeline(0.0, 0.0, 0.0, dt);
                    }
                }
                break;
        }

        performHealthSystemScan();
    }

    private Vector toPhysicalChassisVelocity(double x, double y, double omega) {
        return new Vector(
                x * SwerveConfig.getMaxLinearSpeedMPS(),
                y * SwerveConfig.getMaxLinearSpeedMPS(),
                omega * SwerveConfig.MAX_ANGULAR_VELOCITY_RAD_S);
    }

    private void driveWithPipeline(Vector velocity, double dt) {
        driveWithPipeline(velocity.x(), velocity.y(), velocity.omega(), dt);
    }

    private void driveWithPipeline(double vx, double vy, double omega, double dt) {
        for (int i = 0; i < 4; i++) {
            currentAngles[i] = modules[i].getCurrentRotation();
        }

        lastTranslationAuthority = 1.0;
        kinematics.inverseKinematics(vx, vy, omega, workingStates);
        sanitizeStates(workingStates);
        desaturateStates(workingStates);
        copyInto(lastRawStates, workingStates);
        copyInto(lastOptimizedStates, workingStates);
        steerReadyForDrive = areModulesReadyForDrive(workingStates, currentAngles);
        SwerveModuleState[] commandedStates = workingStates;
        if (SwerveConfig.REQUIRE_STEER_READY_FOR_DRIVE && !steerReadyForDrive) {
            copyZeroDriveSpeeds(gatedStates, workingStates);
            commandedStates = gatedStates;
        }
        for (int i = 0; i < 4; i++) {
            modules[i].update(commandedStates[i], dt);
        }
    }

    private void applyXStance(double dt) {
        double[] lockAngles = SwerveConfig.LOCKED_STANCE_ANGLES_RAD;
        for (int i = 0; i < 4; i++) {
            double angle = lockAngles[i];
            xStates[i].speedMetersPerSecond = 0.0;
            xStates[i].angleRadians = angle;
            modules[i].update(angle, 0.0, dt);
        }
        copyInto(lastRawStates, xStates);
        copyInto(lastOptimizedStates, xStates);
        steerReadyForDrive = true;
    }

    private void performHealthSystemScan() {
        if (logger == null) {
            return;
        }
        for (int i = 0; i < 4; i++) {
            if (modules[i].isStalled()) {
                logger.log("HARDWARE_ALARM", "Critical stall detected on Module " + i, Logger.LogLevels.PRODUCTION);
            }
        }
    }

    public void log() {
        if (logger == null) {
            return;
        }
        for (int i = 0; i < 4; i++) {
            modules[i].log(i);
        }
        Vector actual = velocityObserver.getVelocity();
        logger.log("ObsV_X", actual.x(), Logger.LogLevels.PRODUCTION);
        logger.log("ObsV_Y", actual.y(), Logger.LogLevels.PRODUCTION);
        logger.log("ObsV_W", actual.omega(), Logger.LogLevels.PRODUCTION);
        logger.log("Battery_V", batteryVoltageSupplier.getAsDouble(), Logger.LogLevels.PRODUCTION);
    }

    public void setOffsets(double[] offsets) {
        for (int i = 0; i < 4 && i < offsets.length; i++) {
            modules[i].setOffset(offsets[i]);
        }
    }

    public void setInversions(boolean[] inversions) {
        for (int i = 0; i < 4 && i < inversions.length; i++) {
            modules[i].setInversion(inversions[i]);
        }
    }

    public void setMotorScaling(double[] scalars) {
        for (int i = 0; i < 4 && i < scalars.length; i++) {
            modules[i].setMotorScaling(scalars[i]);
        }
    }

    public Vector getActualVelocity() {
        return velocityObserver.getVelocity();
    }

    public Vector getLastDriverTarget() {
        return new Vector(lastDriverTargetX, lastDriverTargetY, lastDriverTargetOmega);
    }

    public Vector getLastSmoothedVelocity() {
        return new Vector(lastVelocityX, lastVelocityY, lastVelocityOmega);
    }

    public SwerveModuleState[] getLastRawStates() {
        return copyStates(lastRawStates);
    }

    public SwerveModuleState[] getLastOptimizedStates() {
        return copyStates(lastOptimizedStates);
    }

    public double getBatteryVoltage() {
        return batteryVoltageSupplier.getAsDouble();
    }

    public boolean isSteerReadyForDrive() {
        return steerReadyForDrive;
    }

    public double getLastTranslationAuthority() {
        return lastTranslationAuthority;
    }

    public States getState() {
        return state;
    }

    public void resetSmoother() {
        // Teleop now follows the simpler Kooky-style direct command path.
        // This method remains for compatibility with tests and bring-up tools.
    }

    private static SwerveModuleState[] copyStates(SwerveModuleState[] states) {
        SwerveModuleState[] copy = new SwerveModuleState[states.length];
        for (int i = 0; i < states.length; i++) {
            copy[i] = states[i].copy();
        }
        return copy;
    }

    private static void copyInto(SwerveModuleState[] destination, SwerveModuleState[] source) {
        for (int i = 0; i < source.length; i++) {
            destination[i].speedMetersPerSecond = source[i].speedMetersPerSecond;
            destination[i].angleRadians = source[i].angleRadians;
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

    private void sanitizeStates(SwerveModuleState[] states) {
        for (SwerveModuleState state : states) {
            if (Double.isNaN(state.speedMetersPerSecond) || Double.isInfinite(state.speedMetersPerSecond)) {
                state.speedMetersPerSecond = 0.0;
            }
            if (Double.isNaN(state.angleRadians) || Double.isInfinite(state.angleRadians)) {
                state.angleRadians = 0.0;
            } else {
                state.angleRadians = MathUtil.normalizeAngle(state.angleRadians);
            }
        }
    }

    private void desaturateStates(SwerveModuleState[] states) {
        double maxFound = 0.0;
        for (SwerveModuleState state : states) {
            maxFound = Math.max(maxFound, Math.abs(state.speedMetersPerSecond));
        }

        double maxLinearSpeedMps = SwerveConfig.getMaxLinearSpeedMPS();
        if (maxFound <= maxLinearSpeedMps || maxFound < 1e-9) {
            return;
        }

        double scale = maxLinearSpeedMps / maxFound;
        for (SwerveModuleState state : states) {
            state.speedMetersPerSecond *= scale;
        }
    }

    private boolean areModulesReadyForDrive(SwerveModuleState[] states, double[] currentAngles) {
        if (!SwerveConfig.REQUIRE_STEER_READY_FOR_DRIVE) {
            return true;
        }

        for (int i = 0; i < states.length; i++) {
            double error = Math.abs(MathUtil.angleError(currentAngles[i], states[i].angleRadians));
            if (error > SwerveConfig.STEER_READY_ANGLE_TOLERANCE_RAD) {
                return false;
            }
        }
        return true;
    }

    private static void copyZeroDriveSpeeds(SwerveModuleState[] destination, SwerveModuleState[] states) {
        for (int i = 0; i < states.length; i++) {
            destination[i].speedMetersPerSecond = 0.0;
            destination[i].angleRadians = states[i].angleRadians;
        }
    }

}
