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
            states[i].speedMetersPerSecond = Math.hypot(moduleVx, moduleVy);
            states[i].angleRadians = Math.atan2(moduleVy, moduleVx);
        }
    }

    /**
     * Forward Kinematics: Resolves module states back into chassis velocity.
     * Used by the Velocity Observer for feedback fusion.
     */
    public Vector forwardKinematics(SwerveModuleState[] states) {
        double[] velocity = new double[3];
        forwardKinematics(states, velocity);
        return new Vector(velocity[0], velocity[1], velocity[2]);
    }

    public void forwardKinematics(SwerveModuleState[] states, double[] velocityOut) {
        double vx = 0, vy = 0, omega = 0;

        for (int i = 0; i < 4; i++) {
            Vector offset = moduleOffsets[i];
            double mvx = states[i].speedMetersPerSecond * Math.cos(states[i].angleRadians);
            double mvy = states[i].speedMetersPerSecond * Math.sin(states[i].angleRadians);

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

    public Vector projectToCurrentAngleFeasibleVelocity(Vector desiredVelocity, double[] currentAnglesRad, double penalty) {
        double[][] normal = {
                {1.0, 0.0, 0.0},
                {0.0, 1.0, 0.0},
                {0.0, 0.0, 1.0}
        };
        double[] rhs = {desiredVelocity.x(), desiredVelocity.y(), desiredVelocity.omega()};

        for (int i = 0; i < 4; i++) {
            Vector offset = moduleOffsets[i];
            double theta = currentAnglesRad[i];
            double sin = Math.sin(theta);
            double cos = Math.cos(theta);

            double[] row = {
                    -sin,
                    cos,
                    sin * offset.y() + cos * offset.x()
            };

            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    normal[r][c] += penalty * row[r] * row[c];
                }
            }
        }

        double[] solution = solve3x3(normal, rhs);
        Vector result = new Vector(solution[0], solution[1], solution[2]);
        
        return result;
    }

    public void setLoopTimeSec(double loopTimeSec) {
        this.loopTimeSec = loopTimeSec;
    }

    private double[] solve3x3(double[][] a, double[] b) {
        double[][] m = new double[3][4];
        for (int r = 0; r < 3; r++) {
            System.arraycopy(a[r], 0, m[r], 0, 3);
            m[r][3] = b[r];
        }

        for (int pivot = 0; pivot < 3; pivot++) {
            int bestRow = pivot;
            for (int row = pivot + 1; row < 3; row++) {
                if (Math.abs(m[row][pivot]) > Math.abs(m[bestRow][pivot])) {
                    bestRow = row;
                }
            }

            if (Math.abs(m[bestRow][pivot]) < 1e-9) {
                return new double[] {0.0, 0.0, 0.0};
            }

            if (bestRow != pivot) {
                double[] tmp = m[pivot];
                m[pivot] = m[bestRow];
                m[bestRow] = tmp;
            }

            double scale = m[pivot][pivot];
            for (int c = pivot; c < 4; c++) {
                m[pivot][c] /= scale;
            }

            for (int row = 0; row < 3; row++) {
                if (row == pivot) {
                    continue;
                }
                double factor = m[row][pivot];
                for (int c = pivot; c < 4; c++) {
                    m[row][c] -= factor * m[pivot][c];
                }
            }
        }

        return new double[] {m[0][3], m[1][3], m[2][3]};
    }
}
