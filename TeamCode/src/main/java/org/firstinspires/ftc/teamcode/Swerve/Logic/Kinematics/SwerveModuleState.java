/**
 * SwerveModuleState: A simple data holder for a single wheel's instructions.
 * It stores two pieces of information: how fast the wheel should spin and 
 * which direction it should be pointing.
 */
package org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics;

import org.firstinspires.ftc.teamcode.Swerve.Core.MathUtil;

/**
 * SwerveModuleState
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

    /** Desired drive motor power [-1, 1]. */
    public double drivePower;

    /**
     * Desired steering angle in radians, normalized to (-π, π].
     * 0 rad = robot-forward direction.
     */
    public double angleRadians;

    /**
     * Create a zeroed state (module stopped, pointing forward).
     */
    public SwerveModuleState() {
        this.drivePower = 0.0;
        this.angleRadians = 0.0;
    }

    /**
     * Create a state with the given speed and angle.
     *
     * @param drivePower drive power [-1, 1]
     * @param angleRadians         steering angle in radians (will be normalized)
     */
    public SwerveModuleState(double drivePower, double angleRadians) {
        this.drivePower = drivePower;
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
        double power = Math.hypot(vx, vy);
        double angle = Math.atan2(vy, vx);
        return new SwerveModuleState(power, angle);
    }

    /**
     * Return a deep copy of this state.
     */
    public SwerveModuleState copy() {
        return new SwerveModuleState(drivePower, angleRadians);
    }

    @Override
    public String toString() {
        return String.format(
                "SwerveModuleState{power=%.3f, angle=%.2f°}",
                drivePower,
                Math.toDegrees(angleRadians));
    }
}
