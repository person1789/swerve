package org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics;

import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose;

/**
 * SwerveKinematics
 * 
 * Geometrically transforms chassis-space velocities into module-space vectors.
 * Implements second-order discretization to account for curvilinear paths 
 * during high-speed maneuvers.
 */
public class SwerveKinematics {

    private final double halfW;
    private final double halfL;
    private double loopTimeSec;
    private final double[][] moduleOffsets;

    /**
     * Initializes robot dimensions from current configuration.
     */
    public SwerveKinematics() {
        this.halfW = (SwerveConfig.TRACK_WIDTH_IN * 0.0254) / 2.0;
        this.halfL = (SwerveConfig.WHEEL_BASE_IN  * 0.0254) / 2.0;
        this.loopTimeSec = SwerveConfig.LOOP_TIME_SEC;

        // Module positions relative to robot center (FL, FR, RR, RL)
        this.moduleOffsets = new double[][] {
            { halfL,  halfW}, { halfL, -halfW}, {-halfL, -halfW}, {-halfL,  halfW}
        };
    }

    /**
     * Convert chassis-level velocity into four module states using second-order discretization.
     * 
     * @param vx    Forward velocity (m/s).
     * @param vy    Strafe velocity (m/s).
     * @param omega Angular velocity (rad/s).
     * @return      Array of four module states containing target speed and angle.
     */
    public SwerveModuleState[] toModuleStates(double vx, double vy, double omega) {
        // Discretization centers the curved path over the loop window to eliminate rotational skew.
        double dt = loopTimeSec;
        double angleRad = omega * dt;
        double vxCorr, vyCorr;
        
        if (Math.abs(angleRad) < 1e-6) {
            vxCorr = vx;
            vyCorr = vy;
        } else {
            double sin = Math.sin(angleRad);
            double cos = Math.cos(angleRad);
            double s = sin / angleRad;
            double c = (1.0 - cos) / angleRad;
            vxCorr = vx * s - vy * c;
            vyCorr = vx * c + vy * s;
        }

        SwerveModuleState[] states = new SwerveModuleState[4];
        for (int i = 0; i < 4; i++) {
            double lx = moduleOffsets[i][0];
            double ly = moduleOffsets[i][1];

            // Module velocity vector = Robot velocity + cross(Rotation, Position)
            double moduleVx = vxCorr - omega * ly;
            double moduleVy = vyCorr + omega * lx;

            states[i] = SwerveModuleState.fromVector(moduleVx, moduleVy);
        }
        return states;
    }

    /**
     * Forward Kinematics: Resolves module states back into chassis velocity.
     * Used by the Velocity Observer for feedback fusion.
     */
    public Pose toChassisSpeeds(SwerveModuleState[] states) {
        double vx = 0, vy = 0, omega = 0;

        for (int i = 0; i < 4; i++) {
            double lx = moduleOffsets[i][0];
            double ly = moduleOffsets[i][1];
            double mvx = states[i].speedMetersPerSecond * Math.cos(states[i].angleRadians);
            double mvy = states[i].speedMetersPerSecond * Math.sin(states[i].angleRadians);

            vx += mvx;
            vy += mvy;
            omega += (lx * mvy - ly * mvx) / (lx * lx + ly * ly);
        }

        return new Pose(vx / 4.0, vy / 4.0, omega / 4.0);
    }

    public void setLoopTimeSec(double loopTimeSec) { this.loopTimeSec = loopTimeSec; }
}
