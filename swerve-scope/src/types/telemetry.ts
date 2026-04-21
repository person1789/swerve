// Core telemetry frame from the robot / SITL
export interface TelemetryFrame {
  timestamp: number;          // seconds since session start
  // Odometry
  x: number;                  // field X in meters
  y: number;                  // field Y in meters
  heading: number;            // radians, field-relative

  // Module states [FL, FR, RR, RL]
  targets: [number, number][]; // [speedMps, angleRad] per module
  actuals: [number, number][]; // [speedMps, angleRad] per module (measured)

  // Controller state
  isMaintaining: boolean;
  isSnapping: boolean;
  snapTargetRad: number;

  // Inputs
  driveX: number;
  driveY: number;
  turn: number;

  // Diagnostics
  batteryVoltage: number;
  loopTimeMs: number;
}

// Expression result
export interface DerivedField {
  id: string;
  name: string;
  expr: string;
  color: string;
  unit: string;
}

// Panel layout tile IDs
export type TileId =
  | 'arena'
  | 'modules'
  | 'scopes'
  | 'inspector'
  | 'joystick'
  | 'console';

export interface PanelDef {
  id: TileId;
  label: string;
  icon: string;
}

// Connection status
export type ConnectionStatus = 'disconnected' | 'connecting' | 'connected';

// Playback state
export interface PlaybackState {
  isLive: boolean;
  isPlaying: boolean;
  scrubIndex: number;
  playbackRate: number;
}

// Gamepad snapshot (for joystick visualizer)
export interface GamepadSnapshot {
  lx: number; ly: number;
  rx: number; ry: number;
  lt: number; rt: number;
  buttons: boolean[];
}
