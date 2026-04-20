package org.firstinspires.ftc.teamcode.Swerve.Tests;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PinpointTest
 * 
 * Basic verification of heading error math often used with Pinpoint odometry.
 */
public class PinpointTest {

    /**
     * TEST: Heading Error Calculation
     * 
     * PASS: If the error correctly wraps around the 180/-180 boundary.
     * FAIL: If the error produces results > 180 or < -180.
     */
    @Test
    public void testHeadingError() {
        // Robot Position (Meters)
        double robotX = 0;
        double robotY = 0;

        // Robot Heading (Degrees)
        double robotHeading = 0;

        // Goal Position (Meters)
        double goalX = -1.482;
        double goalY = 1.413;

        double targetAngle = Math.toDegrees(Math.atan2((goalY - robotY), (goalX - robotX)));

        double error = targetAngle - robotHeading;

        if (error <= -180) {
            error += 360;
        } else if (error >= 180) {
            error -= 360;
        }

        System.out.println("Robot at: (" + robotX + ", " + robotY + ")");
        System.out.println("Goal at: (" + goalX + ", " + goalY + ")");
        System.out.println("Robot heading: " + robotHeading);
        System.out.println("Calculated Error: " + error);

        assertEquals(136.4, error, 0.5);
    }
}
