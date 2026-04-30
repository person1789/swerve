package org.firstinspires.ftc.teamcode.pedroPathing.tuning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class PedroSecondaryHeadingTuningTest {
    @Test
    void snapshotReflectsSecondaryHeadingValues() {
        PedroTuneSnapshots.PidfSnapshot snapshot = PedroSecondaryHeadingTuning.snapshot();
        assertFalse(snapshot.enabled);
        assertEquals(PedroSecondaryHeadingTuning.P, snapshot.p, 1e-9);
    }
}
