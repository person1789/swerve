package org.firstinspires.ftc.teamcode.Swerve.Tests;

import org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics.SwerveAuditor;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics.SwerveKinematics;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics.SwerveModuleState;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SwerveKinematicsTest {
    private static final double EPSILON = 1e-4;

    /**
     * TEST: Forward Kinematics (IK)
     * 
     * PASS: If a unit forward chassis command (1.0 m/s) results in all four modules 
     *       pointing at 0.0 rad with 1.0 m/s speed.
     * FAIL: If individual module vectors are misaligned or scaled incorrectly.
     */
    @Test
    public void testForwardKinematics() {
        SwerveKinematics ik = new SwerveKinematics();
        // Move at 1.0 m/s forward
        SwerveModuleState[] states = ik.toModuleStates(1.0, 0.0, 0.0);
        
        for (SwerveModuleState state : states) {
            assertEquals(1.0, state.speedMetersPerSecond, EPSILON); // PASS CRITERIA
            assertEquals(0.0, state.angleRadians, EPSILON); // PASS CRITERIA
        }
    }

    /**
     * TEST: Strafe Kinematics (IK)
     * 
     * PASS: If a unit left chassis command (1.0 m/s) results in all four modules 
     *       pointing at 90 degrees (PI/2) with 1.0 m/s speed.
     * FAIL: If strafe vectors are improperly rotated or inverted.
     */
    @Test
    public void testStrafeKinematics() {
        SwerveKinematics ik = new SwerveKinematics();
        // Move at 1.0 m/s left
        SwerveModuleState[] states = ik.toModuleStates(0.0, 1.0, 0.0);
        
        for (SwerveModuleState state : states) {
            assertEquals(1.0, state.speedMetersPerSecond, EPSILON); // PASS CRITERIA
            assertEquals(Math.PI / 2.0, state.angleRadians, EPSILON); // PASS CRITERIA
        }
    }

    /**
     * TEST: Kinematic Inversion (Lossless Recovery)
     * 
     * PASS: If converting Chassis -> Modules (IK) and then Modules -> Chassis (FK) 
     *       recovers the original target vectors exactly.
     * FAIL: If the mathematical transformation is lossy, resulting in "phantom drift" in the observer.
     */
    @Test
    public void testForwardKinematicsInversion() {
        SwerveKinematics kinematics = new SwerveKinematics();
        double targetVx = 1.2;
        double targetVy = 0.5;
        double targetOmega = 2.0;

        // Set loop time to 0 to test pure matrix inversion without discretization offsets
        kinematics.setLoopTimeSec(0.0);

        // IK: Chassis -> Modules
        SwerveModuleState[] states = kinematics.toModuleStates(targetVx, targetVy, targetOmega);

        // FK: Modules -> Chassis
        Pose result = kinematics.toChassisSpeeds(states);

        assertEquals(targetVx, result.x, EPSILON); // PASS CRITERIA
        assertEquals(targetVy, result.y, EPSILON); // PASS CRITERIA
        assertEquals(targetOmega, result.heading, EPSILON); // PASS CRITERIA
    }
}
