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

    private final SwerveModuleIO io;
    private final Logger logger;

    private double offset;
    private boolean inverse;

    private double lastTargetAngleRad = 0.0;
    private double lastTargetVelocityMps = 0.0;
    private double lastDrivePower = 0.0;
    private double lastSteeringPower = 0.0;

    private final PIDController rotationController;
    private double motorScaling = 1.0;

    /**
     * @param driveMotor Driving motor.
     * @param steerServo Steering servo.
     * @param encoder    Absolute encoder for steering angle.
     * @param offset     Encoder offset to match physical zero to encoder reading.
     * @param inverse    Flips direction the encoder reads.
     * @param logger     Logger for telemetry.
     */
    public SwerveModule(DcMotorEx driveMotor, CRServo steerServo, AnalogInput encoder,
            double offset, boolean inverse, Logger logger) {
        this(new FtcSwerveModuleIO(driveMotor, steerServo, encoder), offset, inverse, logger);
    }

    public SwerveModule(SwerveModuleIO io, double offset, boolean inverse, Logger logger) {
        this.io = io;
        this.offset = offset;
        this.inverse = inverse;
        this.logger = logger;

        this.rotationController = new PIDController(SwerveConfig.STEER_P, SwerveConfig.STEER_I, SwerveConfig.STEER_D);
        this.io.setCalibration(offset, inverse);
    }

    /**
     * Updates the module state using simple linear power scaling.
     * 
     * @param targetAngle   Desired module orientation in radians.
     * @param driveSpeedMps Desired wheel velocity in meters per second.
     * @param dt            Loop delta-time for PID calculation.
     */
    public void update(double targetAngle, double driveSpeedMps, double dt) {
        double currentAngle = getCurrentRotation();
        double error = MathUtil.angleError(currentAngle, targetAngle);

        double drivePower = (driveSpeedMps / SwerveConfig.MAX_SPEED_MPS);

        rotationController.setPID(SwerveConfig.STEER_P, SwerveConfig.STEER_I, SwerveConfig.STEER_D);
        rotationController.setSetpoint(0.0);
        double pidOut = rotationController.calculate(-error, dt);

        double steeringPower;
        if (Math.abs(error) < 0.02) {
            steeringPower = 0.0;
        } else {
            steeringPower = Range.clip(pidOut, -1.0, 1.0);
        }

        // Cache state for telemetry/debugging
        lastTargetAngleRad = targetAngle;
        lastTargetVelocityMps = driveSpeedMps;
        lastDrivePower = drivePower;
        lastSteeringPower = steeringPower;

        io.setSteerPower(steeringPower);
        io.setDrivePower(Range.clip(drivePower * motorScaling, -1.0, 1.0));
    }

    public void update(SwerveModuleState state, double dt) {
        update(state.angleRadians, state.speedMetersPerSecond, dt);
    }

    /**
     * Resolves the absolute module rotation using the analog encoder and offset.
     */
    public double getCurrentRotation() {
        return io.getCurrentRotationRadians();
    }

    /**
     * Computes the current wheel velocity in meters per second.
     * This remains encoder-derived even when the drive motor is commanded in
     * RUN_WITHOUT_ENCODER mode.
     */
    public double getVelocityMps() {
        return io.getDriveVelocityMetersPerSecond();
    }

    /**
     * Returns the instantaneous current draw of the drive motor.
     */
    public double getCurrentAmps() {
        return io.getDriveCurrentAmps();
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
        if (logger == null)
            return;
        String prefix = "Mod" + index + " ";

        // Steering Performance
        logger.log(prefix + "TargetDeg", Math.toDegrees(lastTargetAngleRad), Logger.LogLevels.PRODUCTION);
        logger.log(prefix + "CurrentDeg", Math.toDegrees(getCurrentRotation()), Logger.LogLevels.PRODUCTION);

        // Drive Performance
        logger.log(prefix + "TargetVelMps", lastTargetVelocityMps, Logger.LogLevels.PRODUCTION);
        logger.log(prefix + "ActualVelMps", getVelocityMps(), Logger.LogLevels.PRODUCTION);
        logger.log(prefix + "DrivePower", lastDrivePower, Logger.LogLevels.PRODUCTION);
        logger.log(prefix + "SteerPower", lastSteeringPower, Logger.LogLevels.DEBUG);

        // Hardware Health & Calibration
        logger.log(prefix + "CurrentAmps", getCurrentAmps(), Logger.LogLevels.PRODUCTION);

        if (isStalled())
            logger.log(prefix + "HEALTH_ALARM", 1.0, Logger.LogLevels.PRODUCTION);
    }

    public void setOffset(double offset) {
        this.offset = offset;
        io.setCalibration(offset, inverse);
    }

    public void setMode(DcMotor.RunMode mode) {
        io.setDriveMode(mode);
    }

    public void setMotorScaling(double scalar) {
        this.motorScaling = MathUtil.clamp(scalar, 0.0, 1.0);
    }

    public double getLastTargetAngleRad() {
        return lastTargetAngleRad;
    }

    public double getLastTargetVelocityMps() {
        return lastTargetVelocityMps;
    }

    public double getLastDrivePower() {
        return lastDrivePower;
    }

    public double getLastSteeringPower() {
        return lastSteeringPower;
    }

    private static class FtcSwerveModuleIO implements SwerveModuleIO {
        private final DcMotorEx driveMotor;
        private final CRServo steerServo;
        private final AnalogInput encoder;
        private double offset;
        private boolean inverse;

        FtcSwerveModuleIO(DcMotorEx driveMotor, CRServo steerServo, AnalogInput encoder) {
            this.driveMotor = driveMotor;
            this.steerServo = steerServo;
            this.encoder = encoder;
            this.driveMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        }

        @Override
        public double getCurrentRotationRadians() {
            double voltage = encoder.getVoltage();
            double angle = (voltage / 3.3) * 2.0 * Math.PI;
            double result = MathUtil.normalizeAngle(angle - offset);
            return inverse ? -result : result;
        }

        @Override
        public double getDriveVelocityMetersPerSecond() {
            double tps = driveMotor.getVelocity();
            double wheelRps = (tps / SwerveConfig.DRIVE_TICKS_PER_REV) / SwerveConfig.DRIVE_GEAR_RATIO;
            return wheelRps * (2 * Math.PI * SwerveConfig.WHEEL_RADIUS_METERS);
        }

        @Override
        public double getDriveCurrentAmps() {
            return driveMotor.getCurrent(CurrentUnit.AMPS);
        }

        @Override
        public void setDrivePower(double power) {
            driveMotor.setPower(power);
        }

        @Override
        public void setSteerPower(double power) {
            steerServo.setPower(power);
        }

        @Override
        public void setDriveMode(DcMotor.RunMode mode) {
            driveMotor.setMode(mode);
        }

        @Override
        public void setCalibration(double offsetRadians, boolean inverse) {
            this.offset = offsetRadians;
            this.inverse = inverse;
        }
    }
}
