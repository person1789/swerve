package org.firstinspires.ftc.teamcode.Swerve.Core;

/**
 * Estimates loop period with a fixed first-sample approximation and a rolling
 * average thereafter.
 */
public class LoopTimeEstimator {

    private final double[] samples;
    private int sampleCount = 0;
    private int nextIndex = 0;
    private double sampleSum = 0.0;

    public LoopTimeEstimator() {
        this(SwerveConfig.LOOP_TIME_AVERAGE_WINDOW);
    }

    public LoopTimeEstimator(int windowSize) {
        if (windowSize <= 0) {
            throw new IllegalArgumentException("windowSize must be positive");
        }
        this.samples = new double[windowSize];
    }

    /**
     * Returns 20 ms for the first loop. After that, returns a rolling average of
     * measured loop times.
     */
    public double update(double measuredDtSec) {
        if (sampleCount == 0) {
            addSample(SwerveConfig.LOOP_TIME_SEC);
            return SwerveConfig.LOOP_TIME_SEC;
        }

        double currentAverage = sampleSum / sampleCount;
        double sanitized = sanitize(measuredDtSec, currentAverage);
        if (isOutlier(sanitized, currentAverage)) {
            return currentAverage;
        }

        addSample(sanitized);
        return sampleSum / sampleCount;
    }

    public void reset() {
        sampleCount = 0;
        nextIndex = 0;
        sampleSum = 0.0;
        for (int i = 0; i < samples.length; i++) {
            samples[i] = 0.0;
        }
    }

    private void addSample(double sample) {
        if (sampleCount < samples.length) {
            samples[sampleCount] = sample;
            sampleSum += sample;
            sampleCount++;
            nextIndex = sampleCount % samples.length;
            return;
        }

        sampleSum -= samples[nextIndex];
        samples[nextIndex] = sample;
        sampleSum += sample;
        nextIndex = (nextIndex + 1) % samples.length;
    }

    private double sanitize(double measuredDtSec, double fallbackAverage) {
        if (!Double.isFinite(measuredDtSec) || measuredDtSec <= 0.0) {
            return fallbackAverage;
        }
        return measuredDtSec;
    }

    private boolean isOutlier(double measuredDtSec, double currentAverage) {
        if (measuredDtSec > SwerveConfig.LOOP_TIME_OUTLIER_MAX_SEC) {
            return true;
        }
        return measuredDtSec > currentAverage * SwerveConfig.LOOP_TIME_OUTLIER_MULTIPLIER;
    }
}
