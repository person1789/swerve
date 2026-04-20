package org.firstinspires.ftc.teamcode.Swerve.Hardware;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose;
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
 * control pipeline: Velocity Smoothing -> Kinematics -> Optimization -> Hardware.
 */
public class SwerveDrivetrain {

    /**
     * Drivetrain physical states for autonomous and teleop logic.
     */
    public enum States {
        DRIVING,          // Actively pursuing a velocity target.
        WAITING_TO_LOCK,  // Decelerating to a stop, preparing for X-stance.
        LOCKED            // Modules in X-stance to prevent external movement.
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
     * @param hwMap Hardware mapping wrapper for motor/servo references.
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
     * @param driverTarget Requested robot velocity (vx, vy, omega).
     * @param dt           Time since last update.
     */
    public void setPose(Pose driverTarget, double dt) {
        // Update velocity estimate from wheel feedback
        velocityObserver.update(modules);

        boolean hasInput = (Math.hypot(driverTarget.x, driverTarget.y) > 0.01 || Math.abs(driverTarget.heading) > 0.01);

        // 1. Enforce physical capability limits
        SwerveModuleState[] rawStates = kinematics.toModuleStates(driverTarget.x, driverTarget.y, driverTarget.heading);
        double maxFound = 0.0;
        for (SwerveModuleState s : rawStates) maxFound = Math.max(maxFound, Math.abs(s.speedMetersPerSecond));
        
        double scalingFactor = (maxFound > SwerveConfig.MAX_SPEED_MPS) ? SwerveConfig.MAX_SPEED_MPS / maxFound : 1.0;
        Pose systemLimit = new Pose(driverTarget.x * scalingFactor, driverTarget.y * scalingFactor, driverTarget.heading * scalingFactor);

        // 2. Apply S-Curve Motion Smoothing
        Pose smoothedPose = smoother.calculate(driverTarget, systemLimit, dt);

        // 3. Coordinate State Transitions
        double batteryVoltage = voltageSensor.getVoltage();

        switch (state) {
            case DRIVING:
                driveWithPipeline(smoothedPose, dt, batteryVoltage);
                if (!hasInput) {
                    lockTimer.reset();
                    state = States.WAITING_TO_LOCK;
                }
                break;

            case WAITING_TO_LOCK:
                driveWithPipeline(new Pose(0, 0, 0), dt, batteryVoltage);
                if (hasInput) state = States.DRIVING;
                else if (lockTimer.milliseconds() > SwerveConfig.LOCK_DELAY_MS) state = States.LOCKED;
                break;

            case LOCKED:
                applyXStance(dt, batteryVoltage);
                if (hasInput) state = States.DRIVING;
                break;
        }

        performHealthSystemScan();
    }

    /**
     * Low-level pipeline: Kinematics -> Optimize -> Hardware command.
     */
    private void driveWithPipeline(Pose pose, double dt, double batteryVoltage) {
        SwerveModuleState[] raw = kinematics.toModuleStates(pose.x, pose.y, pose.heading);
        double[] currentAngles = new double[4];
        for (int i = 0; i < 4; i++) currentAngles[i] = modules[i].getCurrentRotation();

        SwerveModuleState[] optimized = auditor.optimize(raw, currentAngles, SwerveConfig.MAX_SPEED_MPS);

        for (int i = 0; i < 4; i++) modules[i].update(optimized[i], dt, batteryVoltage);
    }

    private void applyXStance(double dt, double batteryVoltage) {
        double[] xAngles = { Math.toRadians(45), Math.toRadians(-45), Math.toRadians(45), Math.toRadians(-45) };
        for (int i = 0; i < 4; i++) modules[i].update(xAngles[i], 0.0, dt, batteryVoltage);
    }

    private void performHealthSystemScan() {
        for (int i = 0; i < 4; i++) {
            if (modules[i].isStalled()) {
                logger.log("HARDWARE_ALARM", "Critical stall detected on Module " + i, Logger.LogLevels.PRODUCTION);
            }
        }
    }

    public void log() {
        for (int i = 0; i < 4; i++) modules[i].log(i);
        Pose actual = velocityObserver.getVelocity();
        logger.log("ObsV_X", actual.x, Logger.LogLevels.PRODUCTION);
        logger.log("ObsV_Y", actual.y, Logger.LogLevels.PRODUCTION);
        logger.log("ObsV_W", actual.heading, Logger.LogLevels.PRODUCTION);
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

    public Pose getActualVelocity() { return velocityObserver.getVelocity(); }
    public States getState() { return state; }
}
