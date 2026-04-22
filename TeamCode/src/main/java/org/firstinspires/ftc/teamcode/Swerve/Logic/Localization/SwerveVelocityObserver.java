package org.firstinspires.ftc.teamcode.Swerve.Logic.Localization;

import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.hardware.SwerveModule;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics.SwerveKinematics;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics.SwerveModuleState;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Vector;

/**
 * SwerveVelocityObserver
 * 
 * Fuses module-level velocity feedback into a chassis-level robot velocity estimate.
 * Includes a configurable Low-Pass Filter to smooth out encoder chatter.
 */
public class SwerveVelocityObserver {

    private final SwerveKinematics kinematics;
    private Vector observedVelocity = new Vector(0, 0, 0);

    public SwerveVelocityObserver(SwerveKinematics kinematics) {
        this.kinematics = kinematics;
    }

    /**
     * Poll the current state of all modules and update the chassis velocity estimate.
     * @param modules Array of SwerveModules to read encoders from.
     */
    public void update(org.firstinspires.ftc.teamcode.Swerve.Hardware.SwerveModule[] modules) {
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
        Vector instantVelocity = kinematics.forwardKinematics(states);

        // Apply Low-Pass Filter using Vector lerp (efficient single-allocation)
        double alpha = SwerveConfig.OBSERVER_LPF_GAIN;
        observedVelocity = observedVelocity.lerp(instantVelocity, alpha);
    }

    /**
     * @return The smoothed velocity estimate in chassis-space (vx, vy, omega).
     */
    public Vector getVelocity() {
        return observedVelocity;
    }

    /**
     * Resets the observer estimate to zero.
     */
    public void reset() {
        observedVelocity = new Vector(0, 0, 0);
    }
}
