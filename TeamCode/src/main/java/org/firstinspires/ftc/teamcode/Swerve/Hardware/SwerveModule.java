package org.firstinspires.ftc.teamcode.Swerve.Hardware;

import com.arcrobotics.ftclib.controller.PIDFController;
import com.qualcomm.robotcore.hardware.AnalogInput;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.CRServoImplEx;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;
import com.qualcomm.robotcore.hardware.PwmControl;
import com.qualcomm.robotcore.hardware.configuration.typecontainers.MotorConfigurationType;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.teamcode.Swerve.Core.MathUtil;
import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;

import java.util.Locale;

public class SwerveModule {
    public static double MAX_SERVO = 1, MAX_MOTOR = 1;
    public static boolean MOTOR_FLIPPING = true;

    private DcMotorEx motor;
    private CRServo servo;
    private AbsoluteAnalogEncoder encoder;
    private PIDFController rotationController;

    public boolean wheelFlipped = false;
    private double target = 0.0;
    private double position = 0.0;
    public double lastMotorPower = 0;

    public SwerveModule(DcMotorEx m, CRServo s, AbsoluteAnalogEncoder e) {
        motor = m;
        MotorConfigurationType motorConfigurationType = motor.getMotorType().clone();
        motorConfigurationType.setAchieveableMaxRPMFraction(MAX_MOTOR);
        motor.setMotorType(motorConfigurationType);
        motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        servo = s;
        if (servo instanceof CRServoImplEx) {
            ((CRServoImplEx) servo).setPwmRange(new PwmControl.PwmRange(500, 2500, 5000));
        }

        encoder = e;
        rotationController = new PIDFController(SwerveConfig.STEER_P, SwerveConfig.STEER_I, SwerveConfig.STEER_D, 0);
        motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
    }

    public SwerveModule(HardwareMap hardwareMap, String mName, String sName, String eName) {
        this(hardwareMap.get(DcMotorEx.class, mName),
                hardwareMap.get(CRServo.class, sName),
                new AbsoluteAnalogEncoder(hardwareMap.get(AnalogInput.class, eName)));
    }

    public void read() {
        position = encoder.getCurrentPosition();
    }

    public void update() {
        rotationController.setPIDF(SwerveConfig.STEER_P, SwerveConfig.STEER_I, SwerveConfig.STEER_D, 0);
        double targetRot = getTargetRotation();
        double currentRot = getModuleRotation();

        double error = MathUtil.normalizeAngle(targetRot - currentRot);
        if (MOTOR_FLIPPING && Math.abs(error) > Math.PI / 2) {
            targetRot = MathUtil.normalizeAngle(targetRot - Math.PI);
            wheelFlipped = true;
        } else {
            wheelFlipped = false;
        }

        error = MathUtil.normalizeAngle(targetRot - currentRot);

        double power = Range.clip(rotationController.calculate(0, error), -MAX_SERVO, MAX_SERVO);
        if (Double.isNaN(power)) power = 0;
        servo.setPower(power + (Math.abs(error) > SwerveConfig.STEER_DEADBAND_RAD ? SwerveConfig.STEER_K_STATIC : 0) * Math.signum(power));
    }

    public double getTargetRotation() {
        return MathUtil.normalizeAngle(target - Math.PI);
    }

    public double getModuleRotation() {
        return MathUtil.normalizeAngle(position - Math.PI);
    }

    public void setMotorPower(double power) {
        if (wheelFlipped) power *= -1;
        lastMotorPower = power;
        motor.setPower(power);
    }

    public void setTargetRotation(double target) {
        this.target = MathUtil.normalizeAngle(target);
    }

    public String getTelemetry(String name) {
        return String.format(Locale.ENGLISH, "%s: Motor Flipped: %b \ncurrent position %.2f target position %.2f flip modifer = %d motor power = %.2f", 
            name, wheelFlipped, getModuleRotation(), getTargetRotation(), flipModifier(), lastMotorPower);
    }

    public int flipModifier() {
        return wheelFlipped ? -1 : 1;
    }

    public void setMode(DcMotor.RunMode runMode) {
        motor.setMode(runMode);
    }

    public void setZeroPowerBehavior(DcMotor.ZeroPowerBehavior zeroPowerBehavior) {
        motor.setZeroPowerBehavior(zeroPowerBehavior);
    }

    public void setPIDFCoefficients(DcMotor.RunMode runMode, PIDFCoefficients coefficients) {
        motor.setPIDFCoefficients(runMode, coefficients);
    }

    public double getServoPower() {
        return servo.getPower();
    }

    public double getWheelPosition() {
        return encoderTicksToInches(motor.getCurrentPosition());
    }

    public double getWheelVelocity() {
        return encoderTicksToInches(motor.getVelocity());
    }

    public double encoderTicksToInches(double ticks) {
        return SwerveConfig.WHEEL_RADIUS_METERS * 39.37 * 2 * Math.PI * (1.0/SwerveConfig.DRIVE_GEAR_RATIO) * ticks / SwerveConfig.DRIVE_TICKS_PER_REV;
    }

    // Compatibility methods for AutoTuner and SingleModuleTuner
    public double getCurrentRotation() {
        return getModuleRotation();
    }

    public double getSteerErrorRadians() {
        return MathUtil.angleError(getModuleRotation(), getTargetRotation());
    }

    public double getVelocityMps() {
        return getWheelVelocity() * 0.0254;
    }

    public double getLastSteerPower() {
        return getServoPower();
    }

    public double getLastDrivePower() {
        return lastMotorPower;
    }

    public void update(double targetAngleRadians, double targetDrivePower, double dt) {
        setTargetRotation(targetAngleRadians);
        setMotorPower(targetDrivePower);
        update();
    }
}
