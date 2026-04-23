package org.firstinspires.ftc.teamcode.Swerve.Core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.junit.jupiter.api.Test;

class RobotSettingsTest {

    @Test
    void allianceGoalsUseDistinctFieldPositions() {
        // Passes if red and blue alliance goals no longer collapse to the same field coordinates.
        double redY = RobotSettings.Alliance.RED.getGoalPos().getY(DistanceUnit.INCH);
        double blueY = RobotSettings.Alliance.BLUE.getGoalPos().getY(DistanceUnit.INCH);

        assertNotEquals(redY, blueY, 1e-9);
    }

    @Test
    void mirroredStartPositionsStayInsideFieldBounds() {
        // Passes if every configured start pose remains within the 0 to 144 inch field frame after mirroring.
        for (RobotSettings.StartPos startPos : RobotSettings.StartPos.values()) {
            double x = startPos.getPose2D().getX(DistanceUnit.INCH);
            double y = startPos.getPose2D().getY(DistanceUnit.INCH);

            assertEquals(true, x >= 0.0 && x <= 144.0);
            assertEquals(true, y >= 0.0 && y <= 144.0);
        }
    }

    @Test
    void defaultSettingsAlwaysProvideNonNullSelections() {
        // Passes if the default settings object always boots with a valid alliance and start position selection.
        RobotSettings settings = new RobotSettings();

        assertNotNull(settings.alliance);
        assertNotNull(settings.startPosState);
    }
}
