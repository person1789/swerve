package org.firstinspires.ftc.teamcode.Swerve.Tests;

import org.firstinspires.ftc.teamcode.Swerve.Core.MathUtil;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Point;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class GeometryTest {
    private static final double EPSILON = 1e-6;

    /**
     * TEST: Angle Normalization
     * 
     * PASS: If angles wrap correctly within the [-PI, PI] range (e.g. 2PI becomes 0).
     * FAIL: If normalization results in values outside the range or introduces precision drift.
     */
    @Test
    public void testAngleNormalization() {
        assertEquals(0.0, MathUtil.normalizeAngle(2 * Math.PI), EPSILON); // PASS CRITERIA
        assertEquals(Math.PI, MathUtil.normalizeAngle(Math.PI), EPSILON); // PASS CRITERIA
        assertEquals(-Math.PI / 2, MathUtil.normalizeAngle(1.5 * Math.PI), EPSILON);
    }

    /**
     * TEST: Point Rotation (Trigonometric Invariance)
     * 
     * PASS: If rotating (1, 0) by 90 degrees correctly results in (0, 1).
     * FAIL: If rotational matrices are improperly calculated or magnitude is not preserved.
     */
    @Test
    public void testPointRotation() {
        Point p = new Point(1.0, 0.0);
        // Rotate 90 degrees (pi/2)
        Point rotated = p.rotate(Math.PI / 2);
        
        assertEquals(0.0, rotated.x, EPSILON); // PASS CRITERIA
        assertEquals(1.0, rotated.y, EPSILON); // PASS CRITERIA
    }

    /**
     * TEST: Pose Addition
     * 
     * PASS: If adding two poses results in the correct summation of X, Y, and Heading.
     * FAIL: If the addition logic fails to account for coordinate sign conventions.
     */
    @Test
    public void testPoseMath() {
        Pose start = new Pose(10, 20, Math.toRadians(45));
        Pose shift = new Pose(5, -5, Math.toRadians(15));
        
        Pose result = start.addPose(shift);
        assertEquals(15.0, result.x, EPSILON); // PASS CRITERIA
        assertEquals(15.0, result.y, EPSILON); // PASS CRITERIA
        assertEquals(Math.toRadians(60), result.heading, EPSILON);
    }

    /**
     * TEST: Shortest-Path Angle Error
     * 
     * PASS: If calculating the error between 170° and -170° results in +20° (the short way).
     * FAIL: If error calculation takes the long way around (>180°), causing jerky module rotation.
     */
    @Test
    public void testAngleErrorShortestPath() {
        // From 170 to -170 is +20 degrees short way.
        double current = Math.toRadians(170);
        double target = Math.toRadians(-170);
        double error = MathUtil.angleError(current, target);
        
        assertEquals(Math.toRadians(20), error, EPSILON); // PASS CRITERIA
    }
}
