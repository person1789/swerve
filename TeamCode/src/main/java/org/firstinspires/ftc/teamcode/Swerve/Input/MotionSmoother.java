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
    private double maxAccel = SwerveConfig.getMaxLinearAccelMPS2();
    private double maxJerk = SwerveConfig.getMaxLinearJerkMPS3();

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

        // 1. Shape normalized driver input, then convert into physical chassis targets.
        Vector physicalTarget = toPhysicalTarget(applyInputCurves(target));

        double[] nextVel = new double[3];
        double[] nextAccel = new double[3];

        for (int i = 0; i < 3; i++) {
            double targetVal = physicalTarget.get(i);
            double lastTargetVal = lastTarget.get(i);
            double currentVel = currentVelocity.get(i);
            double currentAccelVal = currentAcceleration.get(i);

            boolean isBraking = Math.signum(targetVal) != Math.signum(lastTargetVal)
                    || Math.abs(targetVal) < Math.abs(lastTargetVal) - 1e-4;

            if (isBraking) {
                nextAccel[i] = (targetVal - currentVel) / dt;
                nextVel[i] = targetVal;
            } else {
                double targetAccel = MathUtil.clamp((targetVal - currentVel) / dt, -maxAccel, maxAccel);
                double jerk = MathUtil.clamp((targetAccel - currentAccelVal) / dt, -maxJerk, maxJerk);
                double newAccel = MathUtil.clamp(currentAccelVal + jerk * dt, -maxAccel, maxAccel);

                nextAccel[i] = newAccel;
                double nextValue = currentVel + (newAccel * dt);
                if ((targetVal - currentVel) * (targetVal - nextValue) < 0) {
                    nextValue = targetVal;
                    nextAccel[i] = (nextValue - currentVel) / dt;
                }
                nextVel[i] = nextValue;
            }
        }

        currentVelocity = new Vector(nextVel);
        currentAcceleration = new Vector(nextAccel);
        lastTarget = physicalTarget;

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

    private Vector toPhysicalTarget(Vector normalizedTarget) {
        return new Vector(
                normalizedTarget.x() * SwerveConfig.getMaxLinearSpeedMPS(),
                normalizedTarget.y() * SwerveConfig.getMaxLinearSpeedMPS(),
                normalizedTarget.omega() * SwerveConfig.MAX_ANGULAR_VELOCITY_RAD_S);
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

    public void setLimits(double maxAccel, double maxJerk) {
        this.maxAccel = maxAccel;
        this.maxJerk = maxJerk;
    }

    public void reset() {
        currentVelocity = new Vector(0, 0, 0);
        currentAcceleration = new Vector(0, 0, 0);
        lastTarget = new Vector(0, 0, 0);
        maxAccel = SwerveConfig.getMaxLinearAccelMPS2();
        maxJerk = SwerveConfig.getMaxLinearJerkMPS3();
    }
}
