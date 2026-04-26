package org.firstinspires.ftc.teamcode.Swerve.Hardware;

import com.qualcomm.robotcore.hardware.DcMotor;

import java.util.function.DoubleSupplier;

import org.firstinspires.ftc.teamcode.Swerve.Core.Logger;
import org.firstinspires.ftc.teamcode.Swerve.Core.MathUtil;
import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Vector;
import org.firstinspires.ftc.teamcode.Swerve.Input.MotionSmoother;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics.SwerveAuditor;
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
    private final SwerveAuditor auditor;
    private final MotionSmoother smoother;
    private final SwerveVelocityObserver velocityObserver;
    private final DoubleSupplier batteryVoltageSupplier;
    private final Logger logger;

    private States state = States.DRIVING;
    private double lockTimerMs = 0.0;
    private Vector lastDriverTarget = new Vector(0, 0, 0);
    private Vector lastSmoothedVelocity = new Vector(0, 0, 0);
    private double lastTranslationAuthority = 1.0;
    private SwerveModuleState[] lastRawStates = zeroStates();
    private SwerveModuleState[] lastOptimizedStates = zeroStates();
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
        this.auditor = new SwerveAuditor();
        this.smoother = new MotionSmoother();
        this.velocityObserver = new SwerveVelocityObserver(kinematics);

        for (SwerveModule module : modules) {
            // Keep motor output open-loop while still reading encoder velocity for the observer.
            // RUN_WITHOUT_ENCODER disables the hub's built-in closed-loop mode, not encoder access.
            module.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        }
    }

    public void setVelocity(Vector driverTarget, double dt) {
        setVelocityInternal(driverTarget, dt, true);
    }

    /**
     * Applies an autonomous chassis target without the teleop-oriented motion
     * smoother/input shaping layer.
     */
    public void setAutonomousVelocity(Vector normalizedTarget, double dt) {
        setVelocityInternal(normalizedTarget, dt, false);
    }

    private void setVelocityInternal(Vector driverTarget, double dt, boolean useSmoother) {
        velocityObserver.update(modules);
        lastDriverTarget = driverTarget;
        kinematics.setLoopTimeSec(dt);

        boolean hasInput = driverTarget.magnitude() > 0.01;
        Vector chassisVelocity = useSmoother
                ? smoother.smooth(driverTarget, dt)
                : toPhysicalChassisVelocity(driverTarget);
        lastSmoothedVelocity = chassisVelocity;

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
                    resetSmoother();
                    // Recalculate chassisVelocity after reset to start ramping from 0 in this tick
                    Vector freshVelocity = smoother.smooth(driverTarget, dt);
                    driveWithPipeline(freshVelocity, dt);
                } else {
                    if (SwerveConfig.ENABLE_IDLE_X_STANCE) {
                        applyXStance(dt);
                    } else {
                        driveWithPipeline(new Vector(0, 0, 0), dt);
                    }
                }
                break;
        }

        performHealthSystemScan();
    }

    private Vector toPhysicalChassisVelocity(Vector normalizedTarget) {
        return new Vector(
                normalizedTarget.x() * SwerveConfig.getMaxLinearSpeedMPS(),
                normalizedTarget.y() * SwerveConfig.getMaxLinearSpeedMPS(),
                normalizedTarget.omega() * SwerveConfig.MAX_ANGULAR_VELOCITY_RAD_S);
    }

    private void driveWithPipeline(Vector velocity, double dt) {
        double[] currentAngles = new double[4];
        for (int i = 0; i < 4; i++) {
            currentAngles[i] = modules[i].getCurrentRotation();
        }

        Vector commandedVelocity = applyFeasibleTranslationFilter(velocity, currentAngles);
        SwerveModuleState[] raw = kinematics.inverseKinematics(commandedVelocity);
        lastRawStates = copyStates(raw);
        SwerveModuleState[] optimized = auditor.optimize(raw, currentAngles);
        lastOptimizedStates = copyStates(optimized);
        steerReadyForDrive = areModulesReadyForDrive(optimized, currentAngles);
        SwerveModuleState[] commandedStates = (SwerveConfig.REQUIRE_STEER_READY_FOR_DRIVE && !steerReadyForDrive)
                ? zeroDriveSpeeds(optimized)
                : optimized;
        for (int i = 0; i < 4; i++) {
            modules[i].update(commandedStates[i], dt);
        }
    }

    private void applyXStance(double dt) {
        double[] xAngles = { Math.toRadians(45), Math.toRadians(-45), Math.toRadians(45), Math.toRadians(-45) };
        SwerveModuleState[] xStates = new SwerveModuleState[4];
        for (int i = 0; i < 4; i++) {
            xStates[i] = new SwerveModuleState(0.0, xAngles[i]);
            modules[i].update(xAngles[i], 0.0, dt);
        }
        lastRawStates = copyStates(xStates);
        lastOptimizedStates = copyStates(xStates);
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
        return lastDriverTarget;
    }

    public Vector getLastSmoothedVelocity() {
        return lastSmoothedVelocity;
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
        smoother.reset();
    }

    private static SwerveModuleState[] copyStates(SwerveModuleState[] states) {
        SwerveModuleState[] copy = new SwerveModuleState[states.length];
        for (int i = 0; i < states.length; i++) {
            copy[i] = states[i].copy();
        }
        return copy;
    }

    private static SwerveModuleState[] zeroStates() {
        return new SwerveModuleState[] {
                new SwerveModuleState(),
                new SwerveModuleState(),
                new SwerveModuleState(),
                new SwerveModuleState()
        };
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

    private static SwerveModuleState[] zeroDriveSpeeds(SwerveModuleState[] states) {
        SwerveModuleState[] gated = new SwerveModuleState[states.length];
        for (int i = 0; i < states.length; i++) {
            gated[i] = new SwerveModuleState(0.0, states[i].angleRadians);
        }
        return gated;
    }

    private Vector applyFeasibleTranslationFilter(Vector velocity, double[] currentAngles) {
        if (!SwerveConfig.FEASIBLE_TRANSLATION_FILTER_ENABLED) {
            lastTranslationAuthority = 1.0;
            return velocity;
        }

        double desiredTranslationMag = Math.hypot(velocity.x(), velocity.y());
        if (desiredTranslationMag < 1e-6) {
            lastTranslationAuthority = 1.0;
            return velocity;
        }

        Vector feasibleVelocity = kinematics.projectToCurrentAngleFeasibleVelocity(
                velocity,
                currentAngles,
                SwerveConfig.FEASIBLE_TRANSLATION_PENALTY);
        Vector desiredDirection = new Vector(velocity.x(), velocity.y()).scale(1.0 / desiredTranslationMag);
        double feasibleAlongDesired = feasibleVelocity.x() * desiredDirection.x() + feasibleVelocity.y() * desiredDirection.y();
        double authority = MathUtil.clamp(
                feasibleAlongDesired / desiredTranslationMag,
                SwerveConfig.FEASIBLE_TRANSLATION_MIN_AUTHORITY,
                1.0);

        lastTranslationAuthority = authority;
        return new Vector(
                velocity.x() * authority,
                velocity.y() * authority,
                velocity.omega());
    }
}
