package org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import org.junit.jupiter.api.Test;

class SwerveModuleStateTest {

    @Test
    void fromVectorBuildsSpeedAndAngleFromCartesianComponents() {
        // Passes if the speed is the vector magnitude and the angle matches atan2 for the same Cartesian components.
        SwerveModuleState state = SwerveModuleState.fromVector(3.0, 4.0);

        assertEquals(5.0, state.speedMetersPerSecond, 1e-9);
        assertEquals(Math.atan2(4.0, 3.0), state.angleRadians, 1e-9);
    }

    @Test
    void copyProducesIndependentEquivalentState() {
        // Passes if copy returns a different object with identical state values.
        SwerveModuleState original = new SwerveModuleState(1.5, Math.PI / 3.0);
        SwerveModuleState copy = original.copy();

        assertNotSame(original, copy);
        assertEquals(original.speedMetersPerSecond, copy.speedMetersPerSecond, 1e-9);
        assertEquals(original.angleRadians, copy.angleRadians, 1e-9);
    }
}
