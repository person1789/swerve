package org.firstinspires.ftc.teamcode.auto;

import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.teamcode.Swerve.Core.MathUtil;
import org.firstinspires.ftc.teamcode.Swerve.Core.PIDController;
import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;

final class AutoMath {
    private AutoMath() {
    }

    static void calculateKookyPowers(
            AutoPoseProvider pose,
            double targetX,
            double targetY,
            double targetHeading,
            PIDController xController,
            PIDController yController,
            PIDController headingController,
            double dt,
            double[] output) {
        double deltaX = targetX - pose.getXInches();
        double deltaY = targetY - pose.getYInches();
        double headingError = MathUtil.angleError(pose.getHeadingRadians(), targetHeading);

        double xPower = xController.calculateFromError(deltaX, dt);
        double yPower = yController.calculateFromError(deltaY, dt);
        double headingPower = headingController.calculateFromError(headingError, dt);
        rotateAndClamp(pose.getHeadingRadians(), xPower, yPower, headingPower, output);
    }

    static void calculateInitialPowers(
            AutoPoseProvider pose,
            double targetX,
            double targetY,
            double targetHeading,
            double[] output) {
        double deltaX = targetX - pose.getXInches();
        double deltaY = targetY - pose.getYInches();
        double headingError = MathUtil.angleError(pose.getHeadingRadians(), targetHeading);

        double xPower = SwerveConfig.AUTO_X_P * deltaX;
        double yPower = SwerveConfig.AUTO_Y_P * deltaY;
        double headingPower = SwerveConfig.AUTO_HEADING_P * headingError;
        rotateAndClamp(pose.getHeadingRadians(), xPower, yPower, headingPower, output);
    }

    static double translationError(AutoPoseProvider pose, double targetX, double targetY) {
        return Math.hypot(targetX - pose.getXInches(), targetY - pose.getYInches());
    }

    static double headingError(AutoPoseProvider pose, double targetHeading) {
        return Math.abs(MathUtil.angleError(pose.getHeadingRadians(), targetHeading));
    }

    private static void rotateAndClamp(
            double robotHeading,
            double fieldXPower,
            double fieldYPower,
            double headingPower,
            double[] output) {
        double xRotated = fieldXPower * Math.cos(robotHeading) - fieldYPower * Math.sin(robotHeading);
        double yRotated = fieldXPower * Math.sin(robotHeading) + fieldYPower * Math.cos(robotHeading);
        double xClamped = Range.clip(-xRotated,
                -SwerveConfig.AUTO_MAX_TRANSLATION_POWER,
                SwerveConfig.AUTO_MAX_TRANSLATION_POWER);
        double yClamped = Range.clip(-yRotated,
                -SwerveConfig.AUTO_MAX_TRANSLATION_POWER,
                SwerveConfig.AUTO_MAX_TRANSLATION_POWER);
        double headingClamped = Range.clip(headingPower,
                -SwerveConfig.AUTO_MAX_TURN_POWER,
                SwerveConfig.AUTO_MAX_TURN_POWER);

        output[0] = -yClamped;
        output[1] = xClamped;
        output[2] = -headingClamped;
    }
}
