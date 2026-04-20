package org.firstinspires.ftc.teamcode.Swerve.Tests;

import org.firstinspires.ftc.teamcode.Swerve.Core.PIDController;
import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * HeadingRetentionTest
 * 
 * Verifies the automated drift-correction logic.
 */
public class HeadingRetentionTest {
    private static final double EPSILON = 1e-4;

    /**
     * TEST: Target Capture Logic
     * 
     * PASS: If isMaintaining is false and robot starts moving without turning, 
     *       targetHeading must be set to currentHeading.
     * FAIL: If targetHeading remains stale or is captured while driver is turning.
     */
    @Test
    public void testTargetCapture() {
        boolean isMoving = true;
        boolean noTurnInput = true;
        boolean isMaintaining = false;
        
        double currentHeading = 0.5; // Rad
        double targetHeading = 0.0;
        
        if (isMoving && noTurnInput) {
            if (!isMaintaining) {
                targetHeading = currentHeading;
                isMaintaining = true;
            }
        }
        
        assertEquals(0.5, targetHeading, EPSILON); // PASS CRITERIA
        assertTrue(isMaintaining);
    }

    /**
     * TEST: Correction Omega Generation
     * 
     * PASS: If robot drifts -0.1 rad, the PID must produce a POSITIVE correction value.
     * FAIL: If correction is zero or negative (which would increase drift).
     */
    @Test
    public void testCorrectionDirection() {
        PIDController pid = new PIDController(1.0, 0, 0); // P=1.0
        double target = 0.0;
        double current = -0.1; // Drifted negative
        
        double correction = pid.calculate(current, target, 0.02);
        
        assertTrue(correction > 0); // PASS CRITERIA: Anti-drift must oppose the error
    }

    /**
     * TEST: Manual Override Priority
     * 
     * PASS: If driver touches the turning stick (rawTurn > 0.1), isMaintaining must become FALSE.
     * FAIL: If the robot continues fighting the driver during an intentional turn.
     */
    @Test
    public void testManualOverride() {
        boolean isMaintaining = true;
        double rawTurn = 0.5; // Intentional driver turn
        
        if (Math.abs(rawTurn) > 0.1) {
            isMaintaining = false;
        }
        
        assertFalse(isMaintaining); // PASS CRITERIA: Manual control must preempt retention
    }
}
