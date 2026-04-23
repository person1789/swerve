package org.firstinspires.ftc.teamcode.Swerve.Hardware;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Vector;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics.SwerveAuditor;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics.SwerveKinematics;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics.SwerveModuleState;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Localization.SwerveVelocityObserver;
import org.firstinspires.ftc.teamcode.Swerve.Input.MotionSmoother;
import org.firstinspires.ftc.teamcode.Swerve.Core.Logger;

/**
 * SwerveDrivetrain
 * 
 * Central coordinator for the swerve drive system. Manages the high-level
 * control pipeline: Velocity Smoothing -> Kinematics -> Optimization ->
 * Hardware.
 */
public class SwerveDrivetrain {

    public enum States {
        DRIVING, // Actively pursuing a velocity target.
        WAITING_TO_LOCK, // Decelerating to a stop, preparing for X-stance.
        LOCKED // Modules in X-stance to prevent external movement.
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

    private States state = States.DRIVING;
    private final ElapsedTime lockTimer = new ElapsedTime();
    private final com.qualcomm.robotcore.hardware.VoltageSensor voltageSensor;
    private final Logger logger;

    /**
     * @param hwMap  Hardware mapping wrapper for motor/servo references.
     * @param logger Telemetry wrapper for diagnostic logging.
     */
    public SwerveDrivetrain(HWMap hwMap, Logger logger) {
        this.logger = logger;
        this.voltageSensor = hwMap.getVoltageSensor();
        this.kinematics = new SwerveKinematics();
        this.auditor = new SwerveAuditor();
        this.smoother = new MotionSmoother();
        this.velocityObserver = new SwerveVelocityObserver(kinematics);

        // Hardware initialization from central config
        frontLeftModule = new SwerveModule(hwMap.FLM, hwMap.FLS, hwMap.FLE,
                SwerveConfig.OFFSETS[0], SwerveConfig.INVERSIONS[0], logger);
        frontRightModule = new SwerveModule(hwMap.FRM, hwMap.FRS, hwMap.FRE,
                SwerveConfig.OFFSETS[1], SwerveConfig.INVERSIONS[1], logger);
        backRightModule = new SwerveModule(hwMap.BRM, hwMap.BRS, hwMap.BRE,
                SwerveConfig.OFFSETS[2], SwerveConfig.INVERSIONS[2], logger);
        backLeftModule = new SwerveModule(hwMap.BLM, hwMap.BLS, hwMap.BLE,
                SwerveConfig.OFFSETS[3], SwerveConfig.INVERSIONS[3], logger);

        modules = new SwerveModule[] { frontLeftModule, frontRightModule, backRightModule, backLeftModule };

        for (SwerveModule m : modules) {
            m.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        }
    }

    /**
     * Processes driver intent through the control pipeline to update hardware.
     * 
     * @param driverTarget Requested robot velocity Vector (vx, vy, omega).
     * @param dt           Time since last update.
     */
    public void setVelocity(Vector driverTarget, double dt) {
        // Update velocity estimate from wheel feedback
        velocityObserver.update(modules);

        boolean hasInput = driverTarget.magnitude() > 0.01;

        // 1. Apply S-Curve Motion Smoothing (Sole authority for ramps)
        Vector chassisVelocity = smoother.smooth(driverTarget, dt);

        switch (state) {
            case DRIVING:
                driveWithPipeline(chassisVelocity, dt);
                if (!hasInput) {
                    lockTimer.reset();
                    state = States.WAITING_TO_LOCK;
                }
                break;

            case WAITING_TO_LOCK:
                // Don't command zero — let the smoother drain to zero naturally.
                driveWithPipeline(chassisVelocity, dt);
                if (hasInput) {
                    state = States.DRIVING;
                } else if (lockTimer.milliseconds() > SwerveConfig.LOCK_DELAY_MS
                        && chassisVelocity.magnitude() < 0.01) {
                    // Only lock when the robot has actually stopped
                    state = States.LOCKED;
                }
                break;

            case LOCKED:
                applyXStance(dt);
                if (hasInput)
                    state = States.DRIVING;
                break;
        }

        performHealthSystemScan();
    }

    /**
     * Low-level pipeline: Kinematics -> Optimize -> Hardware command.
     */
    private void driveWithPipeline(Vector velocity, double dt) {
        SwerveModuleState[] raw = kinematics.inverseKinematics(velocity);
        double[] currentAngles = new double[4];
        for (int i = 0; i < 4; i++)
            currentAngles[i] = modules[i].getCurrentRotation();

        SwerveModuleState[] optimized = auditor.optimize(raw, currentAngles);

        for (int i = 0; i < 4; i++)
            modules[i].update(optimized[i], dt);
    }

    private void applyXStance(double dt) {
        double[] xAngles = { Math.toRadians(45), Math.toRadians(-45), Math.toRadians(45), Math.toRadians(-45) };
        for (int i = 0; i < 4; i++)
            modules[i].update(xAngles[i], 0.0, dt);
    }

    private void performHealthSystemScan() {
        for (int i = 0; i < 4; i++) {
            if (modules[i].isStalled()) {
                logger.log("HARDWARE_ALARM", "Critical stall detected on Module " + i, Logger.LogLevels.PRODUCTION);
            }
        }
    }

    public void log() {
        for (int i = 0; i < 4; i++)
            modules[i].log(i);
        Vector actual = velocityObserver.getVelocity();
        logger.log("ObsV_X", actual.x(), Logger.LogLevels.PRODUCTION);
        logger.log("ObsV_Y", actual.y(), Logger.LogLevels.PRODUCTION);
        logger.log("ObsV_W", actual.omega(), Logger.LogLevels.PRODUCTION);
        logger.log("Battery_V", voltageSensor.getVoltage(), Logger.LogLevels.PRODUCTION);
    }

    public void setOffsets(double[] offsets) {
        for (int i = 0; i < 4 && i < offsets.length; i++) {
            modules[i].setOffset(offsets[i]);
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

    public States getState() {
        return state;
    }
}
