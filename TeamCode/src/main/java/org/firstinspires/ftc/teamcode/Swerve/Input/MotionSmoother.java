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
/**
 * @deprecated The live teleop path no longer uses this helper. Prefer direct
 * opmode-to-drivetrain commands unless you are working on legacy tests or
 * archived behavior experiments.
 */
@Deprecated
public class MotionSmoother {
    private static final double EPSILON = 1e-6;

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
        TranslationResult translation = smoothTranslation(physicalTarget, dt);
        double[] omegaState = smoothScalar(
                physicalTarget.omega(),
                currentVelocity.omega(),
                currentAcceleration.omega(),
                maxAccel,
                maxJerk,
                dt);

        currentVelocity = new Vector(translation.velocity.x(), translation.velocity.y(), omegaState[0]);
        currentAcceleration = new Vector(translation.acceleration.x(), translation.acceleration.y(), omegaState[1]);
        lastTarget = physicalTarget;

        return currentVelocity;
    }

    private TranslationResult smoothTranslation(Vector physicalTarget, double dt) {
        Vector currentTranslation = new Vector(currentVelocity.x(), currentVelocity.y());
        Vector targetTranslation = new Vector(physicalTarget.x(), physicalTarget.y());
        Vector currentTranslationAcceleration = new Vector(currentAcceleration.x(), currentAcceleration.y());

        double currentSpeed = currentTranslation.magnitude();
        double targetSpeed = targetTranslation.magnitude();
        double releaseSpeed = SwerveConfig.getMaxLinearSpeedMPS()
                * SwerveConfig.TRANSLATION_REDIRECT_RELEASE_SPEED_FRACTION;

        Vector currentDirection = unitOrFallback(currentTranslation, lastTarget);
        Vector targetDirection = unitOrFallback(targetTranslation, currentTranslation);

        double directionDot = currentDirection.dot(targetDirection);
        boolean redirecting = currentSpeed > releaseSpeed
                && targetSpeed > EPSILON
                && directionDot < Math.cos(SwerveConfig.TRANSLATION_REDIRECT_ANGLE_RAD);

        Vector desiredDirection = redirecting ? currentDirection : targetDirection;
        double desiredSpeed = redirecting ? 0.0 : targetSpeed;
        double accelLimit = redirecting
                ? maxAccel * SwerveConfig.TRANSLATION_REDIRECT_DECEL_MULTIPLIER
                : maxAccel;
        double jerkLimit = redirecting
                ? maxJerk * SwerveConfig.TRANSLATION_REDIRECT_DECEL_MULTIPLIER
                : maxJerk;

        double currentAccelAlong = currentTranslationAcceleration.dot(desiredDirection);
        double[] speedState = smoothScalar(
                desiredSpeed,
                currentSpeed,
                currentAccelAlong,
                accelLimit,
                jerkLimit,
                dt);

        Vector nextVelocity = desiredDirection.scale(speedState[0]);
        Vector nextAcceleration = desiredDirection.scale(speedState[1]);
        return new TranslationResult(nextVelocity, nextAcceleration);
    }

    private double[] smoothScalar(double targetVal, double currentVal, double currentAccelVal,
            double accelLimit, double jerkLimit, double dt) {
        double targetAccel = (targetVal - currentVal) / dt;
        double jerk = MathUtil.clamp((targetAccel - currentAccelVal) / dt, -jerkLimit, jerkLimit);
        double newAccel = MathUtil.clamp(currentAccelVal + jerk * dt, -accelLimit, accelLimit);
        double nextValue = currentVal + (newAccel * dt);

        if ((targetVal - currentVal) * (targetVal - nextValue) < 0) {
            nextValue = targetVal;
            newAccel = 0.0;
        }

        return new double[] { nextValue, newAccel };
    }

    private Vector unitOrFallback(Vector preferred, Vector fallbackSource) {
        if (preferred.magnitude() > EPSILON) {
            return preferred.scale(1.0 / preferred.magnitude());
        }

        Vector fallback = new Vector(fallbackSource.x(), fallbackSource.y());
        if (fallback.magnitude() > EPSILON) {
            return fallback.scale(1.0 / fallback.magnitude());
        }

        return new Vector(1.0, 0.0);
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
    }

    private static class TranslationResult {
        final Vector velocity;
        final Vector acceleration;

        TranslationResult(Vector velocity, Vector acceleration) {
            this.velocity = velocity;
            this.acceleration = acceleration;
        }
    }
}
