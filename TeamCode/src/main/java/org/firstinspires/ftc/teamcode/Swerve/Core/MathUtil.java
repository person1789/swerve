/**
 * MathUtil: A collection of specialized math tools for swerve geometry.
 * This class provides shortcuts for handling "circular" math, such as 
 * standardizing angles and calculating the shortest way to rotate a wheel 
 * so it doesn't get tangled or spin unnecessarily.
 */
package org.firstinspires.ftc.teamcode.Swerve.Core;

/**
 * MathUtil
 *
 * Utility class providing angle normalization, clamping, and other math
 * helpers used across the swerve system.
 *
 * Convention note: all angles are in RADIANS unless a method name explicitly
 * states degrees (e.g. normalizeAngleDegrees).
 */
public final class MathUtil {

    /** No instances. */
    private MathUtil() {
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Angle normalization
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Normalize an angle in radians to the continuous range (-π, π].
     *
     * @param angleRad angle in radians (any value)
     * @return equivalent angle in (-π, π]
     */
    public static double normalizeAngle(double angleRad) {
        // More robust implementation without external dependencies
        double result = angleRad % (2.0 * Math.PI);
        if (result <= -Math.PI) result += 2.0 * Math.PI;
        if (result > Math.PI) result -= 2.0 * Math.PI;
        return result;
    }

    /**
     * Normalize an angle in degrees to the continuous range (-180, 180].
     *
     * @param angleDeg angle in degrees (any value)
     * @return equivalent angle in (-180, 180]
     */
    public static double normalizeAngleDegrees(double angleDeg) {
        double result = angleDeg % 360.0;
        if (result <= -180.0) result += 360.0;
        if (result > 180.0) result -= 360.0;
        return result;
    }

    /**
     * Compute the shortest signed angular difference from {@code current} to
     * {@code target}, both in radians. Result is in (-π, π].
     *
     * @param current current angle in radians
     * @param target  desired angle in radians
     * @return signed error in (-π, π]
     */
    public static double angleError(double current, double target) {
        return normalizeAngle(target - current);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Clamping / range helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Clamp {@code value} to [{@code min}, {@code max}].
     */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Clamp a motor/servo power value to the legal [-1, 1] range.
     */
    public static double clampPower(double power) {
        return clamp(power, -1.0, 1.0);
    }

    /**
     * Return the maximum absolute value among an array of doubles.
     * Returns 0 if the array is null or empty.
     */
    public static double maxAbs(double... values) {
        if (values == null || values.length == 0)
            return 0.0;
        double max = 0.0;
        for (double v : values) {
            double abs = Math.abs(v);
            if (abs > max)
                max = abs;
        }
        return max;
    }

    /**
     * Apply a symmetric deadband: if |value| < deadband return 0, otherwise
     * rescale the remaining range back to [-1, 1] so control feels smooth.
     *
     * @param value    raw input, nominally in [-1, 1]
     * @param deadband threshold below which value is treated as zero
     * @return processed value
     */
    public static double applyDeadband(double value, double deadband) {
        if (Math.abs(value) < deadband) return 0.0;
        return Math.signum(value) * (Math.abs(value) - deadband) / (1.0 - deadband);
    }
}