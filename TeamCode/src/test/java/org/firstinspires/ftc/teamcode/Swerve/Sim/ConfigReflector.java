package org.firstinspires.ftc.teamcode.Swerve.Sim;

import org.firstinspires.ftc.teamcode.Swerve.Core.SwerveConfig;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Uses Java reflection to read/write public static fields from SwerveConfig at runtime.
 * This enables the dashboard to display and edit config values live.
 */
public class ConfigReflector {

    /**
     * Reads all public static fields from SwerveConfig and returns them as a map.
     * Supports: double, int, boolean, double[], boolean[], String.
     */
    public static Map<String, Object> readConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        for (Field f : SwerveConfig.class.getDeclaredFields()) {
            int mods = f.getModifiers();
            if (Modifier.isPublic(mods) && Modifier.isStatic(mods)) {
                try {
                    Class<?> type = f.getType();
                    // Skip enum and complex types
                    if (type == double.class || type == int.class || type == boolean.class
                            || type == long.class || type == String.class
                            || type == double[].class || type == boolean[].class) {
                        config.put(f.getName(), f.get(null));
                    }
                } catch (IllegalAccessException e) {
                    // skip inaccessible fields
                }
            }
        }
        return config;
    }

    /**
     * Writes a single config field by name. Coerces the value to the correct type.
     * @return true if the write succeeded
     */
    public static boolean writeConfig(String key, Object value) {
        try {
            Field f = SwerveConfig.class.getDeclaredField(key);
            int mods = f.getModifiers();
            if (!Modifier.isPublic(mods) || !Modifier.isStatic(mods)) return false;

            Class<?> type = f.getType();
            if (type == double.class) {
                f.setDouble(null, ((Number) value).doubleValue());
            } else if (type == int.class) {
                f.setInt(null, ((Number) value).intValue());
            } else if (type == long.class) {
                f.setLong(null, ((Number) value).longValue());
            } else if (type == boolean.class) {
                f.setBoolean(null, (Boolean) value);
            } else if (type == String.class) {
                f.set(null, String.valueOf(value));
            } else {
                return false;
            }
            return true;
        } catch (Exception e) {
            System.err.println("ConfigReflector: failed to write " + key + " = " + value);
            e.printStackTrace();
            return false;
        }
    }
}
