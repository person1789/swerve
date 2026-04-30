package org.firstinspires.ftc.teamcode.pedroPathing.tuning;

import com.pedropathing.control.FilteredPIDFCoefficients;
import com.pedropathing.control.PIDFCoefficients;
import com.pedropathing.control.PredictiveBrakingCoefficients;

public final class PedroTuneSnapshots {
    private PedroTuneSnapshots() {
    }

    public static final class PidfSnapshot {
        public final boolean enabled;
        public final double p;
        public final double i;
        public final double d;
        public final double f;
        public final double switchThreshold;

        public PidfSnapshot(boolean enabled, double p, double i, double d, double f, double switchThreshold) {
            this.enabled = enabled;
            this.p = p;
            this.i = i;
            this.d = d;
            this.f = f;
            this.switchThreshold = switchThreshold;
        }

        public PIDFCoefficients toPidf() {
            return new PIDFCoefficients(p, i, d, f);
        }
    }

    public static final class FilteredPidfSnapshot {
        public final boolean enabled;
        public final double p;
        public final double i;
        public final double d;
        public final double f;
        public final double t;
        public final double switchThreshold;

        public FilteredPidfSnapshot(boolean enabled, double p, double i, double d, double f, double t, double switchThreshold) {
            this.enabled = enabled;
            this.p = p;
            this.i = i;
            this.d = d;
            this.f = f;
            this.t = t;
            this.switchThreshold = switchThreshold;
        }

        public FilteredPIDFCoefficients toPidf() {
            return new FilteredPIDFCoefficients(p, i, d, f, t);
        }
    }

    public static final class ModelSnapshot {
        public final double mass;
        public final double forwardZeroPowerAcceleration;
        public final double lateralZeroPowerAcceleration;

        public ModelSnapshot(double mass, double forwardZeroPowerAcceleration, double lateralZeroPowerAcceleration) {
            this.mass = mass;
            this.forwardZeroPowerAcceleration = forwardZeroPowerAcceleration;
            this.lateralZeroPowerAcceleration = lateralZeroPowerAcceleration;
        }
    }

    public static final class PathControlSnapshot {
        public final double centripetalScaling;
        public final boolean automaticHoldEnd;
        public final double holdPointTranslationalScaling;
        public final double holdPointHeadingScaling;
        public final double turnHeadingErrorThresholdRad;
        public final int bezierCurveSearchLimit;

        public PathControlSnapshot(double centripetalScaling, boolean automaticHoldEnd,
                                   double holdPointTranslationalScaling, double holdPointHeadingScaling,
                                   double turnHeadingErrorThresholdRad, int bezierCurveSearchLimit) {
            this.centripetalScaling = centripetalScaling;
            this.automaticHoldEnd = automaticHoldEnd;
            this.holdPointTranslationalScaling = holdPointTranslationalScaling;
            this.holdPointHeadingScaling = holdPointHeadingScaling;
            this.turnHeadingErrorThresholdRad = turnHeadingErrorThresholdRad;
            this.bezierCurveSearchLimit = bezierCurveSearchLimit;
        }
    }

    public static final class PredictiveBrakingSnapshot {
        public final boolean enabled;
        public final double linear;
        public final double quadraticFriction;
        public final double p;
        public final double maxPower;

        public PredictiveBrakingSnapshot(boolean enabled, double linear, double quadraticFriction, double p, double maxPower) {
            this.enabled = enabled;
            this.linear = linear;
            this.quadraticFriction = quadraticFriction;
            this.p = p;
            this.maxPower = maxPower;
        }

        public PredictiveBrakingCoefficients toCoefficients() {
            return new PredictiveBrakingCoefficients(linear, quadraticFriction, p)
                    .withMaximumBrakingPower(maxPower);
        }
    }

    public static final class SafetySnapshot {
        public final double driveKalmanModelCovariance;
        public final double driveKalmanDataCovariance;
        public final double stuckVelocity;
        public final double stuckTValue;
        public final double stuckTimeout;

        public SafetySnapshot(double driveKalmanModelCovariance, double driveKalmanDataCovariance,
                              double stuckVelocity, double stuckTValue, double stuckTimeout) {
            this.driveKalmanModelCovariance = driveKalmanModelCovariance;
            this.driveKalmanDataCovariance = driveKalmanDataCovariance;
            this.stuckVelocity = stuckVelocity;
            this.stuckTValue = stuckTValue;
            this.stuckTimeout = stuckTimeout;
        }
    }
}
