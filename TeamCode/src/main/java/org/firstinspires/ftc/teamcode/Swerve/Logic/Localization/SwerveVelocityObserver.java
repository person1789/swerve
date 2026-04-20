package org.firstinspires.ftc.teamcode.Swerve.Logic.Localization;

import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.SwerveModule;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics.SwerveKinematics;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics.SwerveModuleState;

/**
 * SwerveVelocityObserver
 * 
 * Fuses module-level velocity feedback into a chassis-level robot velocity estimate.
 * Includes a configurable Low-Pass Filter to smooth out encoder chatter.
 */
public class SwerveVelocityObserver {

    private final SwerveKinematics kinematics;
    private Pose observedVelocity = new Pose(0, 0, 0);

    public SwerveVelocityObserver(SwerveKinematics kinematics) {
        this.kinematics = kinematics;
    }

    /**
     * Poll the current state of all modules and update the chassis velocity estimate.
     * @param modules Array of SwerveModules to read encoders from.
     */
    public void update(SwerveModule[] modules) {
        SwerveModuleState[] states = new SwerveModuleState[4];
        for (int i = 0; i < 4; i++) {
            states[i] = modules[i].getCurrentState();
        }
        update(states);
    }

    /**
     * Update the chassis velocity estimate using provided module states.
     * @param states Array of 4 module states (speed and angle).
     */
    public void update(SwerveModuleState[] states) {
        // Calculate raw chassis speed
        Pose rawVel = kinematics.toChassisSpeeds(states);

        // Apply Low-Pass Filter (LPF)
        double alpha = SwerveConfig.OBSERVER_LPF_GAIN;
        observedVelocity = new Pose(
            observedVelocity.x * (1 - alpha) + rawVel.x * alpha,
            observedVelocity.y * (1 - alpha) + rawVel.y * alpha,
            observedVelocity.heading * (1 - alpha) + rawVel.heading * alpha
        );
    }

    /**
     * @return The filtered robot velocity in chassis-space (meters/second and radians/second).
     */
    public Pose getVelocity() {
        return observedVelocity;
    }
}
