package org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Vector;
import org.junit.jupiter.api.Test;

class SwerveKinematicsTest {

    @Test
    void pureTranslationProducesMatchingModuleStatesAndRoundTrips() {
        // Passes if pure forward translation gives all modules the same state and reconstructs the chassis command.
        SwerveKinematics kinematics = new SwerveKinematics();

        SwerveModuleState[] states = kinematics.inverseKinematics(new Vector(1.0, 0.0, 0.0));
        Vector chassis = kinematics.forwardKinematics(states);

        for (SwerveModuleState state : states) {
            assertEquals(1.0, state.speedMetersPerSecond, 1e-9);
            assertEquals(0.0, state.angleRadians, 1e-9);
        }
        assertEquals(1.0, chassis.x(), 1e-9);
        assertEquals(0.0, chassis.y(), 1e-9);
        assertEquals(0.0, chassis.omega(), 1e-9);
    }

    @Test
    void compatibilityAliasesUseSameUnderlyingKinematics() {
        // Passes if alias APIs return the same module states and chassis speeds as the primary APIs.
        SwerveKinematics kinematics = new SwerveKinematics();

        SwerveModuleState[] states = kinematics.toModuleStates(0.0, 1.0, 0.0);
        Pose chassis = kinematics.toChassisSpeeds(states);

        assertEquals(Math.PI / 2.0, states[0].angleRadians, 1e-9);
        assertEquals(0.0, chassis.x, 1e-9);
        assertEquals(1.0, chassis.y, 1e-9);
        assertEquals(0.0, chassis.heading, 1e-9);
    }

    @Test
    void secondOrderKinematicsChangesTranslationDuringRotation() {
        // Passes if nonzero omega and loop time trigger the second-order discretization path.
        SwerveKinematics kinematics = new SwerveKinematics();
        kinematics.setLoopTimeSec(0.02);

        SwerveModuleState[] rotating = kinematics.toModuleStates(1.0, 0.0, 3.0);
        kinematics.setLoopTimeSec(0.0);
        SwerveModuleState[] firstOrder = kinematics.toModuleStates(1.0, 0.0, 3.0);

        assertNotEquals(firstOrder[0].angleRadians, rotating[0].angleRadians, 1e-6);
    }
}
