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
    void clearRemovesStoredPose() {
        // Passes if clearing the storage removes any previously stored pose so teleop will not reuse stale autonomous state.
        PoseStorage.setCurrentPose(1.0, 2.0, 3.0);

        PoseStorage.clear();

        assertNull(PoseStorage.getCurrentPose());
    }
}
