package org.firstinspires.ftc.teamcode.Swerve.Logic.Control;

import org.firstinspires.ftc.teamcode.Swerve.Core.MathUtil;
import org.firstinspires.ftc.teamcode.Swerve.Core.PIDController;
import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Vector;

/**
 * SwerveController
 * 
 * Parses controller inputs and outputs chassis-level velocity vectors.
 */
public class SwerveController {

    private final PIDController snapController;
    private final PIDController maintainPID;
    private double headingLockTimer = 0;

    private double targetHeading = 0.0;
    private boolean isSnapping = false;
    private boolean isMaintaining = false;

    public SwerveController() {
        this.snapController = new PIDController(SwerveConfig.SNAP_P, SwerveConfig.SNAP_I, SwerveConfig.SNAP_D);
        this.maintainPID = new PIDController(SwerveConfig.HEADING_P, SwerveConfig.HEADING_I, SwerveConfig.HEADING_D);
    }

    /**
     * Update the control logic and produce the target chassis speeds.
     * 
     * @param vx             Field-centric X velocity (m/s)
     * @param vy             Field-centric Y velocity (m/s)
     * @param turn           Manual rotation input (rad/s)
     * @param currentHeading Current robot heading in radians
     * @param dt             Loop time in seconds
     * @return Target Vector (vx, vy, omega)
     */
    public Vector update(double vx, double vy, double turn, double currentHeading, double dt) {

        // 1. Apply Component-wise Deadband (on the 3D input vector)
        Vector input = MathUtil.applyDeadband(new Vector(vx, vy, turn), SwerveConfig.INPUT_DEADBAND);
        double dvx = input.x();
        double dvy = input.y();
        double dturn = input.omega();

        if (Math.abs(dturn) > 1e-6) {
            isSnapping = false;
            isMaintaining = false;
        }

        // 2. Heading Retention Logic
        boolean isMoving = input.magnitude() > 1e-6;
        boolean isTurning = Math.abs(dturn) > 1e-6;

        double calculatedTurn;

        if (isSnapping) {
            calculatedTurn = snapController.calculate(currentHeading, targetHeading, dt);
            // Auto-stop snapping if we are close enough
            if (Math.abs(MathUtil.angleError(currentHeading, targetHeading)) < 0.05) {
                isSnapping = false;
            }
        } else if (isMoving && !isTurning) {
            headingLockTimer += dt;
            if (headingLockTimer > SwerveConfig.HEADING_LOCK_DELAY_S) {
                if (!isMaintaining) {
                    targetHeading = currentHeading;
                    isMaintaining = true;
                    maintainPID.reset();
                }
                calculatedTurn = maintainPID.calculate(currentHeading, targetHeading, dt);
            } else {
                calculatedTurn = 0;
            }
        } else {
            headingLockTimer = 0;
            isMaintaining = false;
            calculatedTurn = dturn;
        }

        return new Vector(dvx, dvy, calculatedTurn);
    }

    public void setSnapTarget(double angleRad) {
        this.targetHeading = angleRad;
        this.isSnapping = true;
        this.isMaintaining = false;
    }

    public void resetHeading(double currentHeading) {
        this.targetHeading = currentHeading;
        this.isSnapping = false;
        this.isMaintaining = false;
    }

    public boolean isMaintaining() {
        return isMaintaining;
    }

    public boolean isSnapping() {
        return isSnapping;
    }

    public double getTargetHeading() {
        return targetHeading;
    }
}
