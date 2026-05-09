package org.firstinspires.ftc.teamcode.Swerve.Hardware;

import com.qualcomm.robotcore.hardware.DcMotor;

/**
 * Minimal hardware contract for a single swerve module.
 */
public interface SwerveModuleIO {

    default void refreshInputs() {
    }

    double getCurrentRotationRadians();

    double getDriveVelocityMetersPerSecond();

    double getDriveCurrentAmps();

    void setDrivePower(double power);

    void setSteerPower(double power);

    default void setDriveMode(DcMotor.RunMode mode) {
    }

    default void setCalibration(double offsetRadians, boolean inverse) {
    }
}
