/**
 * SwerveAuditor: An optimization tool that cleans up wheel commands.
 * It looks at the calculated wheel directions and makes smart adjustments, 
 * such as telling a wheel to spin backwards if it can reach the target direction 
 * faster by doing so, reducing mechanical wear and steering time.
 */
package org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics;

import org.firstinspires.ftc.teamcode.Swerve.Core.MathUtil;

/**
 * SwerveAuditor — Phase B
 */
public class SwerveAuditor {

    private static final double FLIP_THRESHOLD = Math.PI / 2.0;

    /**
     * Optimize four module states and normalize speeds.
     */
    public SwerveModuleState[] optimize(SwerveModuleState[] desiredStates,
                                        double[] currentAnglesRad,
                                        double maxSpeed) {
        
        SwerveModuleState[] optimized = new SwerveModuleState[4];

        for (int i = 0; i < 4; i++) {
            optimized[i] = optimizeSingle(desiredStates[i].copy(), currentAnglesRad[i]);
        }

        normalizeByMax(optimized, maxSpeed);
        return optimized;
    }

    public static SwerveModuleState optimizeSingle(SwerveModuleState state,
                                                   double currentAngle) {
        double error = MathUtil.angleError(currentAngle, state.angleRadians);

        if (Math.abs(error) > FLIP_THRESHOLD) {
            // Flip: reverse drive direction and aim for the opposite heading.
            state.speedMetersPerSecond *= -1.0;
            state.angleRadians = MathUtil.normalizeAngle(state.angleRadians + Math.PI);
        }

        return state;
    }

    public static void normalizeByMax(SwerveModuleState[] states, double maxSpeed) {
        double maxFound = 0.0;
        for (SwerveModuleState s : states) {
            maxFound = Math.max(maxFound, Math.abs(s.speedMetersPerSecond));
        }

        if (maxFound > maxSpeed && maxFound > 1e-9) {
            double scale = maxSpeed / maxFound;
            for (SwerveModuleState s : states) {
                s.speedMetersPerSecond *= scale;
            }
        }
    }
}