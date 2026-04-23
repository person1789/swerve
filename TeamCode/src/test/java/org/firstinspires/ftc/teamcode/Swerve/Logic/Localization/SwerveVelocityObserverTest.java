package org.firstinspires.ftc.teamcode.Swerve.Logic.Localization;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics.SwerveKinematics;
import org.firstinspires.ftc.teamcode.Swerve.Logic.Kinematics.SwerveModuleState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SwerveVelocityObserverTest {

    private final double originalGain = SwerveConfig.OBSERVER_LPF_GAIN;

    @AfterEach
    void restoreConfig() {
        SwerveConfig.OBSERVER_LPF_GAIN = originalGain;
    }

    @Test
    void updateBlendsInstantVelocityUsingConfiguredLowPassGain() {
        // Passes if the observer output moves toward the instantaneous chassis velocity by exactly alpha on each update.
        SwerveConfig.OBSERVER_LPF_GAIN = 0.5;
        SwerveVelocityObserver observer = new SwerveVelocityObserver(new SwerveKinematics());
        SwerveModuleState[] states = {
                new SwerveModuleState(1.0, 0.0),
                new SwerveModuleState(1.0, 0.0),
                new SwerveModuleState(1.0, 0.0),
                new SwerveModuleState(1.0, 0.0)
        };

        observer.update(states);

        assertEquals(0.5, observer.getVelocity().x(), 1e-9);
        assertEquals(0.0, observer.getVelocity().y(), 1e-9);
        assertEquals(0.0, observer.getVelocity().omega(), 1e-9);
    }

    @Test
    void resetReturnsObserverStateToZero() {
        // Passes if reset clears the filtered velocity estimate back to zero in all axes.
        SwerveConfig.OBSERVER_LPF_GAIN = 1.0;
        SwerveVelocityObserver observer = new SwerveVelocityObserver(new SwerveKinematics());
        SwerveModuleState[] states = {
                new SwerveModuleState(1.0, 0.0),
                new SwerveModuleState(1.0, 0.0),
                new SwerveModuleState(1.0, 0.0),
                new SwerveModuleState(1.0, 0.0)
        };
        observer.update(states);

        observer.reset();

        assertEquals(0.0, observer.getVelocity().x(), 1e-9);
        assertEquals(0.0, observer.getVelocity().y(), 1e-9);
        assertEquals(0.0, observer.getVelocity().omega(), 1e-9);
    }
}
