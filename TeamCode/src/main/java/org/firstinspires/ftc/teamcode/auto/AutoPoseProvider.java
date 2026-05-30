package org.firstinspires.ftc.teamcode.auto;

public interface AutoPoseProvider {
    void update();

    double getXInches();

    double getYInches();

    double getHeadingRadians();

    void setPose(double xInches, double yInches, double headingRadians);

    boolean isReady();
}
