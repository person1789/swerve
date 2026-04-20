package org.firstinspires.ftc.teamcode.Swerve.Logic.Control;

import org.firstinspires.ftc.teamcode.Swerve.Core.PIDController;
import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose;

/**
 * SwerveController
 * 
 * The modular "Brain" of the swerve drivetrain. 
 * This class is designed to run on the robot (OpModes) OR in a simulator (SITL).
 */
public class SwerveController {

    private final PIDController snapController;
    private final PIDController maintainPID;

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
     * @param vx Field-centric X velocity [-1.0, 1.0]
     * @param vy Field-centric Y velocity [-1.0, 1.0]
     * @param turn Manual rotation input [-1.0, 1.0]
     * @param currentHeading Current robot heading in radians
     * @param dt Loop time in seconds
     * @return Target Pose (velocities in m/s and rad/s)
     */
    public Pose update(double vx, double vy, double turn, double currentHeading, double dt) {
        
        // 1. Determine if we are actively turning
        if (Math.abs(turn) > 0.05) {
            isSnapping = false;
            isMaintaining = false;
        }

        // 2. Heading Retention Logic
        boolean isMoving = Math.hypot(vx, vy) > 0.1;
        boolean noTurnInput = Math.abs(turn) < 0.05;

        double calculatedTurn;

        if (isSnapping) {
            calculatedTurn = snapController.calculate(currentHeading, targetHeading, dt);
            // Auto-stop snapping if we are close enough
            if (Math.abs(currentHeading - targetHeading) < 0.02) {
                isSnapping = false;
            }
        } else if (isMoving && noTurnInput) {
            if (!isMaintaining) {
                targetHeading = currentHeading;
                isMaintaining = true;
                maintainPID.reset();
            }
            calculatedTurn = maintainPID.calculate(currentHeading, targetHeading, dt);
        } else {
            isMaintaining = false;
            calculatedTurn = turn * SwerveConfig.MAX_ANGULAR_VELOCITY_RAD_S;
        }

        return new Pose(
            vx * SwerveConfig.MAX_SPEED_MPS,
            vy * SwerveConfig.MAX_SPEED_MPS,
            calculatedTurn
        );
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

    public boolean isMaintaining() { return isMaintaining; }
    public boolean isSnapping() { return isSnapping; }
    public double getTargetHeading() { return targetHeading; }
}
