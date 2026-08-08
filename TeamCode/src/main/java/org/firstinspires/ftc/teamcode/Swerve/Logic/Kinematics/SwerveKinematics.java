package org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics;

import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Geometry.Pose;
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

        // Module positions relative to robot center (FL, FR, RR, RL).
        this.moduleOffsets = new Vector[] {
                moduleOffset(0, halfL, halfW),
                moduleOffset(1, halfL, -halfW),
                moduleOffset(2, -halfL, -halfW),
                moduleOffset(3, -halfL, halfW)
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
        SwerveModuleState[] states = new SwerveModuleState[4];
        inverseKinematics(chassisSpeeds.x(), chassisSpeeds.y(), chassisSpeeds.omega(), states);
        return states;
    }

    public SwerveModuleState[] toModuleStates(double vx, double vy, double omega) {
        SwerveModuleState[] states = new SwerveModuleState[4];
        inverseKinematics(vx, vy, omega, states);
        return states;
    }

    public void inverseKinematics(double vx, double vy, double omega, SwerveModuleState[] states) {
        if (states == null || states.length != 4) {
            throw new IllegalArgumentException("inverseKinematics requires a 4-element output array.");
        }

        double dt = loopTimeSec;
        double angleRad = omega * dt;
        double chassisVx;
        double chassisVy;

        if (Math.abs(angleRad) < 0.001) {
            chassisVx = vx;
            chassisVy = vy;
        } else {
            double sin = Math.sin(angleRad);
            double cos = Math.cos(angleRad);
            double s = sin / angleRad;
            double c = (1.0 - cos) / angleRad;
            chassisVx = vx * s - vy * c;
            chassisVy = vx * c + vy * s;
        }

        for (int i = 0; i < 4; i++) {
            if (states[i] == null) {
                states[i] = new SwerveModuleState();
            }

            Vector offset = moduleOffsets[i];
            double moduleVx = chassisVx - omega * offset.y();
            double moduleVy = chassisVy + omega * offset.x();
            double speedMps = Math.hypot(moduleVx, moduleVy);
            states[i].drivePower = speedMps / SwerveConfig.getMaxLinearSpeedMPS();
            states[i].angleRadians = Math.atan2(moduleVy, moduleVx);
        }
    }

    /** Resolves module states back into chassis velocity for unit tests and diagnostics. */
    public Vector forwardKinematics(SwerveModuleState[] states) {
        double[] velocity = new double[3];
        forwardKinematics(states, velocity);
        return new Vector(velocity[0], velocity[1], velocity[2]);
    }

    public void forwardKinematics(SwerveModuleState[] states, double[] velocityOut) {
        double vx = 0, vy = 0, omega = 0;

        for (int i = 0; i < 4; i++) {
            Vector offset = moduleOffsets[i];
            double mvx = states[i].drivePower * SwerveConfig.getMaxLinearSpeedMPS() * Math.cos(states[i].angleRadians);
            double mvy = states[i].drivePower * SwerveConfig.getMaxLinearSpeedMPS() * Math.sin(states[i].angleRadians);

            vx += mvx;
            vy += mvy;
            omega += (offset.x() * mvy - offset.y() * mvx) / (offset.x() * offset.x() + offset.y() * offset.y());
        }

        velocityOut[0] = vx / 4.0;
        velocityOut[1] = vy / 4.0;
        velocityOut[2] = omega / 4.0;
    }

    public Pose toChassisSpeeds(SwerveModuleState[] states) {
        return Pose.from(forwardKinematics(states));
    }

    public void setLoopTimeSec(double loopTimeSec) {
        this.loopTimeSec = loopTimeSec;
    }

    private static Vector moduleOffset(int index, double fallbackX, double fallbackY) {
        if (SwerveConfig.MODULE_X_IN == null || SwerveConfig.MODULE_Y_IN == null
                || SwerveConfig.MODULE_X_IN.length != 4 || SwerveConfig.MODULE_Y_IN.length != 4) {
            return new Vector(fallbackX, fallbackY);
        }
        return new Vector(
                SwerveConfig.MODULE_X_IN[index] * 0.0254,
                SwerveConfig.MODULE_Y_IN[index] * 0.0254);
    }
}
