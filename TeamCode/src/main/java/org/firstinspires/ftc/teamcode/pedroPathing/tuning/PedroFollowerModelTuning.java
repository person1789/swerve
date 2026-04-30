package org.firstinspires.ftc.teamcode.pedroPathing.tuning;

import com.acmerobotics.dashboard.config.Config;

@Config
public final class PedroFollowerModelTuning {
    public static double MASS = 10.65;
    public static double FORWARD_ZERO_POWER_ACCELERATION = -40.0;
    public static double LATERAL_ZERO_POWER_ACCELERATION = -40.0;

    private PedroFollowerModelTuning() {
    }

    public static PedroTuneSnapshots.ModelSnapshot snapshot() {
        return new PedroTuneSnapshots.ModelSnapshot(MASS, FORWARD_ZERO_POWER_ACCELERATION, LATERAL_ZERO_POWER_ACCELERATION);
    }
}
