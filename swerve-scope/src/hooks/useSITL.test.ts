import { describe, expect, it } from 'vitest';
import { parseTelemetryFrame } from './useSITL';

describe('parseTelemetryFrame', () => {
  it('normalizes missing arrays and reports warnings', () => {
    const { frame, warnings } = parseTelemetryFrame({
      schemaVersion: 2,
      timestamp: 1.5,
      x: 3,
      y: 4,
      heading: 1,
      isMaintaining: true,
      isSnapping: false,
      snapTargetRad: 0.5,
      driveX: 0.2,
      driveY: -0.4,
      turn: 0.1,
      batteryVoltage: 12.2,
      loopTimeMs: 17,
    });

    expect(warnings).toEqual(expect.arrayContaining(['targets', 'actuals']));
    expect(frame.targets).toHaveLength(4);
    expect(frame.actuals).toHaveLength(4);
    expect(frame.schemaVersion).toBe(2);
    expect(frame.driveY).toBe(-0.4);
  });

  it('normalizes gamepad and structured logs', () => {
    const { frame, warnings } = parseTelemetryFrame({
      targets: [[1, 2]],
      actuals: [[3, 4]],
      gamepad: {
        lx: '0.5',
        ly: 0.25,
        rx: -0.75,
        ry: 0,
        buttons: [1, 0, true],
        dpad_up: 1,
      },
      logs: [
        { timestamp: 2, severity: 'warn', source: 'JAVA', message: 'warn message' },
        { timestamp: 3, severity: 'bogus', source: 'JAVA', message: 'ignored' },
      ],
    });

    expect(warnings).toEqual([]);
    expect(frame.gamepad?.lx).toBe(0.5);
    expect(frame.gamepad?.buttons).toEqual([true, false, true]);
    expect(frame.logs).toHaveLength(1);
    expect(frame.logs?.[0].source).toBe('JAVA');
  });

  it('accepts the authoritative Java SITL payload shape without warnings', () => {
    const javaPayload = {
      schemaVersion: 1,
      timestamp: 2.5,
      x: 1.2,
      y: -0.4,
      heading: 0.75,
      isMaintaining: false,
      isSnapping: true,
      snapTargetRad: 1.57,
      driveX: 0.3,
      driveY: 0.8,
      turn: -0.2,
      controllerMode: 'snap',
      drivetrainState: 'SNAPPING',
      targets: [
        [1.1, 0.1],
        [1.2, 0.2],
        [1.3, 0.3],
        [1.4, 0.4],
      ],
      actuals: [
        [1.0, 0.11],
        [1.1, 0.21],
        [1.2, 0.31],
        [1.3, 0.41],
      ],
      currentDraw: [2.1, 2.2, 2.3, 2.4],
      batteryVoltage: 12.18,
      loopTimeMs: 19.6,
      gamepad: {
        lx: 0.3,
        ly: 0.8,
        rx: -0.2,
        ry: 0,
        lt: 0,
        rt: 0,
        dpad_up: false,
        dpad_down: false,
        dpad_left: false,
        dpad_right: true,
      },
      observerVel: {
        vx: 1.02,
        vy: -0.15,
        omega: 0.5,
      },
      smootherState: {
        vx: 1.02,
        vy: -0.15,
        omega: 0.5,
      },
    };

    const { frame, warnings } = parseTelemetryFrame(javaPayload);

    expect(warnings).toEqual([]);
    expect(frame.schemaVersion).toBe(1);
    expect(frame.controllerMode).toBe('snap');
    expect(frame.drivetrainState).toBe('SNAPPING');
    expect(frame.targets[3]).toEqual([1.4, 0.4]);
    expect(frame.actuals[0]).toEqual([1.0, 0.11]);
    expect(frame.currentDraw).toEqual([2.1, 2.2, 2.3, 2.4]);
    expect(frame.gamepad?.dpad_right).toBe(true);
    expect(frame.observerVel?.vx).toBe(1.02);
    expect(frame.smootherState?.omega).toBe(0.5);
    expect(frame.logs).toEqual([]);
  });
});
