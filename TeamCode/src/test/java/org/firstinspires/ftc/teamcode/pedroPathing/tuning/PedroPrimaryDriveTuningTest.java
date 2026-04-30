package org.firstinspires.ftc.teamcode.pedroPathing.tuning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PedroPrimaryDriveTuningTest {
    @Test
    void snapshotReflectsPrimaryDriveValues() {
        PedroTuneSnapshots.FilteredPidfSnapshot snapshot = PedroPrimaryDriveTuning.snapshot();
        assertTrue(snapshot.enabled);
        assertEquals(PedroPrimaryDriveTuning.T, snapshot.t, 1e-9);
        assertEquals(PedroPrimaryDriveTuning.SWITCH, snapshot.switchThreshold, 1e-9);
    }
}
