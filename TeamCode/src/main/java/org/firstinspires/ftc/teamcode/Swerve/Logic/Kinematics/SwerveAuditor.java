package org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics;

import org.firstinspires.ftc.teamcode.Swerve.Core.MathUtil;
import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;

/**
 * SwerveAuditor: Optimizes module states for shortest-path steering and
 * ensures the robot doesn't attempt to exceed physical speed limits.
 */
public class SwerveAuditor {

    /**
     * One-pass optimization for all modules.
     * Handles NaN protection, shortest-path logic, and speed desaturation.
     * 
     * @param desiredStates    The raw module states from kinematics.
     * @param currentAnglesRad The current actual rotation of the modules.
     * @return Optimized and normalized module states.
     */
    public SwerveModuleState[] optimize(SwerveModuleState[] desiredStates, double[] currentAnglesRad) {
        for (SwerveModuleState s : desiredStates) {
            if (Double.isNaN(s.speedMetersPerSecond))
                s.speedMetersPerSecond = 0.0;
            if (Double.isNaN(s.angleRadians))
                s.angleRadians = 0.0;
        }

        SwerveModuleState[] optimized = new SwerveModuleState[4];
        double maxFound = 0.0;

        for (int i = 0; i < 4; i++) {
            SwerveModuleState state = desiredStates[i].copy();
            double error = MathUtil.angleError(currentAnglesRad[i], state.angleRadians);

            if (Math.abs(error) > SwerveConfig.FLIP_THRESHOLD) {
                state.speedMetersPerSecond *= -1.0;
                state.angleRadians = MathUtil.normalizeAngle(state.angleRadians + Math.PI);
                error = MathUtil.angleError(currentAnglesRad[i], state.angleRadians);
            }

            double cosineScale = Math.cos(error);
            state.speedMetersPerSecond *= Math.max(0, cosineScale);

            optimized[i] = state;
            maxFound = Math.max(maxFound, Math.abs(state.speedMetersPerSecond));
        }

        if (maxFound > SwerveConfig.MAX_SPEED_MPS) {
            double scale = SwerveConfig.MAX_SPEED_MPS / maxFound;
            for (SwerveModuleState s : optimized) {
                s.speedMetersPerSecond *= scale;
            }
        }

        return optimized;
    }
}
