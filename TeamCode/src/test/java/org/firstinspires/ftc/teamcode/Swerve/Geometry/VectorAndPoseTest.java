package org.firstinspires.ftc.teamcode.Swerve.Geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class VectorAndPoseTest {

    @Test
    void vectorConstructorDefensivelyCopiesInputArray() {
        // Passes if mutating the source array after construction does not change the vector's stored values.
        double[] source = {1.0, 2.0, 3.0};
        Vector vector = new Vector(source);
        source[0] = 99.0;

        assertEquals(1.0, vector.x(), 1e-9);
    }

    @Test
    void vectorMathOperationsProduceExpectedGeometry() {
        // Passes if add, subtract, scale, dot, magnitude, lerp, rotate, and cross match standard vector math results.
        Vector a = new Vector(1.0, 2.0);
        Vector b = new Vector(3.0, 4.0);

        assertEquals(4.0, a.add(b).x(), 1e-9);
        assertEquals(6.0, a.add(b).y(), 1e-9);
        assertEquals(-2.0, a.subtract(b).x(), 1e-9);
        assertEquals(1.5, a.scale(1.5).x(), 1e-9);
        assertEquals(11.0, a.dot(b), 1e-9);
        assertEquals(Math.sqrt(5.0), a.magnitude(), 1e-9);
        assertEquals(2.0, a.lerp(b, 0.5).x(), 1e-9);
        assertEquals(1.0, new Vector(1.0, 0.0).rotate(Math.PI / 2.0).y(), 1e-9);
        assertEquals(-2.0, new Vector(1.0, 2.0).cross(new Vector(3.0, 4.0)), 1e-9);
    }

    @Test
    void vectorRotateRejectsAmbiguousHigherDimensionalCall() {
        // Passes if calling the 2D-only rotate overload on a 3D vector fails fast instead of silently rotating the wrong axis.
        assertThrows(IllegalArgumentException.class, () -> new Vector(1.0, 2.0, 3.0).rotate(Math.PI / 4.0));
    }

    @Test
    void poseConversionAndAdditionPreserveWrappedHeading() {
        // Passes if pose conversion round-trips through Vector and addPose wraps heading back into the normalized range.
        Pose start = new Pose(2.0, -1.0, Math.PI - 0.1);
        Pose shift = new Pose(0.5, 1.5, 0.3);
        Pose result = start.addPose(shift);

        assertEquals(2.5, result.x, 1e-9);
        assertEquals(0.5, result.y, 1e-9);
        assertEquals(-Math.PI + 0.2, result.heading, 1e-9);
        assertEquals(start.heading, Pose.from(start.toVector()).heading, 1e-9);
    }
}
