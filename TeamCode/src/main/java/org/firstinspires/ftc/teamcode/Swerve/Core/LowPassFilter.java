package org.firstinspires.ftc.teamcode.Swerve.Core;

/**
 * LowPassFilter
 * 
 * Simple exponential moving average filter.
 * Formula: y = alpha * x + (1 - alpha) * last_y
 */
public class LowPassFilter {

    private double lastOutput = 0.0;
    private double alpha;

    /**
     * @param alpha Filter gain [0, 1]. 
     *              1.0 = No filtering (instant response)
     *              0.0 = Infinite filtering (signal never changes)
     */
    public LowPassFilter(double alpha) {
        this.alpha = alpha;
    }

    /**
     * Process a new input value.
     */
    public double calculate(double input) {
        double output = alpha * input + (1.0 - alpha) * lastOutput;
        lastOutput = output;
        return output;
    }

    /**
     * Reset the filter to a specific value.
     */
    public void reset(double value) {
        this.lastOutput = value;
    }

    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }
}
