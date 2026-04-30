package org.firstinspires.ftc.teamcode.pedroPathing.tuning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class PedroSecondaryDriveTuningTest {
    @Test
    void snapshotReflectsSecondaryDriveValues() {
        PedroTuneSnapshots.FilteredPidfSnapshot snapshot = PedroSecondaryDriveTuning.snapshot();
        assertFalse(snapshot.enabled);
        assertEquals(PedroSecondaryDriveTuning.F, snapshot.f, 1e-9);
    }
}
