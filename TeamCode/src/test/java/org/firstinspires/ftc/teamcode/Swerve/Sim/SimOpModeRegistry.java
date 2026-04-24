package org.firstinspires.ftc.teamcode.Swerve.Sim;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Manages simulated OpModes: registration, selection, and lifecycle transitions.
 * Mirrors the FTC SDK's OpMode state machine: STOPPED → INITIALIZED → RUNNING.
 */
public class SimOpModeRegistry {

    public enum OpModeState { STOPPED, INITIALIZED, RUNNING }

    private final Map<String, Supplier<SimOpMode>> registry = new LinkedHashMap<>();
    private SimOpMode activeOpMode;
    private String activeOpModeName = "";
    private OpModeState state = OpModeState.STOPPED;
    private SwerveSimulator simulator;

    public SimOpModeRegistry(SwerveSimulator simulator) {
        this.simulator = simulator;
        register("MainTeleOp", SimMainTeleOp::new);
        register("SwerveSystemCheck", SimSystemCheck::new);
    }

    public void register(String name, Supplier<SimOpMode> supplier) {
        registry.put(name, supplier);
    }

    public List<String> getAvailableOpModes() {
        return new ArrayList<>(registry.keySet());
    }

    public String getActiveOpModeName() {
        return activeOpModeName;
    }

    public OpModeState getState() {
        return state;
    }

    public synchronized void select(String name) {
        if (!registry.containsKey(name)) return;
        stop();
        activeOpModeName = name;
    }

    public synchronized void init() {
        if (activeOpModeName.isEmpty() || !registry.containsKey(activeOpModeName)) return;
        stop();
        activeOpMode = registry.get(activeOpModeName).get();
        activeOpMode.init(simulator);
        state = OpModeState.INITIALIZED;
    }

    public synchronized void start() {
        if (state == OpModeState.INITIALIZED) {
            state = OpModeState.RUNNING;
        }
    }

    public synchronized void stop() {
        if (activeOpMode != null) {
            activeOpMode.stop();
        }
        activeOpMode = null;
        state = OpModeState.STOPPED;
    }

    public synchronized void loop(BrowserGamepadState gamepad, double dt) {
        if (state == OpModeState.RUNNING && activeOpMode != null) {
            activeOpMode.loop(gamepad, dt);
        }
    }

    public synchronized Map<String, String> getTelemetry() {
        if (activeOpMode != null) {
            return activeOpMode.getTelemetry();
        }
        return new LinkedHashMap<>();
    }
}
