package org.firstinspires.ftc.teamcode.Swerve.Core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PoseStorageTest {

    @AfterEach
    void clearStorage() {
        PoseStorage.clear();
    }

    @Test
    void setFromPedroPoseCopiesPoseIntoExplicitSwerveStorage() {
        // Passes if Pedro pose handoff stores the same x, y, and heading values in the swerve-side pose container.
        com.pedropathing.geometry.Pose pedroPose = new com.pedropathing.geometry.Pose(12.5, 30.0, 1.2);

        PoseStorage.setFromPedroPose(pedroPose);
        Pose stored = PoseStorage.getCurrentPose();

        assertEquals(12.5, stored.x, 1e-9);
        assertEquals(30.0, stored.y, 1e-9);
        assertEquals(1.2, stored.heading, 1e-9);
    }

    @Test
    void clearRemovesStoredPose() {
        // Passes if clearing the storage removes any previously stored pose so teleop will not reuse stale autonomous state.
        PoseStorage.setCurrentPose(1.0, 2.0, 3.0);

        PoseStorage.clear();

        assertNull(PoseStorage.getCurrentPose());
    }
}
