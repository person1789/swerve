package org.firstinspires.ftc.teamcode.Swerve.Tests;

import org.firstinspires.ftc.teamcode.Swerve.Core.PIDController;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the custom PIDController implementation.
 * Verifies P, I, and D terms independently and tests anti-windup logic.
 */
public class PIDControllerTest {

    private static final double EPSILON = 1e-4;

    /**
     * TEST: Proportional (P) Term
     * 
     * PASS: If a unit step error (10) produces exactly unit output (10) given Kp=1.0.
     * FAIL: If the proportional gain is scaled or ignored.
     */
    @Test
    public void testProportionalOnly() {
        PIDController pid = new PIDController(1.0, 0, 0); // Kp = 1
        pid.setSetpoint(10.0);
        
        // Error = 10, Kp = 1 -> Output = 10
        double output = pid.calculate(0.0, 0.1);
        assertEquals(10.0, output, EPSILON); // PASS CRITERIA
    }

    /**
     * TEST: Integral (I) Anti-Windup
     * 
     * PASS: If the integral sum correctly accumulates over time but CLAMPS to the 
     *       pre-defined Max Integral Sum.
     * FAIL: If the integral sum overflows the limit, causing catastrophic overshoot.
     */
    @Test
    public void testIntegralAntiWindup() {
        PIDController pid = new PIDController(0, 1.0, 0); // Ki = 1
        pid.setSetpoint(10.0);
        pid.setMaxIntegralSum(5.0);
        
        // Sum after 10 seconds of 10 error = 100
        // Should be clamped to 5.0
        for (int i = 0; i < 100; i++) {
            pid.calculate(0.0, 0.1);
        }
        
        double output = pid.calculate(0.0, 0.1);
        assertEquals(5.0, output, EPSILON); // PASS CRITERIA
    }

    /**
     * TEST: Derivative (D) Term
     * 
     * PASS: If a rapid change in error produces an output proportional to the rate of change.
     * FAIL: If the derivative calculation uses the wrong sign or incorrectly handles delta-time (dt).
     */
    @Test
    public void testDerivativeTerm() {
        PIDController pid = new PIDController(0, 0, 1.0); // Kd = 1
        pid.setSetpoint(10.0);
        pid.setDerivativeFilter(0.0); // No filter for exact math
        
        // First call sets lastError
        pid.calculate(0.0, 0.1);
        
        // Change current from 0 to 2 in 0.1s -> Velocity = 20, Error change = -20
        double output = pid.calculate(2.0, 0.1);
        assertEquals(-20.0, output, EPSILON); // PASS CRITERIA
    }

    /**
     * TEST: Global Controller Reset
     * 
     * PASS: If calling reset() clears the internal integral sum and error history.
     * FAIL: If "memory effects" persist after reset, causing unexpected jumps in motor power.
     */
    @Test
    public void testReset() {
        PIDController pid = new PIDController(1, 1, 1);
        pid.setSetpoint(10);
        pid.calculate(0, 0.1);
        
        pid.reset();
        
        // After reset, calculating with current=setpoint should be 0 exactly
        double output = pid.calculate(10.0, 0.1);
        assertEquals(0.0, output, EPSILON); // PASS CRITERIA
    }
}
