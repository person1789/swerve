/**
 * SwerveModule: Controls a single wheel assembly on the robot.
 * It manages two main jobs: spinning the wheel at the correct speed and rotating 
 * the entire wheel assembly to the correct direction using sensors and motors.
 */
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
import org.firstinspires.ftc.teamcode.core.Logger;

/**
 * SwerveModule — Phase C
 *
 * Controls a single coaxial swerve pod.
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
    private double lastDrivePower = 0.0;

    // Use our custom PID controller
    private final PIDController rotationController;

    /**
     * Create a SwerveModule with hardware references and calibration data.
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
     * Command the module using a SwerveModuleState.
     */
    public void update(SwerveModuleState state) {
        update(state.angleRadians, state.speedMetersPerSecond);
    }

    /**
     * Command the module with target angle (rad) and drive speed (mps).
     */
    public void update(double targetAngle, double driveSpeed) {
        double currentAngle = getCurrentRotation();
        double error = MathUtil.angleError(currentAngle, targetAngle);

        // Cosine scaler: reduce drive power proportional to steering error
        double cosScaler = Math.cos(error);
        
        // Convert speed m/s to normalized power [-1, 1] using SwerveConfig
        double drivePower = (driveSpeed / SwerveConfig.MAX_SPEED_MPS) * cosScaler;

        // Steering PID
        rotationController.setPID(SwerveConfig.STEER_P, SwerveConfig.STEER_I, SwerveConfig.STEER_D);
        rotationController.setSetpoint(targetAngle);
        
        double pidOut = rotationController.calculate(currentAngle, SwerveConfig.LOOP_TIME_SEC);

        double steeringPower;
        if (Math.abs(error) < 0.02) { // Small deadzone
            steeringPower = 0.0;
        } else {
            // Apply static friction feedforward in direction of error
            steeringPower = Range.clip(pidOut + Math.signum(error) * SwerveConfig.STEER_STATIC_FF, -1.0, 1.0);
        }

        lastTargetAngleRad = targetAngle;
        lastDrivePower = drivePower;

        steerServo.setPower(steeringPower);
        driveMotor.setPower(drivePower * motorScaling);
    }

    public double getCurrentRotation() {
        double rawRad = (encoder.getVoltage() / 3.3) * (2.0 * Math.PI);
        if (inverse) rawRad = (2.0 * Math.PI) - rawRad;
        return MathUtil.normalizeAngle(rawRad - offset);
    }

    public void setMode(DcMotor.RunMode mode) { driveMotor.setMode(mode); }
    public void setOffset(double offset) { this.offset = offset; }
    public void setInverse(boolean inverse) { this.inverse = inverse; }
    public void setMotorScaling(double scaling) { this.motorScaling = scaling; }

    public void log(int index) {
        if (logger == null) return;
        String prefix = "Mod" + index + " ";
        logger.log(prefix + "TargetDeg", Math.toDegrees(lastTargetAngleRad), Logger.LogLevels.PRODUCTION);
        logger.log(prefix + "CurrentDeg", Math.toDegrees(getCurrentRotation()), Logger.LogLevels.PRODUCTION);
    }
}