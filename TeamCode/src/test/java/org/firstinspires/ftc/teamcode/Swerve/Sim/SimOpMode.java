package org.firstinspires.ftc.teamcode.Swerve.Sim;

import java.util.Map;

/**
 * Interface for simulated OpModes that run against the SwerveSimulator.
 * Each implementation mirrors a real OpMode's logic using mock hardware.
 */
public interface SimOpMode {
    /** Called once when the OpMode is initialized. */
    void init(SwerveSimulator sim);

    /** Called repeatedly at ~50Hz while the OpMode is running. */
    void loop(BrowserGamepadState gamepad, double dt);

    /** Called when the OpMode is stopped. */
    void stop();

    /** Returns the current grouped telemetry (Bundle Name -> (Key -> Value)). */
    Map<String, Map<String, String>> getTelemetry();
}
