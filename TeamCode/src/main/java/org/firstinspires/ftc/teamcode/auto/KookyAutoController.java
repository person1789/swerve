package org.firstinspires.ftc.teamcode.auto;

import org.firstinspires.ftc.teamcode.Swerve.Core.MathUtil;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Vector;

/**
 * Minimal pose controller inspired by KookyBotz position commands.
 *
 * Takes field-space pose error and outputs normalized robot-relative chassis
 * commands for the drivetrain.
 */
public class KookyAutoController {
    public static double X_P = 0.04;
    public static double Y_P = 0.04;
    public static double H_P = 0.60;
    public static double MAX_TRANSLATION = 1.0;
    public static double MAX_ROTATION = 0.6;

    public Vector update(Pose currentPose, Pose targetPose) {
        double[] command = new double[3];
        update(currentPose.x, currentPose.y, currentPose.heading, targetPose, command);
        return new Vector(command[0], command[1], command[2]);
    }

    public void update(double currentX, double currentY, double currentHeading, Pose targetPose, double[] commandOut) {
        double errorX = targetPose.x - currentX;
        double errorY = targetPose.y - currentY;
        double errorHeading = MathUtil.angleError(currentHeading, targetPose.heading);

        double fieldForward = errorX * X_P;
        double fieldStrafe = errorY * Y_P;
        double translationMagnitude = Math.hypot(fieldForward, fieldStrafe);
        if (translationMagnitude > MAX_TRANSLATION && translationMagnitude > 1e-9) {
            double scale = MAX_TRANSLATION / translationMagnitude;
            fieldForward *= scale;
            fieldStrafe *= scale;
        }
        double turn = MathUtil.clamp(errorHeading * H_P, -MAX_ROTATION, MAX_ROTATION);

        double cos = Math.cos(-currentHeading);
        double sin = Math.sin(-currentHeading);
        commandOut[0] = fieldForward * cos - fieldStrafe * sin;
        commandOut[1] = fieldForward * sin + fieldStrafe * cos;
        commandOut[2] = turn;
    }
}
