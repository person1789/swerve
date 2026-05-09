package org.firstinspires.ftc.teamcode.auto;

import org.firstinspires.ftc.teamcode.Swerve.Core.MathUtil;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Vector;

/**
 * Small sequential autonomous runner that mirrors Kooky's pose-command style
 * without bringing in a heavy pathing framework.
 */
public class SimpleAutoSequence {
    public static double ALLOWED_TRANSLATIONAL_ERROR_IN = 1.0;
    public static double ALLOWED_HEADING_ERROR_RAD = Math.toRadians(3.0);

    private final AutoPoseStep[] steps;
    private final KookyAutoController controller;
    private int currentStepIndex = 0;
    private double stepElapsedSec = 0.0;
    private double settledElapsedSec = 0.0;

    public SimpleAutoSequence(KookyAutoController controller, AutoPoseStep... steps) {
        this.controller = controller;
        this.steps = steps;
    }

    public Vector update(Pose currentPose, double dtSec) {
        double[] command = new double[3];
        update(currentPose.x, currentPose.y, currentPose.heading, dtSec, command);
        return new Vector(command[0], command[1], command[2]);
    }

    public void update(double currentX, double currentY, double currentHeading, double dtSec, double[] commandOut) {
        if (isFinished()) {
            commandOut[0] = 0.0;
            commandOut[1] = 0.0;
            commandOut[2] = 0.0;
            return;
        }

        AutoPoseStep step = steps[currentStepIndex];
        stepElapsedSec += dtSec;

        boolean atTarget = isAtTarget(currentX, currentY, currentHeading, step.targetPose);
        settledElapsedSec = atTarget ? (settledElapsedSec + dtSec) : 0.0;

        if (settledElapsedSec >= step.settleTimeSec || stepElapsedSec >= step.timeoutSec) {
            currentStepIndex++;
            stepElapsedSec = 0.0;
            settledElapsedSec = 0.0;
            if (isFinished()) {
                commandOut[0] = 0.0;
                commandOut[1] = 0.0;
                commandOut[2] = 0.0;
                return;
            }
            step = steps[currentStepIndex];
        }

        controller.update(currentX, currentY, currentHeading, step.targetPose, commandOut);
    }

    public boolean isFinished() {
        return currentStepIndex >= steps.length;
    }

    public int getCurrentStepIndex() {
        return currentStepIndex;
    }

    public Pose getCurrentTargetPose() {
        return isFinished() ? null : steps[currentStepIndex].targetPose;
    }

    private boolean isAtTarget(Pose currentPose, Pose targetPose) {
        return isAtTarget(currentPose.x, currentPose.y, currentPose.heading, targetPose);
    }

    private boolean isAtTarget(double currentX, double currentY, double currentHeading, Pose targetPose) {
        double translationError = Math.hypot(targetPose.x - currentX, targetPose.y - currentY);
        double headingError = Math.abs(MathUtil.angleError(currentHeading, targetPose.heading));
        return translationError <= ALLOWED_TRANSLATIONAL_ERROR_IN
                && headingError <= ALLOWED_HEADING_ERROR_RAD;
    }
}
