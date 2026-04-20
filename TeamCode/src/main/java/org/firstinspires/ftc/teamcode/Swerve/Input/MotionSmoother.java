package org.firstinspires.ftc.teamcode.Swerve.Input;

import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Core.MathUtil;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose;

/**
 * MotionSmoother
 * 
 * Provides Jerk-limited S-curve smoothing for the robot's velocity commands.
 * Instead of instantly jumping to a new acceleration, this class ramps the 
 * acceleration up and down (limiting Jerk), which creates very smooth motion 
 * that prevents the robot from tipping or jerking.
 */
public class MotionSmoother {

    private Pose currentVelocity = new Pose(0, 0, 0);
    private Pose currentAcceleration = new Pose(0, 0, 0);
    private Pose lastDriverIntent = new Pose(0, 0, 0);

    private double maxAccel;
    private double maxJerk;

    public MotionSmoother() {
        this.maxAccel = SwerveConfig.MAX_ACCEL;
        this.maxJerk = SwerveConfig.MAX_JERK;
    }

    /**
     * Updates the smoother for a chassis-level pose.
     * 
     * @param driverTarget The raw intent from the human driver.
     * @param systemLimit  The final target after being scaled/limited by automated logic.
     * @param dt loop period in seconds
     * @return smoothed velocity
     */
    public Pose calculate(Pose driverTarget, Pose systemLimit, double dt) {
        if (dt <= 0) return currentVelocity;

        // X Axis
        currentVelocity.x = smoothAxis(currentVelocity.x, driverTarget.x, systemLimit.x, lastDriverIntent.x, 0, dt);
        // Y Axis
        currentVelocity.y = smoothAxis(currentVelocity.y, driverTarget.y, systemLimit.y, lastDriverIntent.y, 1, dt);
        // Theta Axis
        currentVelocity.heading = smoothAxis(currentVelocity.heading, driverTarget.heading, systemLimit.heading, lastDriverIntent.heading, 2, dt);

        lastDriverIntent = new Pose(driverTarget.x, driverTarget.y, driverTarget.heading);
        
        return new Pose(currentVelocity.x, currentVelocity.y, currentVelocity.heading);
    }

    /**
     * Internal intelligent axis smoother.
     * 
     * Logic:
     * - If Driver Intent decreased: Snap instantly to System Limit (Responsive Braking).
     * - Otherwise: Use S-Curve to reach System Limit (Smooth Acceleration/Automated Scaling).
     */
    private double smoothAxis(double currentV, double driverTarget, double systemLimit, double lastIntent, int axisIndex, double dt) {
        
        boolean driverDecreased = Math.abs(driverTarget) < Math.abs(lastIntent) - 1e-4;

        if (driverDecreased) {
            // Instant Response for Driver Braking
            // We reset acceleration to zero to prevent "kicking" when driver stops then starts again
            if (axisIndex == 0) currentAcceleration.x = 0;
            else if (axisIndex == 1) currentAcceleration.y = 0;
            else currentAcceleration.heading = 0;
            
            return systemLimit;
        }

        // Standard S-curve for Acceleration or Automated/Internal Slowdowns
        double desiredAccel = (systemLimit - currentV) / dt;

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
