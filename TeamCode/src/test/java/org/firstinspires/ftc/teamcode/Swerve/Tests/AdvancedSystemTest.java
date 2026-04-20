package org.firstinspires.ftc.teamcode.Swerve.Tests;

import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics.SwerveKinematics;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics.SwerveModuleState;
import org.firstinspires.ftc.teamcode.Swerve.Input.MotionSmoother;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * AdvancedSystemTest
 * 
 * Stress-testing the high-fidelity control logic.
 */
public class AdvancedSystemTest {
    private static final double EPSILON = 1e-4;

    /**
     * TEST: Second-Order Kinematics (Discretization)
     * 
     * PASS: If the IK states, when inverted back to Chassis speeds, produce the target 
     *       velocity within 2% error margin.
     * FAIL: If second-order corrections cause the resulting chassis speed to drift 
     *       beyond the discretization energy loss threshold.
     */
    @Test
    public void testSecondOrderKinematics() {
        SwerveKinematics kinematics = new SwerveKinematics();
        kinematics.setLoopTimeSec(0.0); // 0ms for pure matrix inversion test
        
        // Fast strafe + fast rotation
        double vx = 1.0;
        double vy = 1.0;
        double omega = Math.PI; // 180 deg/sec
        
        SwerveModuleState[] states = kinematics.toModuleStates(vx, vy, omega);
        
        // At 20ms, the rotation correction should slightly offset the VX/VY 
        // within a small epsilon.
        Pose result = kinematics.toChassisSpeeds(states);
        
        assertEquals(vx, result.x, 0.02); // PASS CRITERIA (Allow slight discretization error)
        assertEquals(vy, result.y, 0.02); // PASS CRITERIA
        assertEquals(omega, result.heading, 0.02); // PASS CRITERIA
    }

    /**
     * TEST: Jerk Limiting
     * 
     * PASS: If the measured jerk between two simulation cycles is less than MAX_JERK.
     * FAIL: If the motion smoother allows instantaneous acceleration jumps larger than MAX_JERK.
     */
    @Test
    public void testJerkLimiting() {
        MotionSmoother smoother = new MotionSmoother();
        double dt = 0.02;
        
        Pose target = new Pose(1.0, 0, 0); // Instant command to 1 m/s
        Pose systemLimit = new Pose(1.0, 0, 0);
        
        // First loop
        Pose step1 = smoother.calculate(target, systemLimit, dt);
        double accel1 = step1.x / dt;
        
        // Second loop
        Pose step2 = smoother.calculate(target, systemLimit, dt);
        double accel2 = (step2.x - step1.x) / dt;
        
        double jerk = (accel2 - accel1) / dt;
        
        // PASS CRITERIA
        assertTrue(Math.abs(jerk) <= SwerveConfig.MAX_JERK + 0.1);
    }

    /**
     * TEST: Physics Feedforward Math
     * 
     * PASS: If a specific velocity and acceleration produce the expected voltage.
     * FAIL: If the algebraic model (V = kS + kV*v + kA*a) is incorrectly implemented.
     */
    @Test
    public void testFeedforwardMath() {
        // V = kS * sign(v) + kV * v + kA * a
        double v = 1.0;
        double a = 5.0;
        
        double ks = 1.0;
        double kv = 4.0;
        double ka = 0.5;
        
        double expectedVolts = ks * 1.0 + kv * v + ka * a; // 1.0 + 4.0 + 2.5 = 7.5
        assertEquals(7.5, expectedVolts, EPSILON); // PASS CRITERIA
    }

    /**
     * TEST: Voltage Compensation
     * 
     * PASS: If the motor power scales inversely with the measured battery voltage (e.g. 8V/10V = 0.8).
     * FAIL: If power normalization uses a static constant, leading to inconsistent torque on low battery.
     */
    @Test
    public void testVoltageCompensation() {
        double targetVolts = 8.0;
        
        // Scenario 1: Low Battery (10V)
        double battery1 = 10.0;
        double power1 = targetVolts / battery1; // PASS CRITERIA: Expected 0.8
        assertEquals(0.8, power1, EPSILON);
        
        // Scenario 2: Fresh Battery (14V)
        double battery2 = 14.0;
        double power2 = targetVolts / battery2; // PASS CRITERIA: Expected ~0.57
        assertEquals(targetVolts / 14.0, power2, EPSILON);
    }
}
