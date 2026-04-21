/** Gamepad state snapshot from the SITL bridge. */
export interface GamepadState {
    lx: number;   // Left stick X (-1 to 1)
    ly: number;   // Left stick Y (-1 to 1)
    rx: number;   // Right stick X (-1 to 1)
    ry: number;   // Right stick Y (-1 to 1)
    dpad_up: boolean;
    dpad_down: boolean;
    dpad_left: boolean;
    dpad_right: boolean;
    [key: string]: any;
}

/** MotionSmoother state for diagnostics. */
export interface SmootherSnapshot {
    vx: number;
    vy: number;
    omega: number;
    ax: number;
    ay: number;
    aOmega: number;
}

/** Chassis velocity from the velocity observer. */
export interface ObserverVelocity {
    vx: number;
    vy: number;
    omega: number;
}

/**
 * A single telemetry frame streamed from the SITL server at 50Hz.
 * Each field maps to a plottable data source in the Line Graph and Statistics tabs.
 */
export interface TelemetryEntry {
    /** Monotonic timestamp in seconds since session start. */
    timestamp: number;

    /** Robot field position X (meters). */
    x: number;
    /** Robot field position Y (meters). */
    y: number;
    /** Robot heading (radians). */
    heading: number;

    /** Target module states [speed m/s, angle rad] × 4: [FL, FR, RR, RL]. */
    targets: number[][];
    /** Actual/measured module states [speed m/s, angle rad] × 4: [FL, FR, RR, RL]. */
    actuals: number[][];

    /** SwerveController heading retention state. */
    isMaintaining: boolean;
    /** SwerveController cardinal snap state. */
    isSnapping: boolean;

    /** Data source identifier ('SITL', 'LOCAL', 'LOG'). */
    mode: string;

    /** Gamepad input snapshot (Phase 2D). */
    gamepad?: GamepadState;

    /** MotionSmoother intermediate state (Phase 2B graphs). */
    smootherState?: SmootherSnapshot;

    /** Velocity observer output (Phase 2B graphs). */
    observerVel?: ObserverVelocity;

    /** Per-module current draw in Amps [FL, FR, RR, RL] (Phase 3). */
    currentDraw?: number[];

    /** Battery voltage (Phase 3). */
    batteryVoltage?: number;

    /** Actual loop time in ms (Phase 3). */
    loopTimeMs?: number;

    /** Drivetrain state machine state (Phase 3). */
    drivetrainState?: string;

    /** Console log messages for this frame. */
    logMessages?: LogMessage[];

    /** Catch-all for future fields. */
    [key: string]: any;
}

/** Console log message structure. */
export interface LogMessage {
    level: 'DEBUG' | 'INFO' | 'WARNING' | 'ERROR';
    tag: string;
    message: string;
    timestamp: number;
}

/**
 * All plottable field paths available in a TelemetryEntry.
 * Used by the Line Graph field selector and Statistics tab.
 */
export const TELEMETRY_FIELDS = {
    pose: [
        { key: 'x', label: 'Field X', unit: 'm' },
        { key: 'y', label: 'Field Y', unit: 'm' },
        { key: 'heading', label: 'Heading', unit: 'rad' },
    ],
    modules: ['FL', 'FR', 'RR', 'RL'].flatMap((name, i) => [
        { key: `targets.${i}.0`, label: `${name} Target Speed`, unit: 'm/s', extract: (e: TelemetryEntry) => e.targets?.[i]?.[0] ?? 0 },
        { key: `targets.${i}.1`, label: `${name} Target Angle`, unit: 'rad', extract: (e: TelemetryEntry) => e.targets?.[i]?.[1] ?? 0 },
        { key: `actuals.${i}.0`, label: `${name} Actual Speed`, unit: 'm/s', extract: (e: TelemetryEntry) => e.actuals?.[i]?.[0] ?? 0 },
        { key: `actuals.${i}.1`, label: `${name} Actual Angle`, unit: 'rad', extract: (e: TelemetryEntry) => e.actuals?.[i]?.[1] ?? 0 },
    ]),
    gamepad: [
        { key: 'gamepad.lx', label: 'Gamepad LX', unit: '', extract: (e: TelemetryEntry) => e.gamepad?.lx ?? 0 },
        { key: 'gamepad.ly', label: 'Gamepad LY', unit: '', extract: (e: TelemetryEntry) => e.gamepad?.ly ?? 0 },
        { key: 'gamepad.rx', label: 'Gamepad RX', unit: '', extract: (e: TelemetryEntry) => e.gamepad?.rx ?? 0 },
    ],
    observer: [
        { key: 'observerVel.vx', label: 'Observer Vx', unit: 'm/s', extract: (e: TelemetryEntry) => e.observerVel?.vx ?? 0 },
        { key: 'observerVel.vy', label: 'Observer Vy', unit: 'm/s', extract: (e: TelemetryEntry) => e.observerVel?.vy ?? 0 },
        { key: 'observerVel.omega', label: 'Observer ω', unit: 'rad/s', extract: (e: TelemetryEntry) => e.observerVel?.omega ?? 0 },
    ],
    diagnostics: [
        { key: 'batteryVoltage', label: 'Battery', unit: 'V', extract: (e: TelemetryEntry) => e.batteryVoltage ?? 0 },
        { key: 'loopTimeMs', label: 'Loop Time', unit: 'ms', extract: (e: TelemetryEntry) => e.loopTimeMs ?? 0 },
        ...['FL', 'FR', 'RR', 'RL'].map((name, i) => ({
            key: `currentDraw.${i}`, label: `${name} Current`, unit: 'A', extract: (e: TelemetryEntry) => e.currentDraw?.[i] ?? 0
        })),
    ],
} as const;

/**
 * Resolve a dot-path field key to a value in a TelemetryEntry.
 */
export function resolveTelemetryField(entry: TelemetryEntry, key: string): number {
    // Check all field definitions for an extract function
    const allFields = [
        ...TELEMETRY_FIELDS.pose,
        ...TELEMETRY_FIELDS.modules,
        ...TELEMETRY_FIELDS.gamepad,
        ...TELEMETRY_FIELDS.observer,
        ...TELEMETRY_FIELDS.diagnostics,
    ];
    const fieldDef = allFields.find(f => f.key === key);
    if (fieldDef && 'extract' in fieldDef && typeof fieldDef.extract === 'function') {
        return (fieldDef as any).extract(entry);
    }
    // Fallback: simple top-level key
    const val = (entry as any)[key];
    return typeof val === 'number' ? val : 0;
}
