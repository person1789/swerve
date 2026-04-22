package org.firstinspires.ftc.teamcode.Swerve.Input;

import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Core.MathUtil;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Vector;

/**
 * MotionSmoother
 * 
 * The sole authority for velocity processing.
 * Handles non-linear joystick scaling, responsive braking, and S-curve smoothing.
 */
public class MotionSmoother {

    private Vector currentVelocity = new Vector(0, 0, 0);
    private Vector currentAcceleration = new Vector(0, 0, 0);
    private Vector lastTarget = new Vector(0, 0, 0);

    public MotionSmoother() {
    }

    /**
     * Updates the smoother for a chassis-level velocity target.
     * 
     * @param target The intended robot velocity (m/s and rad/s).
     * @param dt     loop period in seconds.
     * @return smoothed velocity Vector.
     */
    public Vector smooth(Vector target, double dt) {
        if (dt <= 0) return currentVelocity;

        // 1. Apply Non-Linear Input Curves (Sensitivity Tuning)
        Vector curvedTarget = applyInputCurves(target);

        double maxAccel = SwerveConfig.MAX_ACCEL;
        double maxJerk = SwerveConfig.MAX_JERK;

        double[] nextVel = new double[3];
        double[] nextAccel = new double[3];

        for (int i = 0; i < 3; i++) {
            double targetVal = curvedTarget.get(i);
            double lastTargetVal = lastTarget.get(i);
            
            boolean isBraking = Math.abs(targetVal) < Math.abs(lastTargetVal) - 1e-4;

            if (isBraking) {
                nextAccel[i] = 0;
                nextVel[i] = targetVal;
            } else {
                double desiredAccel = (targetVal - currentVelocity.get(i)) / dt;
                double accelError = desiredAccel - currentAcceleration.get(i);
                
                double limitedJerkAccelChange = MathUtil.clamp(accelError, -maxJerk * dt, maxJerk * dt);
                double newAccel = MathUtil.clamp(currentAcceleration.get(i) + limitedJerkAccelChange, -maxAccel, maxAccel);
                
                nextAccel[i] = newAccel;
                nextVel[i] = currentVelocity.get(i) + (newAccel * dt);
            }
        }

        currentVelocity = new Vector(nextVel);
        currentAcceleration = new Vector(nextAccel);
        lastTarget = curvedTarget;

        return currentVelocity;
    }

    /**
     * Applies non-linear power curves to the translation magnitude and rotation scalar.
     */
    private Vector applyInputCurves(Vector raw) {
        // Translation (Radial Scaling)
        double transMag = Math.hypot(raw.x(), raw.y());
        if (transMag > 1e-6) {
            double scaledMag = scaleValue(transMag);
            double ratio = scaledMag / transMag;
            
            // Rotation Scaling (Scalar)
            double scaledTurn = scaleValue(raw.omega());
            
            return new Vector(raw.x() * ratio, raw.y() * ratio, scaledTurn);
        } else {
            return new Vector(0, 0, scaleValue(raw.omega()));
        }
    }

    private double scaleValue(double input) {
        double intercept = SwerveConfig.INPUT_INTERCEPT;
        double splinePoint = SwerveConfig.INPUT_SPLINE_POINT;
        double slope = SwerveConfig.INPUT_SLOPE;

        return Math.signum(input) * intercept
                + (1 - intercept)
                * (Math.abs(input) > splinePoint
                    ? Math.pow(Math.abs(input), Math.log(splinePoint / slope) / Math.log(splinePoint)) * Math.signum(input)
                    : input / slope);
    }

    public void reset() {
        currentVelocity = new Vector(0, 0, 0);
        currentAcceleration = new Vector(0, 0, 0);
        lastTarget = new Vector(0, 0, 0);
    }
}
