package org.firstinspires.ftc.teamcode.pedroPathing.tuning;

import com.acmerobotics.dashboard.config.Config;

@Config
public final class PedroPathControlTuning {
    public static double CENTRIPETAL_SCALING = 0.0005;
    public static boolean AUTOMATIC_HOLD_END = false;
    public static double HOLD_POINT_TRANSLATIONAL_SCALING = 0.45;
    public static double HOLD_POINT_HEADING_SCALING = 0.35;
    public static double TURN_HEADING_ERROR_THRESHOLD_RAD = Math.toRadians(3.0);
    public static int BEZIER_CURVE_SEARCH_LIMIT = 10;

    private PedroPathControlTuning() {
    }

    public static PedroTuneSnapshots.PathControlSnapshot snapshot() {
        return new PedroTuneSnapshots.PathControlSnapshot(
                CENTRIPETAL_SCALING,
                AUTOMATIC_HOLD_END,
                HOLD_POINT_TRANSLATIONAL_SCALING,
                HOLD_POINT_HEADING_SCALING,
                TURN_HEADING_ERROR_THRESHOLD_RAD,
                BEZIER_CURVE_SEARCH_LIMIT);
    }
}
