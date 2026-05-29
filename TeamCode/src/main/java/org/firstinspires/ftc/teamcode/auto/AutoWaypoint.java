package org.firstinspires.ftc.teamcode.auto;

public class AutoWaypoint {
    public final double xInches;
    public final double yInches;
    public final double headingRadians;
    public final double settleDelayMs;
    public final double timeoutMs;

    public AutoWaypoint(double xInches, double yInches, double headingRadians,
            double settleDelayMs, double timeoutMs) {
        this.xInches = xInches;
        this.yInches = yInches;
        this.headingRadians = headingRadians;
        this.settleDelayMs = settleDelayMs;
        this.timeoutMs = timeoutMs;
    }
}
