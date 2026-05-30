package org.firstinspires.ftc.teamcode.Swerve.Hardware;

import com.qualcomm.robotcore.hardware.AnalogInput;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.teamcode.Swerve.Core.MathUtil;
import org.firstinspires.ftc.teamcode.Swerve.Core.PIDController;
import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics.SwerveModuleState;

/**
 * Minimal Kooky-style swerve module: cache encoder once, flip if useful, command
 * the drive motor open-loop and the steering CRServo with one PID.
 */
public class SwerveModule {

    public interface HardwareAdapter {
        void read();

        double getRotationRadians();

        double getDriveVelocityTicksPerSecond();

        void setDrivePower(double power);

        void setSteerPower(double power);

        void setDriveMode(DcMotor.RunMode mode);
    }

    private final HardwareAdapter hardware;
    private final PIDController rotationController =
            new PIDController(SwerveConfig.STEER_P, SwerveConfig.STEER_I, SwerveConfig.STEER_D);

    private double offset;
    private boolean inverse;
    private double currentRotationRadians = 0.0;
    private double lastTargetAngleRadians = 0.0;
    private double lastDrivePower = 0.0;
    private double lastSteerPower = 0.0;
    private double lastSteerErrorRadians = 0.0;

    public SwerveModule(DcMotorEx driveMotor, CRServo steerServo, AnalogInput encoder,
            double offset, boolean inverse) {
        this(new FtcHardwareAdapter(driveMotor, steerServo, encoder), offset, inverse);
    }

    public SwerveModule(HardwareAdapter hardware, double offset, boolean inverse) {
        this.hardware = hardware;
        this.offset = offset;
        this.inverse = inverse;
        rotationController.enableContinuousInput(-Math.PI, Math.PI);
        read();
    }

    public void read() {
        hardware.read();
        double angle = MathUtil.normalizeAngle(hardware.getRotationRadians() - offset);
        currentRotationRadians = inverse ? MathUtil.normalizeAngle(-angle) : angle;
    }

    public void update(SwerveModuleState state, double dt) {
        update(state.angleRadians, state.speedMetersPerSecond, dt);
    }

    public void update(double targetAngleRadians, double targetSpeedMps, double dt) {
        double optimizedAngle = MathUtil.normalizeAngle(targetAngleRadians);
        double optimizedSpeed = targetSpeedMps;
        double error = MathUtil.angleError(currentRotationRadians, optimizedAngle);

        if (Math.abs(error) > SwerveConfig.FLIP_THRESHOLD) {
            optimizedAngle = MathUtil.normalizeAngle(optimizedAngle + Math.PI);
            optimizedSpeed *= -1.0;
            error = MathUtil.angleError(currentRotationRadians, optimizedAngle);
        }

        rotationController.setPID(SwerveConfig.STEER_P, SwerveConfig.STEER_I, SwerveConfig.STEER_D);
        
        double steerPower = 0.0;
        if (Math.abs(error) >= SwerveConfig.STEER_DEADBAND_RAD) {
            steerPower = rotationController.calculate(currentRotationRadians, optimizedAngle, Math.max(1e-3, dt));
        }

        double drivePower = targetSpeedMpsToPower(optimizedSpeed);

        lastTargetAngleRadians = optimizedAngle;
        lastSteerErrorRadians = error;
        lastDrivePower = drivePower;
        lastSteerPower = steerPower;
        hardware.setSteerPower(steerPower);
        hardware.setDrivePower(drivePower);
    }

    public double getCurrentRotation() {
        return currentRotationRadians;
    }

    public double getVelocityMps() {
        return driveTicksPerSecondToMetersPerSecond(hardware.getDriveVelocityTicksPerSecond());
    }

    public double getLastTargetAngleRadians() {
        return lastTargetAngleRadians;
    }

    public double getLastDrivePower() {
        return lastDrivePower;
    }

    public double getLastSteerPower() {
        return lastSteerPower;
    }

    public double getSteerErrorRadians() {
        return lastSteerErrorRadians;
    }

    public boolean isAzimuthReady(double toleranceRad) {
        return Math.abs(lastSteerErrorRadians) <= Math.abs(toleranceRad);
    }

    public void setMode(DcMotor.RunMode mode) {
        hardware.setDriveMode(mode);
    }

    public void setOffset(double offset) {
        this.offset = offset;
    }

    public void setInversion(boolean inverse) {
        this.inverse = inverse;
    }

    public static double driveTicksPerSecondToMetersPerSecond(double ticksPerSecond) {
        double wheelRps = (ticksPerSecond / SwerveConfig.DRIVE_TICKS_PER_REV) / SwerveConfig.DRIVE_GEAR_RATIO;
        return wheelRps * (2.0 * Math.PI * SwerveConfig.WHEEL_RADIUS_METERS);
    }

    private static double targetSpeedMpsToPower(double speedMetersPerSecond) {
        double maxSpeed = SwerveConfig.getMaxLinearSpeedMPS();
        if (maxSpeed < 1e-9) {
            return 0.0;
        }
        return speedMetersPerSecond / maxSpeed;
    }

    private static class FtcHardwareAdapter implements HardwareAdapter {
        private final DcMotorEx driveMotor;
        private final CRServo steerServo;
        private final AnalogInput encoder;
        private double rotationRadians;

        FtcHardwareAdapter(DcMotorEx driveMotor, CRServo steerServo, AnalogInput encoder) {
            this.driveMotor = driveMotor;
            this.steerServo = steerServo;
            this.encoder = encoder;
            this.driveMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        }

        @Override
        public void read() {
            rotationRadians = MathUtil.normalizeAngle((encoder.getVoltage() / 3.3) * 2.0 * Math.PI);
        }

        @Override
        public double getRotationRadians() {
            return rotationRadians;
        }

        @Override
        public double getDriveVelocityTicksPerSecond() {
            return driveMotor.getVelocity();
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
    }
}
