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
  timestamp: number;
  x: number;
  y: number;
  heading: number;
  targets: [number, number][];
  actuals: [number, number][];
  isMaintaining: boolean;
  isSnapping: boolean;
  snapTargetRad: number;
  controllerMode?: 'manual' | 'snap' | 'maintain';
  drivetrainState?: string;
  driveX: number;
  driveY: number;
  turn: number;
  gamepad?: GamepadTelemetry;
  batteryVoltage: number;
  loopTimeMs: number;
  currentDraw?: number[];
  observerVel?: VelocityTelemetry;
  smootherState?: SmootherTelemetry;
  logs?: StructuredLogEntry[];
}

export interface DerivedField {
  id: string;
  name: string;
  expr: string;
  color: string;
  unit: string;
}

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

export type ConnectionStatus = 'disconnected' | 'connecting' | 'connected';
export type RuntimeMode = 'local' | 'sitl';

export interface PlaybackState {
  isLive: boolean;
  isPlaying: boolean;
  scrubIndex: number;
  playbackRate: number;
}

export interface GamepadSnapshot {
  lx: number;
  ly: number;
  rx: number;
  ry: number;
  lt: number;
  rt: number;
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

export interface AnalysisRange {
  startIndex: number;
  endIndex: number;
}

export interface SessionPlaybackState {
  isLive: boolean;
  scrubIndex: number;
}

export interface SessionUiState {
  showVectors: boolean;
  showTrail: boolean;
}

export interface SessionAnalysisState {
  range: AnalysisRange | null;
  bookmarks: number[];
}

export interface SwerveScopeSessionFile {
  app: 'SwerveScope';
  version: 1;
  exportedAt: string;
  frames: TelemetryFrame[];
  activeFields: string[];
  customFields: DerivedField[];
  workspace: WorkspaceLayout;
  playback: SessionPlaybackState;
  ui: SessionUiState;
  analysis?: SessionAnalysisState;
}
