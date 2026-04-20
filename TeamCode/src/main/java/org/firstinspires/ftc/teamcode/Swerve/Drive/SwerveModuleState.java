/**
 * SwerveModuleState: A simple data holder for a single wheel's instructions.
 * It stores two pieces of information: how fast the wheel should spin and 
 * which direction it should be pointing.
 */
package org.firstinspires.ftc.teamcode.Swerve.Drive;

import org.firstinspires.ftc.teamcode.Swerve.Geo.MathUtil;

/**
 * SwerveModuleState — Phase A
 *
 * Immutable (by convention) data-transfer object that describes the desired
 * state of one swerve module at a point in time.
 *
 * <pre>
 *   speedMetersPerSecond : [-maxSpeed, +maxSpeed]  (positive = forward)
 *   angleRadians         : (-π, π]                 (CCW positive, 0 = robot forward)
 * </pre>
 */
public class SwerveModuleState {

    /** Desired drive wheel speed in metres per second. */
    public double speedMetersPerSecond;

    /**
     * Desired steering angle in radians, normalized to (-π, π].
     * 0 rad = robot-forward direction.
     */
    public double angleRadians;

    /**
     * Create a zeroed state (module stopped, pointing forward).
     */
    public SwerveModuleState() {
        this.speedMetersPerSecond = 0.0;
        this.angleRadians = 0.0;
    }

    /**
     * Create a state with the given speed and angle.
     *
     * @param speedMetersPerSecond drive speed in m/s
     * @param angleRadians         steering angle in radians (will be normalized)
     */
    public SwerveModuleState(double speedMetersPerSecond, double angleRadians) {
        this.speedMetersPerSecond = speedMetersPerSecond;
        this.angleRadians = MathUtil.normalizeAngle(angleRadians);
    }

    /**
     * Return the steering angle in degrees (for telemetry / logging).
     */
    public double getAngleDegrees() {
        return Math.toDegrees(angleRadians);
    }

    /**
     * Construct a state from a Cartesian module velocity vector.
     *
     * @param vx module velocity component in the robot-X direction (m/s)
     * @param vy module velocity component in the robot-Y direction (m/s)
     * @return corresponding SwerveModuleState
     */
    public static SwerveModuleState fromVector(double vx, double vy) {
        double speed = Math.hypot(vx, vy);
        double angle = Math.atan2(vy, vx);
        return new SwerveModuleState(speed, angle);
    }

    /**
     * Return a deep copy of this state.
     */
    public SwerveModuleState copy() {
        return new SwerveModuleState(speedMetersPerSecond, angleRadians);
    }

    @Override
    public String toString() {
        return String.format(
                "SwerveModuleState{speed=%.3f m/s, angle=%.2f°}",
                speedMetersPerSecond,
                Math.toDegrees(angleRadians));
    }
}