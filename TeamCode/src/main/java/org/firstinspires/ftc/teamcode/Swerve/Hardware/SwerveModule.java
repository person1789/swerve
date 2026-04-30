package org.firstinspires.ftc.teamcode.Swerve.Hardware;

import com.arcrobotics.ftclib.controller.PIDFController;
import com.qualcomm.robotcore.hardware.AnalogInput;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.configuration.typecontainers.MotorConfigurationType;
import com.qualcomm.robotcore.util.Range;
import static org.firstinspires.ftc.robotcore.external.navigation.AngleUnit.normalizeRadians;




public class SwerveModule {

    public static double STEER_P = 0.0;
    public static double STEER_I = 0.0;
    public static double STEER_D = 0.0;
    public static double K_STATIC = 0.0;

    public static double MAX_STEER_POWER = 1.0;
    public static double MAX_DRIVE_POWER = 1.0;

    private DcMotorEx driveMotor;
    private CRServo steerServo;
    private AnalogInput encoder;
    private PIDFController rotationController;

    private double offset;
    private boolean inverse;

    private double targetAngle = 0.0;
    private double currentAngle = 0.0;

    public double lastDrivePower = 0.0;
    public double lastSteeringPower = 0.0;



    public SwerveModule(DcMotorEx driveMotor, CRServo steerServo, AnalogInput encoder ) {
        this.driveMotor = driveMotor;
        MotorConfigurationType motorConfigurationType = this.driveMotor.getMotorType().clone();
        motorConfigurationType.setAchieveableMaxRPMFraction(MAX_DRIVE_POWER);
        this.driveMotor.setMotorType(motorConfigurationType);
        this.driveMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        this.steerServo = steerServo;

        this.encoder = encoder;
        rotationController = new PIDFController(STEER_P, STEER_I, STEER_D, 0.0);

        this.driveMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        this.driveMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);


    }

    public SwerveModule(HardwareMap hardwareMap, String driveMotorName, String steerServoName, String encoderName, double offset, boolean inverse) {
        this(
                hardwareMap.get(DcMotorEx.class, driveMotorName),
                hardwareMap.get(CRServo.class, steerServoName),
                hardwareMap.get(AnalogInput.class, encoderName)


        );
        this.inverse = inverse;
        this.offset = offset;
    }


    public void update() {
        rotationController.setPIDF(STEER_P, STEER_I, STEER_D, 0.0);

        double targetAngle = getTargetAngle();
        double currentAngle = getCurrentRotation();

        double error = normalizeRadians(targetAngle - currentAngle);

        double steeringPower = Range.clip(rotationController.calculate(0, error), -MAX_STEER_POWER, MAX_STEER_POWER);

        if (Double.isNaN(steeringPower)) {
            steeringPower = 0.0;
        }

        steeringPower += (Math.abs(error) > 0.02 ? K_STATIC : 0.0) * Math.signum(steeringPower);
        lastSteeringPower = steeringPower;
        steerServo.setPower(steeringPower);

    }

    public double getTargetAngle() {
        return normalizeRadians(targetAngle);
    }

    public double getCurrentRotation() {
        double voltage = encoder.getVoltage();
        double angle = (voltage / 3.3) * (2.0 * Math.PI);
        double rotation = normalizeRadians(angle - offset);
        return inverse ? -rotation : rotation;
    }

    public void setDrivePower(double power) {
        lastDrivePower = power;
        driveMotor.setPower(power);
    }

    public void setTargetAngle(double targetAngle) {
        this.targetAngle = normalizeRadians(targetAngle);
    }

    public void setOffset(double offset) {
        this.offset = offset;
    }

    public void setInversion(boolean inverse) {
        this.inverse = inverse;
    }

    public void setMode(DcMotor.RunMode runMode) {
        driveMotor.setMode(runMode);
    }

    public void setZeroPowerBehavior(DcMotor.ZeroPowerBehavior zeroPowerBehavior) {
        driveMotor.setZeroPowerBehavior(zeroPowerBehavior);
    }
}