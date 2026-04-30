package org.firstinspires.ftc.teamcode.pedroPathing.tuning;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PedroFollowerSafetyTuningTest {
    @Test
    void snapshotReflectsSafetyValues() {
        PedroTuneSnapshots.SafetySnapshot snapshot = PedroFollowerSafetyTuning.snapshot();
        assertEquals(PedroFollowerSafetyTuning.DRIVE_KALMAN_MODEL_COVARIANCE, snapshot.driveKalmanModelCovariance, 1e-9);
        assertEquals(PedroFollowerSafetyTuning.STUCK_TIMEOUT, snapshot.stuckTimeout, 1e-9);
    }
}
