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
        this(8);
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

        double sanitized = sanitize(measuredDtSec);
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

    private double sanitize(double measuredDtSec) {
        if (!Double.isFinite(measuredDtSec) || measuredDtSec <= 0.0) {
            return sampleSum / sampleCount;
        }
        return measuredDtSec;
    }
}
