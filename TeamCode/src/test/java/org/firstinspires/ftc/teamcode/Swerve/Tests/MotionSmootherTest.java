package org.firstinspires.ftc.teamcode.Swerve.Tests;

import org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose;
import org.firstinspires.ftc.teamcode.Swerve.Input.MotionSmoother;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MotionSmoother — Phase D
 * 
 * Verifies that the smoother correctly ramps velocity (Acceleration limit)
 * and ramps acceleration (Jerk limit) when speeding up, while allowing 
 * instant snappy response when slowing down.
 */
public class MotionSmootherTest {

    @Test
    public void testJerkLimiting() {
        MotionSmoother smoother = new MotionSmoother();
        smoother.setLimits(2.0, 5.0); // Accel=2.0, Jerk=5.0
        
        Pose target = new Pose(1.0, 0, 0); // Target 1.0 m/s
        double dt = 0.02; // 20ms loop
        
        Pose lastV = new Pose(0, 0, 0);
        
        // Over several loops, velocity should increase but NOT reach 1.0 immediately
        for (int i = 0; i < 5; i++) {
            Pose currentV = smoother.calculate(target, target, dt);
            
            // Check that velocity is increasing
            assertTrue(currentV.x >= lastV.x, "Velocity should be non-decreasing");
            
            // Current V should be much less than target due to limits
            assertTrue(currentV.x < 1.0, "Velocity should be limited by acceleration/jerk");
            
            lastV = currentV;
        }
    }

    @Test
    public void testIndependence() {
        MotionSmoother smoother = new MotionSmoother();
        smoother.setLimits(10.0, 100.0);
        
        // Demand X velocity, but zero Y and Theta
        Pose target = new Pose(1.0, 0, 0.0);
        Pose result = smoother.calculate(target, target, 0.1);
        
        assertTrue(result.x > 0);
        assertEquals(0.0, result.y, 1e-6);
        assertEquals(0.0, result.heading, 1e-6);
    }

    @Test
    public void testDeceleration() {
        MotionSmoother smoother = new MotionSmoother();
        smoother.setLimits(5.0, 50.0);
        
        // First get up to speed
        for(int i=0; i<50; i++) {
            smoother.calculate(new Pose(1.0, 0, 0), new Pose(1.0, 0, 0), 0.02);
        }
        
        // Now target zero (Driver pullback)
        Pose stopTarget = new Pose(0, 0, 0);
        Pose result = smoother.calculate(stopTarget, stopTarget, 0.02);
        
        // As per the "Intelligent Asymmetric Braking" requirement, 
        // driver-initiated slowdowns should be INSTANT.
        assertEquals(0.0, result.x, 1e-6);
    }
}
