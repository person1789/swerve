package org.firstinspires.ftc.teamcode.Swerve.Core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.firstinspires.ftc.teamcode.Swerve.Geometry.Vector;
import org.junit.jupiter.api.Test;

class MathUtilTest {

    @Test
    void normalizeAngleWrapsAcrossPiBoundary() {
        // Passes if the normalized angle stays in (-pi, pi] and preserves direction across the wrap boundary.
        assertEquals(-Math.PI / 2.0, MathUtil.normalizeAngle(3.0 * Math.PI / 2.0), 1e-9);
        assertEquals(Math.PI / 2.0, MathUtil.normalizeAngle(-3.0 * Math.PI / 2.0), 1e-9);
    }

    @Test
    void angleErrorReturnsShortestSignedRotation() {
        // Passes if the computed error chooses the shortest signed turn between current and target angles.
        assertEquals(0.2, MathUtil.angleError(Math.PI - 0.1, -Math.PI + 0.1), 1e-9);
        assertEquals(-0.2, MathUtil.angleError(-Math.PI + 0.1, Math.PI - 0.1), 1e-9);
    }

    @Test
    void clampAndDeadbandRespectConfiguredRanges() {
        // Passes if scalar helpers clamp values correctly and zero-out values inside the deadband window.
        assertEquals(1.0, MathUtil.clamp(5.0, -1.0, 1.0), 1e-9);
        assertEquals(0.0, MathUtil.applyDeadband(0.03, 0.05), 1e-9);
        assertEquals(0.1578947368, MathUtil.applyDeadband(0.2, 0.05), 1e-9);
    }

    @Test
    void vectorDeadbandZeroesEachComponentIndependently() {
        // Passes if each vector component is zeroed or preserved independently based on the deadband threshold.
        Vector result = MathUtil.applyDeadband(new Vector(0.02, -0.06, 0.30), 0.05);

        assertEquals(0.0, result.x(), 1e-9);
        assertEquals(-0.06, result.y(), 1e-9);
        assertEquals(0.30, result.omega(), 1e-9);
    }
}
