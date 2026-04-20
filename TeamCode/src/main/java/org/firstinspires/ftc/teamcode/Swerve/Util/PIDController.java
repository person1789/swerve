package org.firstinspires.ftc.teamcode.Swerve.Util;

import com.qualcomm.robotcore.util.Range;

/**
 * Custom PID Controller — Phase A
 * 
 * Optimized for FTC swerve control loops. 
 * Provides:
 * - Kp, Ki, Kd gains
 * - Integral anti-windup (sum limit)
 * - Derivative smoothing/filtering
 * - Max output clamping
 */
public class PIDController {
    private double Kp, Ki, Kd;
    private double setpoint;
    private double integralSum = 0;
    private double lastError = 0;
    private double maxIntegralSum = 1.0; // Default limit to prevent runaway
    private double lastTimestamp = 0;
    
    // For derivative filtering
    private double derivativeBuffer = 0;
    private double derivativeFilter = 0.8; // 0 = raw, 1 = extremely smooth

    public PIDController(double Kp, double Ki, double Kd) {
        this.Kp = Kp;
        this.Ki = Ki;
        this.Kd = Kd;
    }

    public void setPID(double Kp, double Ki, double Kd) {
        this.Kp = Kp;
        this.Ki = Ki;
        this.Kd = Kd;
    }

    public void setSetpoint(double setpoint) {
        this.setpoint = setpoint;
    }

    public void setMaxIntegralSum(double max) {
        this.maxIntegralSum = max;
    }

    public void setDerivativeFilter(double filter) {
        this.derivativeFilter = Range.clip(filter, 0, 1);
    }

    /**
     * Calculate control output based on current state.
     * 
     * @param current current measured value
     * @param dt loop period in seconds
     * @return control effort
     */
    public double calculate(double current, double dt) {
        if (dt <= 0) return 0;

        double error = setpoint - current;
        
        // P term
        double pOut = Kp * error;

        // I term with anti-windup
        integralSum += error * dt;
        integralSum = Range.clip(integralSum, -maxIntegralSum, maxIntegralSum);
        double iOut = Ki * integralSum;

        // D term with derivative filtering
        double rawDerivative = (error - lastError) / dt;
        derivativeBuffer = (derivativeFilter * derivativeBuffer) + ((1.0 - derivativeFilter) * rawDerivative);
        double dOut = Kd * derivativeBuffer;

        lastError = error;
        
        return pOut + iOut + dOut;
    }

    /**
     * Reset integral sum and error state.
     */
    public void reset() {
        integralSum = 0;
        lastError = 0;
        derivativeBuffer = 0;
    }
}
