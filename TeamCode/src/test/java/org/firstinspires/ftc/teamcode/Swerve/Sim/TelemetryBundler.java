package org.firstinspires.ftc.teamcode.Swerve.Sim;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manages grouped telemetry data.
 */
public class TelemetryBundler {
    private final Map<String, Map<String, String>> bundles = new LinkedHashMap<>();

    public void add(String bundleName, String key, Object value) {
        bundles.computeIfAbsent(bundleName, k -> new LinkedHashMap<>())
               .put(key, String.valueOf(value));
    }

    public void add(String key, Object value) {
        add("General", key, value);
    }

    public void clear() {
        bundles.clear();
    }

    public Map<String, Map<String, String>> getBundles() {
        return bundles;
    }
}
