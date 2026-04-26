package org.firstinspires.ftc.teamcode.Swerve.Sim;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    public enum OpModeState { STOPPED, INITIALIZED, INIT_FAILED, RUNNING }

    private final Map<String, Supplier<SimOpMode>> registry = new LinkedHashMap<>();
    private SimOpMode activeOpMode;
    private String activeOpModeName = "";
    private OpModeState state = OpModeState.STOPPED;
    private SwerveSimulator simulator;

    public SimOpModeRegistry(SwerveSimulator simulator) {
        this.simulator = simulator;
        register("MainTeleOp", SimMainTeleOp::new);
        register("SwerveSystemCheck", SimSystemCheck::new);
        registerTaggedPedroAutos();
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
        state = didInitFail(activeOpMode) ? OpModeState.INIT_FAILED : OpModeState.INITIALIZED;
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

    public synchronized Map<String, Map<String, String>> getTelemetry() {
        if (activeOpMode != null) {
            return activeOpMode.getTelemetry();
        }
        return new LinkedHashMap<>();
    }

    public synchronized List<String> reloadTaggedPedroAutos() {
        String selectedName = activeOpModeName;
        boolean shouldRestoreSelection = !selectedName.isEmpty();
        stop();

        List<String> preservedBuiltIns = new ArrayList<>();
        preservedBuiltIns.add("MainTeleOp");
        preservedBuiltIns.add("SwerveSystemCheck");

        registry.keySet().removeIf(name -> !preservedBuiltIns.contains(name));
        registerTaggedPedroAutos();

        if (shouldRestoreSelection && registry.containsKey(selectedName)) {
            activeOpModeName = selectedName;
        } else if (!registry.containsKey(activeOpModeName)) {
            activeOpModeName = "";
        }

        return getAvailableOpModes();
    }

    private void registerTaggedPedroAutos() {
        Path pedroDir = resolvePedroDir();

        if (!Files.isDirectory(pedroDir)) {
            return;
        }

        try (java.util.stream.Stream<Path> stream = Files.list(pedroDir)) {
            stream.filter(path -> path.getFileName().toString().endsWith(".java"))
                    .forEach(this::registerTaggedPedroAutoFile);
        } catch (IOException ignored) {
        }
    }

    private void registerTaggedPedroAutoFile(Path file) {
        try {
            String source = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            if (!source.contains("@sim")) {
                return;
            }

            String simpleName = file.getFileName().toString().replace(".java", "");
            final String className = "org.firstinspires.ftc.teamcode.pedroPathing." + simpleName;
            final String displayName = SimPedroTaggedAuto.readDisplayName(className, simpleName);

            register(displayName, new Supplier<SimOpMode>() {
                @Override
                public SimOpMode get() {
                    return new SimPedroTaggedAuto(className, displayName, file);
                }
            });
        } catch (IOException ignored) {
        }
    }

    private static Path resolvePedroDir() {
        Path cwd = Paths.get("").toAbsolutePath().normalize();
        Path teamCodeRoot = cwd.getFileName() != null && "TeamCode".equals(cwd.getFileName().toString())
                ? cwd
                : cwd.resolve("TeamCode");

        return teamCodeRoot
                .resolve("src")
                .resolve("main")
                .resolve("java")
                .resolve("org")
                .resolve("firstinspires")
                .resolve("ftc")
                .resolve("teamcode")
                .resolve("pedroPathing")
                .normalize();
    }

    private static boolean didInitFail(SimOpMode opMode) {
        if (opMode == null) {
            return true;
        }

        Map<String, Map<String, String>> telemetry = opMode.getTelemetry();
        if (telemetry == null) {
            return false;
        }

        Map<String, String> statusBundle = telemetry.get("Status");
        if (statusBundle == null) {
            return false;
        }

        return "Init Failed".equals(statusBundle.get("Status"));
    }
}
