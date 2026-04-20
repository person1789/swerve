/**
 * SwerveKinematics: The "math engine" of the swerve drive.
 * This file takes the robot's desired movement (like "move forward and turn left") 
 * and calculates the specific speed and angle each of the four wheels needs to 
 * move at to make that robot motion happen.
 */
package org.firstinspires.ftc.teamcode.Swerve.Drive;

import org.firstinspires.ftc.teamcode.Swerve.Geo.MathUtil;

/**
 * SwerveKinematics — Phase B
 */
public class SwerveKinematics {

    private final double halfW;
    private final double halfL;
    private double loopTimeSec;
    private final double[][] moduleOffsets;

    /**
     * Construct kinematics using robot dimensions from SwerveConfig.
     */
    public SwerveKinematics() {
        this.halfW = (SwerveConfig.TRACK_WIDTH_IN * 0.0254) / 2.0;
        this.halfL = (SwerveConfig.WHEEL_BASE_IN  * 0.0254) / 2.0;
        this.loopTimeSec = SwerveConfig.LOOP_TIME_SEC;

        // FL, FR, RR, RL  (Standard module order)
        this.moduleOffsets = new double[][] {
            { halfL,  halfW},   // 0 — Front-Left
            { halfL, -halfW},   // 1 — Front-Right
            {-halfL, -halfW},   // 2 — Rear-Right
            {-halfL,  halfW}    // 3 — Rear-Left
        };
    }

    /**
     * Convert chassis-level velocity into four module states.
     */
    public SwerveModuleState[] toModuleStates(double vx, double vy, double omega) {
        // Skew correction
        double halfAngle = omega * loopTimeSec / 2.0;
        double cosH = Math.cos(halfAngle);
        double sinH = Math.sin(halfAngle);
        double vxCorr = vx * cosH - vy * sinH;
        double vyCorr = vx * sinH + vy * cosH;

        SwerveModuleState[] states = new SwerveModuleState[4];
        for (int i = 0; i < 4; i++) {
            double lx = moduleOffsets[i][0];
            double ly = moduleOffsets[i][1];

            double moduleVx = vxCorr - omega * ly;
            double moduleVy = vyCorr + omega * lx;

            states[i] = SwerveModuleState.fromVector(moduleVx, moduleVy);
        }
        return states;
    }

    public static void normalizeModuleSpeeds(SwerveModuleState[] states, double maxSpeed) {
        double maxFound = 0.0;
        for (SwerveModuleState s : states) {
            maxFound = Math.max(maxFound, Math.abs(s.speedMetersPerSecond));
        }
        if (maxFound > maxSpeed) {
            double scale = maxSpeed / maxFound;
            for (SwerveModuleState s : states) {
                s.speedMetersPerSecond *= scale;
            }
        }
    }

    public void setLoopTimeSec(double loopTimeSec) {
        this.loopTimeSec = loopTimeSec;
    }
}