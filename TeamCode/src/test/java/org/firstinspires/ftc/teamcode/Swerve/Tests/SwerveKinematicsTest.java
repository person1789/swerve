package org.firstinspires.ftc.teamcode.Swerve.Tests;

import org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics.SwerveAuditor;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics.SwerveKinematics;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics.SwerveModuleState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SwerveKinematicsTest {
    private static final double EPSILON = 1e-4;

    @Test
    public void testForwardKinematics() {
        SwerveKinematics ik = new SwerveKinematics();
        // Move at 1.0 m/s forward
        SwerveModuleState[] states = ik.toModuleStates(1.0, 0.0, 0.0);
        
        for (SwerveModuleState state : states) {
            assertEquals(1.0, state.speedMetersPerSecond, EPSILON);
            assertEquals(0.0, state.angleRadians, EPSILON);
        }
    }

    @Test
    public void testStrafeKinematics() {
        SwerveKinematics ik = new SwerveKinematics();
        // Move at 1.0 m/s left
        SwerveModuleState[] states = ik.toModuleStates(0.0, 1.0, 0.0);
        
        for (SwerveModuleState state : states) {
            assertEquals(1.0, state.speedMetersPerSecond, EPSILON);
            assertEquals(Math.PI / 2.0, state.angleRadians, EPSILON);
        }
    }

    @Test
    public void testAuditorOptimization() {
        // Current angle is 0. Target is 170 degrees.
        // Optimization should flip speed and target -10 degrees.
        SwerveModuleState desired = new SwerveModuleState(1.0, Math.toRadians(170));
        SwerveModuleState result = SwerveAuditor.optimizeSingle(desired, 0.0);
        
        assertEquals(-1.0, result.speedMetersPerSecond, EPSILON);
        assertEquals(Math.toRadians(-10), result.angleRadians, EPSILON);
    }
}
