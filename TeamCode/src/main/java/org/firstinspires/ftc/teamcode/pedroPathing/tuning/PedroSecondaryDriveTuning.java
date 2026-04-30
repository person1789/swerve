package org.firstinspires.ftc.teamcode.pedroPathing.tuning;

import com.acmerobotics.dashboard.config.Config;

@Config
public final class PedroSecondaryDriveTuning {
    public static boolean ENABLED = false;
    public static double P = 0.008;
    public static double I = 0.0;
    public static double D = 0.0003;
    public static double F = 0.0;
    public static double T = 0.0;

    private PedroSecondaryDriveTuning() {
    }

    public static PedroTuneSnapshots.FilteredPidfSnapshot snapshot() {
        return new PedroTuneSnapshots.FilteredPidfSnapshot(ENABLED, P, I, D, F, T, 0.0);
    }
}
