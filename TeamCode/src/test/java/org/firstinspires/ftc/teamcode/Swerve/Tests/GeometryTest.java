package org.firstinspires.ftc.teamcode.Swerve.Tests;

import org.firstinspires.ftc.teamcode.Swerve.Core.MathUtil;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Point;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class GeometryTest {
    private static final double EPSILON = 1e-6;

    @Test
    public void testAngleNormalization() {
        assertEquals(0.0, MathUtil.normalizeAngle(2 * Math.PI), EPSILON);
        assertEquals(Math.PI, MathUtil.normalizeAngle(Math.PI), EPSILON);
        assertEquals(-Math.PI / 2, MathUtil.normalizeAngle(1.5 * Math.PI), EPSILON);
    }

    @Test
    public void testPointRotation() {
        Point p = new Point(1.0, 0.0);
        // Rotate 90 degrees (pi/2)
        Point rotated = p.rotate(Math.PI / 2);
        
        assertEquals(0.0, rotated.x, EPSILON);
        assertEquals(1.0, rotated.y, EPSILON);
    }

    @Test
    public void testPoseMath() {
        Pose start = new Pose(10, 20, Math.toRadians(45));
        Pose shift = new Pose(5, -5, Math.toRadians(15));
        
        Pose result = start.addPose(shift);
        assertEquals(15.0, result.x, EPSILON);
        assertEquals(15.0, result.y, EPSILON);
        assertEquals(Math.toRadians(60), result.heading, EPSILON);
    }

    @Test
    public void testAngleErrorShortestPath() {
        // From 170 to -170 is +20 degrees short way.
        double current = Math.toRadians(170);
        double target = Math.toRadians(-170);
        double error = MathUtil.angleError(current, target);
        
        assertEquals(Math.toRadians(20), error, EPSILON);
    }
}
