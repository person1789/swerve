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
            state.speedMetersPerSecond *= steerDriveScale(Math.abs(error));

            optimized[i] = state;
            maxFound = Math.max(maxFound, Math.abs(state.speedMetersPerSecond));
        }

        double maxLinearSpeedMps = SwerveConfig.getMaxLinearSpeedMPS();
        if (maxFound > maxLinearSpeedMps) {
            double scale = maxLinearSpeedMps / maxFound;
            for (SwerveModuleState s : optimized) {
                s.speedMetersPerSecond *= scale;
            }
        }

        return optimized;
    }

    private double steerDriveScale(double absErrorRad) {
        if (absErrorRad <= SwerveConfig.STEER_DRIVE_FULL_AUTHORITY_RAD) {
            return 1.0;
        }
        if (absErrorRad >= SwerveConfig.STEER_DRIVE_HARD_CUTOFF_RAD) {
            return SwerveConfig.STEER_DRIVE_MIN_AUTHORITY;
        }

        double range = SwerveConfig.STEER_DRIVE_HARD_CUTOFF_RAD - SwerveConfig.STEER_DRIVE_FULL_AUTHORITY_RAD;
        double normalized = (SwerveConfig.STEER_DRIVE_HARD_CUTOFF_RAD - absErrorRad) / range;
        double smooth = normalized * normalized * (3.0 - 2.0 * normalized);
        return SwerveConfig.STEER_DRIVE_MIN_AUTHORITY
                + (1.0 - SwerveConfig.STEER_DRIVE_MIN_AUTHORITY) * smooth;
    }
}
