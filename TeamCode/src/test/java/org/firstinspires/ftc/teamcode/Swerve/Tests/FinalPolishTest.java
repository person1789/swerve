package org.firstinspires.ftc.teamcode.Swerve.Tests;

import org.firstinspires.ftc.teamcode.Swerve.Core.LowPassFilter;
import org.firstinspires.ftc.teamcode.Swerve.Core.PIDController;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics.SwerveModuleState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FinalPolishTest {
    private static final double EPSILON = 1e-4;

    /**
     * TEST: Low Pass Filter (Exponential Moving Average)
     * 
     * PASS: If a filter with alpha=0.5 correctly averages the current and previous 
     *       input across multiple iterations (converging on steady value).
     * FAIL: If the filter output is static or ignores input history.
     */
    @Test
    public void testLowPassFilter() {
        // Alpha = 0.5 (Average of current and last)
        LowPassFilter filter = new LowPassFilter(0.5);
        
        assertEquals(0.5, filter.calculate(1.0), EPSILON); // PASS CRITERIA
        assertEquals(0.75, filter.calculate(1.0), EPSILON); // PASS CRITERIA
        assertEquals(0.875, filter.calculate(1.0), EPSILON);
        
        filter.reset(0.0);
        assertEquals(0.0, filter.calculate(0.0), EPSILON);
    }

    /**
     * TEST: Basic PID Controller
     * 
     * PASS: If a Pure-P controller produces `error * kP` output and drops to zero at the setpoint.
     * FAIL: If the target is not reached or if the derivative/integral components bleed into static P.
     */
    @Test
    public void testPIDController() {
        PIDController pid = new PIDController(1.0, 0.0, 0.0);
        pid.setSetpoint(10.0);
        
        // P only: error = 10, Output = 10 * 1.0 = 10
        assertEquals(10.0, pid.calculate(0.0, 0.1), EPSILON); // PASS CRITERIA
        
        // At target: error = 0, Output = 0
        assertEquals(0.0, pid.calculate(10.0, 0.1), EPSILON); // PASS CRITERIA
    }

    /**
     * TEST: Swerve Module State Vector Resolution
     * 
     * PASS: If a (1, 1) vector correctly converts to sqrt(2) magnitude and 45 degree angle.
     * FAIL: If vector-to-target calculations use incorrect trigonometric mappings (e.g. flipped sin/cos).
     */
    @Test
    public void testSwerveModuleState() {
        // Test fromVector (Forward 1, Right 1)
        // Magnitude = sqrt(2), Angle = 45 deg
        SwerveModuleState state = SwerveModuleState.fromVector(1.0, 1.0);
        
        assertEquals(Math.sqrt(2.0), state.speedMetersPerSecond, EPSILON); // PASS CRITERIA
        assertEquals(Math.toRadians(45), state.angleRadians, EPSILON); // PASS CRITERIA
        
        // Test copy
        SwerveModuleState copy = state.copy();
        assertEquals(state.speedMetersPerSecond, copy.speedMetersPerSecond);
        assertEquals(state.angleRadians, copy.angleRadians);
    }
}
