package org.firstinspires.ftc.teamcode.Swerve.Logic.Control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SwerveControllerTest {

    private final double originalDelay = SwerveConfig.HEADING_LOCK_DELAY_S;

    @AfterEach
    void restoreConfig() {
        SwerveConfig.HEADING_LOCK_DELAY_S = originalDelay;
    }

    @Test
    void manualTurnPassesThroughAndDisablesHeadingHold() {
        // Passes if manual turn input is returned directly and no heading-maintain mode remains active.
        SwerveController controller = new SwerveController();

        Vector result = controller.update(0.4, 0.0, 0.3, 0.0, 0.02);

        assertEquals(0.3, result.omega(), 1e-9);
        assertFalse(controller.isMaintaining());
    }

    @Test
    void headingLockWaitsForConfiguredDelayBeforeActivating() {
        // Passes if heading hold remains off before the delay elapses and turns on once continuous translation exceeds the delay.
        SwerveConfig.HEADING_LOCK_DELAY_S = 0.1;
        SwerveController controller = new SwerveController();

        controller.update(0.4, 0.0, 0.0, 0.2, 0.05);
        assertFalse(controller.isMaintaining());

        controller.update(0.4, 0.0, 0.0, 0.2, 0.06);
        assertTrue(controller.isMaintaining());
    }

    @Test
    void snapModeCommandsRotationTowardTargetHeading() {
        // Passes if a positive heading error relative to the snap target produces corrective rotation in the negative direction.
        SwerveController controller = new SwerveController();
        controller.setSnapTarget(0.0);

        Vector result = controller.update(0.0, 0.0, 0.0, 0.3, 0.02);

        assertTrue(result.omega() < 0.0);
        assertTrue(controller.isSnapping());
    }

    @Test
    void resetHeadingCancelsSnapAndMaintainModes() {
        // Passes if resetHeading clears both mode flags and stores the supplied heading as the new target.
        SwerveController controller = new SwerveController();
        controller.setSnapTarget(Math.PI / 2.0);
        controller.resetHeading(-0.4);

        assertFalse(controller.isSnapping());
        assertFalse(controller.isMaintaining());
        assertEquals(-0.4, controller.getTargetHeading(), 1e-9);
    }
}
