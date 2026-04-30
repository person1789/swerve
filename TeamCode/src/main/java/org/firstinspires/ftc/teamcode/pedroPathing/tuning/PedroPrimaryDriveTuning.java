package org.firstinspires.ftc.teamcode.pedroPathing.tuning;

import com.acmerobotics.dashboard.config.Config;

@Config
public final class PedroPrimaryDriveTuning {
    public static boolean ENABLED = true;
    public static double P = 0.015;
    public static double I = 0.0;
    public static double D = 0.0005;
    public static double F = 0.0;
    public static double T = 0.0;
    public static double SWITCH = 2.0;

    private PedroPrimaryDriveTuning() {
    }

    public static PedroTuneSnapshots.FilteredPidfSnapshot snapshot() {
        return new PedroTuneSnapshots.FilteredPidfSnapshot(ENABLED, P, I, D, F, T, SWITCH);
    }
}
