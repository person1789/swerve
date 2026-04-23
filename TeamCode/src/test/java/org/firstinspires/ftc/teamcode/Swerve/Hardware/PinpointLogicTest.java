package org.firstinspires.ftc.teamcode.Swerve.Hardware;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D;
import org.junit.jupiter.api.Test;

class PinpointLogicTest {

    @Test
    void goalDistanceUsesConsistentInchUnits() {
        // Passes if goal distance is computed in inches using the goal pose unit conversion instead of mixing meters with inches.
        Pose2D goal = new Pose2D(DistanceUnit.INCH, 10.0, 24.0, AngleUnit.DEGREES, 0.0);

        double distance = Pinpoint.distanceToGoalInches(10.0, 0.0, goal);

        assertEquals(24.0, distance, 1e-9);
    }

    @Test
    void headingErrorWrapsToShortestTurnInDegrees() {
        // Passes if goal-facing heading error wraps into the shortest signed degree error instead of returning a long-way-around turn.
        Pose2D goal = new Pose2D(DistanceUnit.INCH, 10.0, 0.0, AngleUnit.DEGREES, 0.0);

        double error = Pinpoint.headingErrorToGoalDegrees(0.0, 0.0, 350.0, goal);

        assertEquals(10.0, error, 1e-9);
    }
}
