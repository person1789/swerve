package org.firstinspires.ftc.teamcode.pedroPathing.tuning;

import com.acmerobotics.dashboard.config.Config;

@Config
public final class PedroFollowerSafetyTuning {
    public static double DRIVE_KALMAN_MODEL_COVARIANCE = 1.0;
    public static double DRIVE_KALMAN_DATA_COVARIANCE = 1.0;
    public static double STUCK_VELOCITY = 1.0;
    public static double STUCK_T_VALUE = 0.995;
    public static double STUCK_TIMEOUT = 0.25;

    private PedroFollowerSafetyTuning() {
    }

    public static PedroTuneSnapshots.SafetySnapshot snapshot() {
        return new PedroTuneSnapshots.SafetySnapshot(
                DRIVE_KALMAN_MODEL_COVARIANCE,
                DRIVE_KALMAN_DATA_COVARIANCE,
                STUCK_VELOCITY,
                STUCK_T_VALUE,
                STUCK_TIMEOUT);
    }
}
