package org.firstinspires.ftc.teamcode.Swerve.Logic.Localization;

import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Hardware.SwerveModule;
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
    private final SwerveModuleState[] stateCache = {
            new SwerveModuleState(),
            new SwerveModuleState(),
            new SwerveModuleState(),
            new SwerveModuleState()
    };
    private final double[] instantVelocity = new double[3];
    private double observedVx = 0.0;
    private double observedVy = 0.0;
    private double observedOmega = 0.0;

    public SwerveVelocityObserver(SwerveKinematics kinematics) {
        this.kinematics = kinematics;
    }

    /**
     * Poll the current state of all modules and update the chassis velocity estimate.
     * @param modules Array of SwerveModules to read encoders from.
     */
    public void update(org.firstinspires.ftc.teamcode.Swerve.Hardware.SwerveModule[] modules) {
        for (int i = 0; i < 4; i++) {
            modules[i].copyCurrentStateInto(stateCache[i]);
        }
        update(stateCache);
    }

    /**
     * Update the chassis velocity estimate using provided module states.
     * @param states Array of 4 module states (speed and angle).
     */
    public void update(SwerveModuleState[] states) {
        kinematics.forwardKinematics(states, instantVelocity);
        double alpha = SwerveConfig.OBSERVER_LPF_GAIN;
        observedVx += alpha * (instantVelocity[0] - observedVx);
        observedVy += alpha * (instantVelocity[1] - observedVy);
        observedOmega += alpha * (instantVelocity[2] - observedOmega);
    }

    /**
     * @return The smoothed velocity estimate in chassis-space (vx, vy, omega).
     */
    public Vector getVelocity() {
        return new Vector(observedVx, observedVy, observedOmega);
    }

    /**
     * Resets the observer estimate to zero.
     */
    public void reset() {
        observedVx = 0.0;
        observedVy = 0.0;
        observedOmega = 0.0;
    }
}
