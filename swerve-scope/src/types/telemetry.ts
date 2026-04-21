// Core telemetry frame from the robot / SITL
export type LogSeverity = 'info' | 'warn' | 'error' | 'debug';

export interface StructuredLogEntry {
  timestamp: number;
  severity: LogSeverity;
  source: string;
  message: string;
}

export interface GamepadTelemetry {
  lx: number;
  ly: number;
  rx: number;
  ry: number;
  lt?: number;
  rt?: number;
  buttons?: boolean[];
  dpad_up?: boolean;
  dpad_down?: boolean;
  dpad_left?: boolean;
  dpad_right?: boolean;
}

export interface VelocityTelemetry {
  vx: number;
  vy: number;
  omega: number;
}

export interface SmootherTelemetry extends VelocityTelemetry {
  ax?: number;
  ay?: number;
  alpha?: number;
}

export interface TelemetryFrame {
  schemaVersion: number;
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
  controllerMode?: 'manual' | 'snap' | 'maintain';
  drivetrainState?: string;

  // Inputs
  driveX: number;
  driveY: number;
  turn: number;
  gamepad?: GamepadTelemetry;

  // Diagnostics
  batteryVoltage: number;
  loopTimeMs: number;
  currentDraw?: number[];
  observerVel?: VelocityTelemetry;
  smootherState?: SmootherTelemetry;
  logs?: StructuredLogEntry[];
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
export type RuntimeMode = 'local' | 'sitl';

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

export type WorkspacePaneId =
  | 'arena'
  | 'detailer'
  | 'graph'
  | 'inspector'
  | 'console'
  | 'joystick'
  | 'expressions';

export type WorkspaceSlotId =
  | 'primary'
  | 'secondary'
  | 'sidebarTop'
  | 'sidebarMiddle'
  | 'sidebarBottom';

export interface WorkspaceLayout {
  leftPct: number;
  slotHeights: {
    primary: number;
    secondary: number;
    sidebarTop: number;
    sidebarMiddle: number;
    sidebarBottom: number;
  };
  slots: Record<WorkspaceSlotId, WorkspacePaneId>;
}
