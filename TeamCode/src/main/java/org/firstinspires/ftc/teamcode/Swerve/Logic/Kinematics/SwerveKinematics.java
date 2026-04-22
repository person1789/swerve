package org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics;

import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Vector;

/**
 * Geometrically transforms chassis-space velocities into module-space vectors.
 * Implements second-order discretization to account for curvilinear paths
 */
public class SwerveKinematics {

    private final double halfW;
    private final double halfL;
    private double loopTimeSec;
    private final Vector[] moduleOffsets;

    /** Initializes robot dimensions from current configuration. */
    public SwerveKinematics() {
        this.halfW = (SwerveConfig.TRACK_WIDTH_IN * 0.0254) / 2.0;
        this.halfL = (SwerveConfig.WHEEL_BASE_IN * 0.0254) / 2.0;
        this.loopTimeSec = SwerveConfig.LOOP_TIME_SEC;

        // Module positions relative to robot center (FL, FR, RR, RL)
        this.moduleOffsets = new Vector[] {
                new Vector(halfL, halfW), // FL
                new Vector(halfL, -halfW), // FR
                new Vector(-halfL, -halfW), // RR
                new Vector(-halfL, halfW) // RL
        };
    }

    /**
     * Inverse Kinematics: Converts robot-level velocity vector (vx, vy, omega) into
     * four individual module states (speed and angle).
     * 
     * @param chassisSpeeds 3D Vector containing [vx, vy, omega]
     * @return Array of four module states.
     */
    public SwerveModuleState[] inverseKinematics(Vector chassisSpeeds) {
        double vx = chassisSpeeds.x();
        double vy = chassisSpeeds.y();
        double omega = chassisSpeeds.omega();

        double dt = loopTimeSec;
        double angleRad = omega * dt;
        Vector chassisTranslationalVelocity;

        if (Math.abs(angleRad) < 0.001) {
            chassisTranslationalVelocity = new Vector(vx, vy);
        } else {
            double sin = Math.sin(angleRad);
            double cos = Math.cos(angleRad);
            double s = sin / angleRad;
            double c = (1.0 - cos) / angleRad;
            // Second-order discretization
            chassisTranslationalVelocity = new Vector(
                    vx * s - vy * c,
                    vx * c + vy * s);
        }

        SwerveModuleState[] states = new SwerveModuleState[4];
        for (int i = 0; i < 4; i++) {
            Vector offset = moduleOffsets[i];

            // Module velocity vector = Robot velocity + cross(Rotation, Position)
            // Cross product in 2D: (-omega * y, omega * x)
            double moduleVx = chassisTranslationalVelocity.x() - omega * offset.y();
            double moduleVy = chassisTranslationalVelocity.y() + omega * offset.x();

            states[i] = SwerveModuleState.fromVector(moduleVx, moduleVy);
        }
        return states;
    }

    /**
     * Forward Kinematics: Resolves module states back into chassis velocity.
     * Used by the Velocity Observer for feedback fusion.
     */
    public Vector forwardKinematics(SwerveModuleState[] states) {
        double vx = 0, vy = 0, omega = 0;

        for (int i = 0; i < 4; i++) {
            Vector offset = moduleOffsets[i];
            double mvx = states[i].speedMetersPerSecond * Math.cos(states[i].angleRadians);
            double mvy = states[i].speedMetersPerSecond * Math.sin(states[i].angleRadians);

            vx += mvx;
            vy += mvy;
            omega += (offset.x() * mvy - offset.y() * mvx) / (offset.x() * offset.x() + offset.y() * offset.y());
        }

        return new Vector(vx / 4.0, vy / 4.0, omega / 4.0);
    }

    public void setLoopTimeSec(double loopTimeSec) {
        this.loopTimeSec = loopTimeSec;
    }
}
