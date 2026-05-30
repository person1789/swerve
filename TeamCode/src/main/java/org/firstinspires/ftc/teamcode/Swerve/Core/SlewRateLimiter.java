package org.firstinspires.ftc.teamcode.Swerve.Core;

import com.qualcomm.robotcore.util.ElapsedTime;

/**
 * Limits the rate of change of a value. Adapted from KookyBotz/WPILib SlewRateLimiter.
 * Supports asymmetric rate limits for acceleration vs deceleration.
 */
public class SlewRateLimiter {
    private double positiveRateLimit;
    private double negativeRateLimit;
    private final ElapsedTime timer;
    private double prevValue;
    private double prevTimeSeconds;

    /**
     * @param positiveRateLimit Maximum rate of increase per second (e.g. 3.0 means 0→1 in 0.33s).
     * @param negativeRateLimit Maximum rate of decrease per second (should be negative, e.g. -3.0).
     * @param initialValue     Starting value for the limiter.
     */
    public SlewRateLimiter(double positiveRateLimit, double negativeRateLimit, double initialValue) {
        this.positiveRateLimit = positiveRateLimit;
        this.negativeRateLimit = negativeRateLimit;
        this.prevValue = initialValue;
        this.timer = new ElapsedTime();
        this.prevTimeSeconds = 0.0;
    }

    /**
     * Symmetric rate limit constructor.
     * @param rateLimit Maximum rate of change per second in either direction.
     */
    public SlewRateLimiter(double rateLimit) {
        this(rateLimit, -rateLimit, 0.0);
    }

    /**
     * Updates the rate limits dynamically.
     */
    public void setRateLimits(double positiveRateLimit, double negativeRateLimit) {
        this.positiveRateLimit = positiveRateLimit;
        this.negativeRateLimit = negativeRateLimit;
    }

    /**
     * Updates the symmetric rate limit dynamically.
     */
    public void setRateLimit(double rateLimit) {
        setRateLimits(rateLimit, -rateLimit);
    }

    /**
     * Filters the input to limit its rate of change.
     * @param input The desired value.
     * @return The rate-limited value.
     */
    public double calculate(double input) {
        double currentTime = timer.seconds();
        double elapsedTime = currentTime - prevTimeSeconds;
        if (elapsedTime <= 0.0) {
            elapsedTime = 0.02;
        }
        prevTimeSeconds = currentTime;

        double delta = input - prevValue;
        double maxChange;
        if (delta > 0.0) {
            maxChange = positiveRateLimit * elapsedTime;
        } else {
            maxChange = negativeRateLimit * elapsedTime;
        }

        double clampedDelta = Math.max(maxChange, Math.min(delta, -maxChange));
        if (delta > 0.0) {
            clampedDelta = Math.min(delta, positiveRateLimit * elapsedTime);
        } else {
            clampedDelta = Math.max(delta, negativeRateLimit * elapsedTime);
        }

        prevValue += clampedDelta;
        return prevValue;
    }

    /**
     * Resets the limiter to a specified value.
     */
    public void reset(double value) {
        prevValue = value;
        prevTimeSeconds = timer.seconds();
    }
}
