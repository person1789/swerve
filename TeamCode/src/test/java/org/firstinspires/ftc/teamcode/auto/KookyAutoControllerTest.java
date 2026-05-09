package org.firstinspires.ftc.teamcode.auto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class KookyAutoControllerTest {

    private final double originalXP = KookyAutoController.X_P;
    private final double originalYP = KookyAutoController.Y_P;
    private final double originalHP = KookyAutoController.H_P;
    private final double originalMaxTranslation = KookyAutoController.MAX_TRANSLATION;
    private final double originalMaxRotation = KookyAutoController.MAX_ROTATION;

    @AfterEach
    void restoreConfig() {
        KookyAutoController.X_P = originalXP;
        KookyAutoController.Y_P = originalYP;
        KookyAutoController.H_P = originalHP;
        KookyAutoController.MAX_TRANSLATION = originalMaxTranslation;
        KookyAutoController.MAX_ROTATION = originalMaxRotation;
    }

    @Test
    void diagonalTranslationRespectsMagnitudeLimit() {
        // Passes if a large diagonal pose error keeps the translational command capped at the configured max without distorting the intended direction.
        KookyAutoController.X_P = 1.0;
        KookyAutoController.Y_P = 1.0;
        KookyAutoController.MAX_TRANSLATION = 1.0;
        KookyAutoController controller = new KookyAutoController();

        Vector command = controller.update(new Pose(0.0, 0.0, 0.0), new Pose(10.0, 10.0, 0.0));

        assertEquals(1.0, Math.hypot(command.x(), command.y()), 1e-9);
        assertTrue(command.x() > 0.0);
        assertTrue(command.y() > 0.0);
        assertEquals(command.x(), command.y(), 1e-9);
    }
}
