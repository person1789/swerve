package org.firstinspires.ftc.teamcode.Swerve.Hardware;

import com.qualcomm.robotcore.hardware.AnalogInput;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit;
import org.firstinspires.ftc.teamcode.Swerve.Core.MathUtil;
import org.firstinspires.ftc.teamcode.Swerve.Core.PIDController;
import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics.SwerveModuleState;
import org.firstinspires.ftc.teamcode.Swerve.Core.Logger;

/**
 * SwerveModule
 * 
 * Manages a single coaxial swerve pod, integrating drive and steer actuators 
 * with a physics-informed control model.
 */
public class SwerveModule {

    private final DcMotorEx driveMotor;
    private final CRServo steerServo;
    private final AnalogInput encoder;
    private final Logger logger;

    private double offset;
    private boolean inverse;
    private double motorScaling = 1.0;

    private double lastTargetAngleRad = 0.0;
    private double lastTargetVelocityMps = 0.0;
    private double lastDrivePower = 0.0;

    private final PIDController rotationController;

    /**
     * @param driveMotor Motor used for wheel propulsion.
     * @param steerServo Continuous rotation servo for orientation.
     * @param encoder    Absolute analog encoder for feedback.
     * @param offset     Zero-position calibration offset.
     * @param inverse    Directional inversion flag for steering.
     * @param logger     System-wide telemetry logger.
     */
    public SwerveModule(DcMotorEx driveMotor, CRServo steerServo, AnalogInput encoder,
                        double offset, boolean inverse, Logger logger) {
        this.driveMotor = driveMotor;
        this.steerServo = steerServo;
        this.encoder = encoder;
        this.offset = offset;
        this.inverse = inverse;
        this.logger = logger;
        
        this.rotationController = new PIDController(SwerveConfig.STEER_P, SwerveConfig.STEER_I, SwerveConfig.STEER_D);
        this.driveMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
    }

    /**
     * Updates the module state using a physics-based voltage feedforward model 
     * with active battery voltage compensation.
     * 
     * @param targetAngle    Desired module orientation in radians.
     * @param driveSpeedMps  Desired wheel velocity in meters per second.
     * @param dt             Loop delta-time for acceleration derivation.
     * @param batteryVoltage Actual measured battery voltage for power normalization.
     */
    public void update(double targetAngle, double driveSpeedMps, double dt, double batteryVoltage) {
        double currentAngle = getCurrentRotation();
        double error = MathUtil.angleError(currentAngle, targetAngle);

        // Efficiency optimization: Scale power by the cosine of the heading error.
        double cosScaler = Math.max(0, Math.cos(error));
        
        // Derive target acceleration for the kA feedforward term.
        double targetAccel = (driveSpeedMps - lastTargetVelocityMps) / dt;
        
        // Physics-informed Voltage Feedforward Model (V = kS + kV*v + kA*a)
        double ffVoltage = SwerveConfig.DRIVE_KS * Math.signum(driveSpeedMps) +
                           SwerveConfig.DRIVE_KV * driveSpeedMps +
                           SwerveConfig.DRIVE_KA * targetAccel;
                           
        // Normalization: In FTC, motor.setPower(1.0) applies all available battery voltage.
        // To achieve a specific voltage target, we divide by the actual battery reading.
        // We ensure batteryVoltage is never zero to avoid division errors.
        double safeBatteryVoltage = Math.max(8.0, batteryVoltage);
        double drivePower = (ffVoltage / safeBatteryVoltage) * cosScaler;

        // Execute Steering PID control loop.
        rotationController.setPID(SwerveConfig.STEER_P, SwerveConfig.STEER_I, SwerveConfig.STEER_D);
        rotationController.setSetpoint(targetAngle);
        double pidOut = rotationController.calculate(currentAngle, dt);

        double steeringPower;
        if (Math.abs(error) < 0.02) {
            steeringPower = 0.0;
        } else {
            // Apply steering static friction feedforward.
            steeringPower = Range.clip(pidOut + Math.signum(error) * SwerveConfig.STEER_STATIC_FF, -1.0, 1.0);
        }

        // Cache state for next acceleration derivative.
        lastTargetAngleRad = targetAngle;
        lastTargetVelocityMps = driveSpeedMps;
        lastDrivePower = drivePower;

        steerServo.setPower(steeringPower);
        driveMotor.setPower(Range.clip(drivePower * motorScaling, -1.0, 1.0));
    }

    public void update(SwerveModuleState state, double dt, double batteryVoltage) {
        update(state.angleRadians, state.speedMetersPerSecond, dt, batteryVoltage);
    }

    /**
     * Resolves the absolute module rotation using the analog encoder and offset.
     */
    public double getCurrentRotation() {
        double voltage = encoder.getVoltage();
        double angle = (voltage / 3.3) * 2.0 * Math.PI;
        double result = MathUtil.normalizeAngle(angle - offset);
        return inverse ? -result : result;
    }

    /**
     * Computes the current wheel velocity in meters per second.
     */
    public double getVelocityMps() {
        double tps = driveMotor.getVelocity();
        double wheelRps = (tps / SwerveConfig.DRIVE_TICKS_PER_REV) / SwerveConfig.DRIVE_GEAR_RATIO;
        return wheelRps * (2 * Math.PI * SwerveConfig.WHEEL_RADIUS_METERS);
    }

    /**
     * Returns the instantaneous current draw of the drive motor.
     */
    public double getCurrentAmps() {
        return driveMotor.getCurrent(CurrentUnit.AMPS);
    }

    /**
     * Predictive stall detection logic: Flags a mechanical jam if high power is 
     * applied without corresponding movement, accompanied by high current draw.
     */
    public boolean isStalled() {
        return Math.abs(lastDrivePower) > 0.3 && 
               Math.abs(getVelocityMps()) < 0.05 && 
               getCurrentAmps() > SwerveConfig.DRIVE_CURRENT_THRESHOLD;
    }

    public SwerveModuleState getCurrentState() {
        return new SwerveModuleState(getVelocityMps(), getCurrentRotation());
    }

    public void log(int index) {
        if (logger == null) return;
        String prefix = "Mod" + index + " ";
        logger.log(prefix + "TargetDeg", Math.toDegrees(lastTargetAngleRad), Logger.LogLevels.PRODUCTION);
        logger.log(prefix + "CurrentDeg", Math.toDegrees(getCurrentRotation()), Logger.LogLevels.PRODUCTION);
        logger.log(prefix + "CurrentAmps", getCurrentAmps(), Logger.LogLevels.PRODUCTION);
        if (isStalled()) logger.log(prefix + "HEALTH_ALARM", 1.0, Logger.LogLevels.PRODUCTION);
    }

    public void setOffset(double offset) { this.offset = offset; }
    public void setMotorScaling(double scaling) { this.motorScaling = scaling; }
    public void setMode(DcMotor.RunMode mode) { driveMotor.setMode(mode); }
}
