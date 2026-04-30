package org.firstinspires.ftc.teamcode.pedroPathing.tuning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PedroPrimaryHeadingTuningTest {
    @Test
    void snapshotReflectsPrimaryHeadingValues() {
        PedroTuneSnapshots.PidfSnapshot snapshot = PedroPrimaryHeadingTuning.snapshot();
        assertTrue(snapshot.enabled);
        assertEquals(PedroPrimaryHeadingTuning.SWITCH_RAD, snapshot.switchThreshold, 1e-9);
    }
}
