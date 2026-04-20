package org.firstinspires.ftc.teamcode.Swerve.Tests;

import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics.SwerveKinematics;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics.SwerveModuleState;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Localization.SwerveVelocityObserver;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SwerveVelocityObserverTest {
    private static final double EPSILON = 1e-4;

    /**
     * TEST: Stationary Observer
     * 
     * PASS: If stationary modules result in zero chassis velocity.
     * FAIL: If sensor noise or rounding errors accumulate into "phantom velocity" while the robot is still.
     */
    @Test
    public void testStationary() {
        SwerveKinematics kinematics = new SwerveKinematics();
        SwerveVelocityObserver observer = new SwerveVelocityObserver(kinematics);
        
        SwerveModuleState[] states = new SwerveModuleState[] {
            new SwerveModuleState(0, 0),
            new SwerveModuleState(0, 0),
            new SwerveModuleState(0, 0),
            new SwerveModuleState(0, 0)
        };

        observer.update(states);
        Pose vel = observer.getVelocity();

        assertEquals(0.0, vel.x, EPSILON); // PASS CRITERIA
        assertEquals(0.0, vel.y, EPSILON); // PASS CRITERIA
        assertEquals(0.0, vel.heading, EPSILON); // PASS CRITERIA
    }

    /**
     * TEST: Linear Velocity Matching
     * 
     * PASS: If constant module velocities (1.0 m/s) converge to a 1.0 m/s chassis velocity within 50 iterations.
     * FAIL: If the observer output is decoupled from actual module movement.
     */
    @Test
    public void testLinearMovement() {
        SwerveKinematics kinematics = new SwerveKinematics();
        SwerveVelocityObserver observer = new SwerveVelocityObserver(kinematics);
        
        // All modules moving forward at 1.0 m/s
        SwerveModuleState[] states = new SwerveModuleState[] {
            new SwerveModuleState(1.0, 0),
            new SwerveModuleState(1.0, 0),
            new SwerveModuleState(1.0, 0),
            new SwerveModuleState(1.0, 0)
        };

        // Update several times to let LPF converge
        for (int i = 0; i < 50; i++) {
            observer.update(states);
        }
        
        Pose vel = observer.getVelocity();
        // Should be close to 1.0
        assertTrue(Math.abs(1.0 - vel.x) < 0.05); // PASS CRITERIA
        assertEquals(0.0, vel.y, EPSILON);
        assertEquals(0.0, vel.heading, EPSILON);
    }

    /**
     * TEST: Rotational Velocity Matching
     * 
     * PASS: If module vectors in a tangential circle result in the correct angular velocity.
     * FAIL: If rotation calculation fails to account for track-width/wheel-base geometry.
     */
    @Test
    public void testRotation() {
        SwerveKinematics kinematics = new SwerveKinematics();
        SwerveVelocityObserver observer = new SwerveVelocityObserver(kinematics);
        
        // Target 1.0 rad/s pure rotation
        SwerveModuleState[] states = kinematics.toModuleStates(0, 0, 1.0);

        // Update to converge LPF
        for (int i = 0; i < 50; i++) {
            observer.update(states);
        }
        
        Pose vel = observer.getVelocity();
        assertEquals(0.0, vel.x, EPSILON);
        assertEquals(0.0, vel.y, EPSILON);
        assertTrue(Math.abs(1.0 - vel.heading) < 0.05); // PASS CRITERIA
    }

    /**
     * TEST: LPF Convergence Curve
     * 
     * PASS: If the observer output follows the exponential moving average curve (1st update = Gain * Error).
     * FAIL: If the filter allows an instantaneous jump to the target, bypassing noise rejection.
     */
    @Test
    public void testLPFConvergence() {
        SwerveKinematics kinematics = new SwerveKinematics();
        SwerveVelocityObserver observer = new SwerveVelocityObserver(kinematics);
        
        // Initial state is 0. 
        // Sudden jump to 1.0 m/s
        SwerveModuleState[] states = new SwerveModuleState[] {
            new SwerveModuleState(1.0, 0),
            new SwerveModuleState(1.0, 0),
            new SwerveModuleState(1.0, 0),
            new SwerveModuleState(1.0, 0)
        };

        observer.update(states);
        double firstUpdate = observer.getVelocity().x;
        
        // With alpha = 0.15, first update should be exactly 0.15
        assertEquals(SwerveConfig.OBSERVER_LPF_GAIN, firstUpdate, EPSILON); // PASS CRITERIA
        
        observer.update(states);
        double secondUpdate = observer.getVelocity().x;
        // 0.15 + (1.0 - 0.15) * 0.15 = 0.2775
        assertEquals(0.2775, secondUpdate, EPSILON); // PASS CRITERIA
    }
}
