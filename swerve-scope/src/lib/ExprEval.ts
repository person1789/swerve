// Expression evaluator — safe subset of JS for derived telemetry fields
import type { TelemetryFrame } from '../types/telemetry';

export function evalExpr(expr: string, frame: TelemetryFrame): number {
  try {
    // Build a safe context from the frame
    const ctx: Record<string, unknown> = {
      t: frame.timestamp,
      x: frame.x,
      y: frame.y,
      heading: frame.heading,
      headingDeg: frame.heading * (180 / Math.PI),
      speed: Math.hypot(
        frame.actuals.reduce((s, m) => s + m[0] * Math.cos(m[1]), 0) / 4,
        frame.actuals.reduce((s, m) => s + m[0] * Math.sin(m[1]), 0) / 4,
      ),
      battery: frame.batteryVoltage,
      loopMs: frame.loopTimeMs,
      driveX: frame.driveX,
      driveY: frame.driveY,
      turn: frame.turn,
      // Per-module accessors
      m0_speed: frame.actuals[0]?.[0] ?? 0,
      m1_speed: frame.actuals[1]?.[0] ?? 0,
      m2_speed: frame.actuals[2]?.[0] ?? 0,
      m3_speed: frame.actuals[3]?.[0] ?? 0,
      m0_angle: frame.actuals[0]?.[1] ?? 0,
      m1_angle: frame.actuals[1]?.[1] ?? 0,
      m2_angle: frame.actuals[2]?.[1] ?? 0,
      m3_angle: frame.actuals[3]?.[1] ?? 0,
      // Math helpers
      abs: Math.abs, sqrt: Math.sqrt, sin: Math.sin, cos: Math.cos,
      atan2: Math.atan2, hypot: Math.hypot, PI: Math.PI,
    };

    // Build function body from variable context
    const keys = Object.keys(ctx);
    const vals = keys.map(k => ctx[k]);

    // eslint-disable-next-line @typescript-eslint/no-implied-eval
    const fn = new Function(...keys, `"use strict"; return (${expr});`);
    const result = fn(...vals);
    return typeof result === 'number' && isFinite(result) ? result : 0;
  } catch {
    return NaN;
  }
}

export const BUILTIN_EXPRS = [
  { id: 'speed',      name: 'Chassis Speed',     expr: 'speed',                     unit: 'm/s',  color: '#10b981' },
  { id: 'hdg_deg',    name: 'Heading (°)',        expr: 'headingDeg',                unit: '°',    color: '#0ea5e9' },
  { id: 'battery',    name: 'Battery Voltage',    expr: 'battery',                   unit: 'V',    color: '#f59e0b' },
  { id: 'loop_ms',    name: 'Loop Time',          expr: 'loopMs',                    unit: 'ms',   color: '#a855f7' },
  { id: 'drive_mag',  name: 'Drive Magnitude',    expr: 'hypot(driveX, driveY)',     unit: '',     color: '#ef4444' },
  { id: 'm0_spd',     name: 'FL Speed',           expr: 'm0_speed',                  unit: 'm/s',  color: '#f43f5e' },
  { id: 'm1_spd',     name: 'FR Speed',           expr: 'm1_speed',                  unit: 'm/s',  color: '#fb923c' },
  { id: 'm2_spd',     name: 'RR Speed',           expr: 'm2_speed',                  unit: 'm/s',  color: '#facc15' },
  { id: 'm3_spd',     name: 'RL Speed',           expr: 'm3_speed',                  unit: 'm/s',  color: '#4ade80' },
];
