package org.firstinspires.ftc.teamcode.Swerve.Core;

import com.qualcomm.robotcore.util.Range;

/**
 * Custom PID Controller
 * 
 * Optimized for FTC swerve control loops. 
 * Provides:
 * - Kp, Ki, Kd, Kf gains
 * - Integral anti-windup (sum limit) and I-Zone
 * - Derivative smoothing/filtering
 * - Max/Min output clamping
 * - Continuous input wrapping (e.g., for angles)
 * - Tolerances and atSetpoint checking
 */
public class PIDController {
    private double Kp, Ki, Kd, Kf;
    private double setpoint;
    private double integralSum = 0;
    private double lastError = 0;
    private double lastMeasurement = 0;
    
    // Limits
    private double maxIntegralSum = 1.0; 
    private double iZone = Double.POSITIVE_INFINITY;
    private double minOutput = Double.NEGATIVE_INFINITY;
    private double maxOutput = Double.POSITIVE_INFINITY;

    // Tolerances
    private double positionTolerance = 0.05;
    private double velocityTolerance = Double.POSITIVE_INFINITY;
    
    // Continuous Input
    private boolean continuous = false;
    private double minimumInput = 0;
    private double maximumInput = 0;

    // For derivative filtering
    private double derivativeBuffer = 0;
    private double derivativeFilter = 0.8; // 0 = raw, 1 = extremely smooth

    public PIDController(double Kp, double Ki, double Kd) {
        this(Kp, Ki, Kd, 0.0);
    }

    public PIDController(double Kp, double Ki, double Kd, double Kf) {
        this.Kp = Kp;
        this.Ki = Ki;
        this.Kd = Kd;
        this.Kf = Kf;
    }

    public void setPID(double Kp, double Ki, double Kd) {
        this.Kp = Kp;
        this.Ki = Ki;
        this.Kd = Kd;
    }

    public void setPIDF(double Kp, double Ki, double Kd, double Kf) {
        this.Kp = Kp;
        this.Ki = Ki;
        this.Kd = Kd;
        this.Kf = Kf;
    }

    public void setSetpoint(double setpoint) {
        this.setpoint = setpoint;
    }

    public double getSetpoint() {
        return this.setpoint;
    }

    public void setMaxIntegralSum(double max) {
        this.maxIntegralSum = max;
    }

    public void setIZone(double iZone) {
        this.iZone = Math.abs(iZone);
    }

    public void setOutputRange(double min, double max) {
        this.minOutput = min;
        this.maxOutput = max;
    }

    public void setDerivativeFilter(double filter) {
        this.derivativeFilter = Range.clip(filter, 0, 1);
    }

    public void enableContinuousInput(double minimumInput, double maximumInput) {
        this.continuous = true;
        this.minimumInput = minimumInput;
        this.maximumInput = maximumInput;
    }

    public void disableContinuousInput() {
        this.continuous = false;
    }

    public void setTolerance(double positionTolerance) {
        this.positionTolerance = Math.abs(positionTolerance);
    }

    public void setTolerance(double positionTolerance, double velocityTolerance) {
        this.positionTolerance = Math.abs(positionTolerance);
        this.velocityTolerance = Math.abs(velocityTolerance);
    }

    public boolean atSetpoint() {
        double positionError = Math.abs(getContinuousError(setpoint - lastMeasurement));
        double velocityError = Math.abs(derivativeBuffer); // Approximates velocity error if target is static
        return positionError <= positionTolerance && velocityError <= velocityTolerance;
    }

    private double getContinuousError(double error) {
        if (continuous) {
            double inputRange = maximumInput - minimumInput;
            while (Math.abs(error) > inputRange / 2.0) {
                if (error > 0) {
                    error -= inputRange;
                } else {
                    error += inputRange;
                }
            }
        }
        return error;
    }

    /**
     * Calculate control output based on current state and a new target.
     */
    public double calculate(double current, double target, double dt) {
        this.setpoint = target;
        return calculate(current, dt);
    }

    /**
     * Calculate control output based on current state.
     */
    public double calculate(double current, double dt) {
        if (dt <= 0) return 0;

        double error = getContinuousError(setpoint - current);
        
        // P term
        double pOut = Kp * error;

        // I term with anti-windup and I-Zone
        if (Math.abs(error) <= iZone) {
            integralSum += error * dt;
            integralSum = Range.clip(integralSum, -maxIntegralSum, maxIntegralSum);
        } else {
            integralSum = 0; // Reset if outside I-Zone
        }
        double iOut = Ki * integralSum;

        // D term with derivative filtering
        // Calculated on measurement rather than error to avoid derivative kick
        double rawDerivative;
        if (continuous) {
            rawDerivative = -getContinuousError(current - lastMeasurement) / dt;
        } else {
            rawDerivative = -(current - lastMeasurement) / dt;
        }
        
        derivativeBuffer = (derivativeFilter * derivativeBuffer) + ((1.0 - derivativeFilter) * rawDerivative);
        double dOut = Kd * derivativeBuffer;

        // F term
        double fOut = Kf * setpoint;

        lastError = error;
        lastMeasurement = current;
        
        return Range.clip(pOut + iOut + dOut + fOut, minOutput, maxOutput);
    }

    /**
     * Calculate control output based on pure error (useful if wrapping is done externally).
     * WARNING: This causes derivative kick if the setpoint changes instantly.
     */
    public double calculateFromError(double error, double dt) {
        if (dt <= 0) return 0;

        error = getContinuousError(error);

        double pOut = Kp * error;

        if (Math.abs(error) <= iZone) {
            integralSum += error * dt;
            integralSum = Range.clip(integralSum, -maxIntegralSum, maxIntegralSum);
        } else {
            integralSum = 0;
        }
        double iOut = Ki * integralSum;

        double rawDerivative = (error - lastError) / dt;
        derivativeBuffer = (derivativeFilter * derivativeBuffer) + ((1.0 - derivativeFilter) * rawDerivative);
        double dOut = Kd * derivativeBuffer;

        double fOut = Kf * setpoint;

        lastError = error;
        // Approximation since we only have error
        lastMeasurement = setpoint - error;

        return Range.clip(pOut + iOut + dOut + fOut, minOutput, maxOutput);
    }

    public void reset() {
        integralSum = 0;
        lastError = 0;
        lastMeasurement = 0;
        derivativeBuffer = 0;
    }
}
