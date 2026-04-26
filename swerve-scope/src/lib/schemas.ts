export type TransportSource = 'sim' | 'robot' | 'replay';

export interface TelemetryFrame {
  schemaVersion: 2;
  timestampMs: number;
  source: TransportSource;
  data: Record<string, unknown>;
  pose?: {
    xMeters: number;
    yMeters: number;
    headingRadians: number;
    headingDegrees?: number;
  };
  actualVelocity?: {
    x: number;
    y: number;
    omega: number;
    linearSpeed?: number;
  };
  modules?: Array<Record<string, unknown>>;
  opModeState?: string;
  activeOpMode?: string;
  drivetrainState?: string;
  configSnapshot?: Record<string, unknown>;
  markers?: string[];
}

export interface SessionManifest {
  schemaVersion: 2;
  id: string;
  name: string;
  source: TransportSource;
  startedAtMs: number;
  endedAtMs: number;
  frameCount: number;
  opMode?: string;
  routeName?: string;
  tags: string[];
  notes?: string;
}

export interface SavedSession {
  manifest: SessionManifest;
  frames: TelemetryFrame[];
}

export interface PanelLayoutItem {
  id: string;
  visible: boolean;
  size?: number;
}

export interface PanelLayout {
  schemaVersion: 2;
  name: string;
  mode: 'dashboard' | 'route-builder';
  items: PanelLayoutItem[];
  updatedAtMs: number;
}

export interface RouteWorkspace {
  schemaVersion: 2;
  routeName: string;
  selectedAutoFile?: string;
  showRobotSilhouettes: boolean;
  fieldImageOpacity: number;
  robotSizeIn: number;
  updatedAtMs: number;
}

export function normalizeTelemetryFrame(
  data: Record<string, unknown>,
  source: TransportSource,
  timestampMs: number,
  configSnapshot?: Record<string, unknown>,
): TelemetryFrame {
  return {
    schemaVersion: 2,
    timestampMs,
    source,
    data,
    pose: asRecord(data.pose),
    actualVelocity: asRecord(data.actualVelocity),
    modules: Array.isArray(data.modules) ? (data.modules as Array<Record<string, unknown>>) : undefined,
    opModeState: typeof data.opModeState === 'string' ? data.opModeState : undefined,
    activeOpMode: typeof data.activeOpMode === 'string' ? data.activeOpMode : undefined,
    drivetrainState: typeof data.drivetrainState === 'string' ? data.drivetrainState : undefined,
    configSnapshot,
    markers: buildMarkers(data),
  };
}

export function flattenTelemetry(input: Record<string, unknown>, prefix = ''): Record<string, string> {
  const output: Record<string, string> = {};
  for (const [key, value] of Object.entries(input)) {
    const fullKey = prefix ? `${prefix}.${key}` : key;
    if (value && typeof value === 'object' && !Array.isArray(value)) {
      Object.assign(output, flattenTelemetry(value as Record<string, unknown>, fullKey));
    } else if (Array.isArray(value)) {
      output[fullKey] = JSON.stringify(value);
    } else {
      output[fullKey] = String(value);
    }
  }
  return output;
}

export function flattenTelemetryNumbers(input: Record<string, unknown>, prefix = ''): Record<string, number> {
  const output: Record<string, number> = {};
  for (const [key, value] of Object.entries(input)) {
    const fullKey = prefix ? `${prefix}.${key}` : key;
    if (typeof value === 'number' && Number.isFinite(value)) {
      output[fullKey] = value;
    } else if (value && typeof value === 'object' && !Array.isArray(value)) {
      Object.assign(output, flattenTelemetryNumbers(value as Record<string, unknown>, fullKey));
    }
  }
  return output;
}

function asRecord<T>(value: unknown): T | undefined {
  return value && typeof value === 'object' && !Array.isArray(value) ? (value as T) : undefined;
}

function buildMarkers(data: Record<string, unknown>): string[] {
  const markers: string[] = [];
  if (data.opModeState === 'INITIALIZED') markers.push('INIT');
  if (data.opModeState === 'RUNNING') markers.push('RUNNING');
  if (data.opModeState === 'INIT_FAILED') markers.push('INIT_FAILED');
  if (data.snapping === true) markers.push('SNAP');
  if (data.headingHold === true) markers.push('HOLD');
  return markers;
}
