package org.firstinspires.ftc.teamcode.auto;

import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.teamcode.Swerve.Core.MathUtil;
import com.arcrobotics.ftclib.controller.PIDFController;
import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;

final class AutoMath {
    private AutoMath() {
    }

    static void calculateKookyPowers(
            AutoPoseProvider pose,
            double targetX,
            double targetY,
            double targetHeading,
            PIDFController xController,
            PIDFController yController,
            PIDFController headingController,
            double dt,
            double[] output) {
        double xPower = xController.calculate(pose.getXInches(), targetX);
        double yPower = yController.calculate(pose.getYInches(), targetY);
        
        // Manual angle wrap for continuous input since FTCLib doesn't support it natively
        double headingError = MathUtil.angleError(pose.getHeadingRadians(), targetHeading);
        double headingPower = headingController.calculate(0.0, headingError);
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
        
        // Field-centric to Robot-centric rotation (rotate field vector by -robotHeading)
        double cos = Math.cos(-robotHeading);
        double sin = Math.sin(-robotHeading);
        double robotForwardPower = fieldXPower * cos - fieldYPower * sin;
        double robotStrafePower = fieldXPower * sin + fieldYPower * cos;

        double forwardClamped = Range.clip(robotForwardPower,
                -SwerveConfig.AUTO_MAX_TRANSLATION_POWER,
                SwerveConfig.AUTO_MAX_TRANSLATION_POWER);
        double strafeClamped = Range.clip(robotStrafePower,
                -SwerveConfig.AUTO_MAX_TRANSLATION_POWER,
                SwerveConfig.AUTO_MAX_TRANSLATION_POWER);
        double turnClamped = Range.clip(headingPower,
                -SwerveConfig.AUTO_MAX_TURN_POWER,
                SwerveConfig.AUTO_MAX_TURN_POWER);

        if (Math.abs(forwardClamped) < SwerveConfig.AUTO_TRANSLATION_DEADBAND) {
            forwardClamped = 0.0;
        }
        if (Math.abs(strafeClamped) < SwerveConfig.AUTO_TRANSLATION_DEADBAND) {
            strafeClamped = 0.0;
        }

        // output[0] = Forward (X)
        // output[1] = Strafe (Y)
        // output[2] = Turn (Omega)
        output[0] = forwardClamped;
        output[1] = strafeClamped;
        output[2] = turnClamped;
    }
}
