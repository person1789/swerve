package org.firstinspires.ftc.teamcode.Swerve.Input;

import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Core.MathUtil;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose;

/**
 * MotionSmoother — Phase D
 * 
 * Provides Jerk-limited S-curve smoothing for the robot's velocity commands.
 * Instead of instantly jumping to a new acceleration, this class ramps the 
 * acceleration up and down (limiting Jerk), which creates very smooth motion 
 * that prevents the robot from tipping or jerking.
 */
public class MotionSmoother {

    private Pose currentVelocity = new Pose(0, 0, 0);
    private Pose currentAcceleration = new Pose(0, 0, 0);

    private double maxAccel;
    private double maxJerk;

    public MotionSmoother() {
        this.maxAccel = SwerveConfig.MAX_ACCEL;
        this.maxJerk = SwerveConfig.MAX_JERK;
    }

    /**
     * Updates the smoother and returns the next velocity command.
     * 
     * @param target velocity requested by the driver (m/s and rad/s)
     * @param dt loop period in seconds
     * @return smoothed velocity
     */
    public Pose calculate(Pose target, double dt) {
        if (dt <= 0) return currentVelocity;

        // X Axis
        currentVelocity.x = smoothAxis(currentVelocity.x, target.x, 0, dt);
        // Y Axis
        currentVelocity.y = smoothAxis(currentVelocity.y, target.y, 1, dt);
        // Theta Axis (Rotational smoothing is usually scaled differently)
        currentVelocity.heading = smoothAxis(currentVelocity.heading, target.heading, 2, dt);

        return new Pose(currentVelocity.x, currentVelocity.y, currentVelocity.heading);
    }

    /**
     * Internal jerk-limited axis smoother.
     * 1. Calculate desired acceleration to reach target velocity.
     * 2. Limit the change in acceleration (Jerk).
     * 3. Apply acceleration to velocity.
     */
    private double smoothAxis(double currentV, double targetV, int axisIndex, double dt) {
        // Step 1: Find acceleration needed to reach target in one step
        double desiredAccel = (targetV - currentV) / dt;

        // Step 2: Limit the rate of change of acceleration (Jerk)
        // Note: For simplicity in Phase D, we use currentVelocity's acceleration tracking
        double accelError;
        if (axisIndex == 0) accelError = desiredAccel - currentAcceleration.x;
        else if (axisIndex == 1) accelError = desiredAccel - currentAcceleration.y;
        else accelError = desiredAccel - currentAcceleration.heading;

        double limitedJerkAccelChange = MathUtil.clamp(accelError, -maxJerk * dt, maxJerk * dt);
        
        double newAccel;
        if (axisIndex == 0) {
            currentAcceleration.x += limitedJerkAccelChange;
            currentAcceleration.x = MathUtil.clamp(currentAcceleration.x, -maxAccel, maxAccel);
            newAccel = currentAcceleration.x;
        } else if (axisIndex == 1) {
            currentAcceleration.y += limitedJerkAccelChange;
            currentAcceleration.y = MathUtil.clamp(currentAcceleration.y, -maxAccel, maxAccel);
            newAccel = currentAcceleration.y;
        } else {
            currentAcceleration.heading += limitedJerkAccelChange;
            currentAcceleration.heading = MathUtil.clamp(currentAcceleration.heading, -maxAccel, maxAccel);
            newAccel = currentAcceleration.heading;
        }

        // Step 3: Apply smoothed acceleration to velocity
        return currentV + (newAccel * dt);
    }

    public void setLimits(double maxAccel, double maxJerk) {
        this.maxAccel = maxAccel;
        this.maxJerk = maxJerk;
    }

    public void reset() {
        currentVelocity = new Pose(0, 0, 0);
        currentAcceleration = new Pose(0, 0, 0);
    }
}
