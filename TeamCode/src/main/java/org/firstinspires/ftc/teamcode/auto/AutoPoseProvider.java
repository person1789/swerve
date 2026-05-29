package org.firstinspires.ftc.teamcode.auto;

public interface AutoPoseProvider {
    void update();

    double getXInches();

    double getYInches();

    double getHeadingRadians();

    boolean isReady();
}
