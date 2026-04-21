import type {
  AnalysisRange,
  DerivedField,
  SessionAnalysisState,
  SessionPlaybackState,
  SessionUiState,
  SwerveScopeSessionFile,
  TelemetryFrame,
  WorkspaceLayout,
} from '../types/telemetry';

interface SessionInput {
  frames: TelemetryFrame[];
  activeFields: string[];
  customFields: DerivedField[];
  workspace: WorkspaceLayout;
  playback: SessionPlaybackState;
  ui: SessionUiState;
  analysis: SessionAnalysisState;
}

function isObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null;
}

function isTelemetryFrame(value: unknown): value is TelemetryFrame {
  return isObject(value)
    && typeof value.schemaVersion === 'number'
    && typeof value.timestamp === 'number'
    && typeof value.x === 'number'
    && typeof value.y === 'number'
    && typeof value.heading === 'number'
    && Array.isArray(value.targets)
    && Array.isArray(value.actuals)
    && typeof value.isMaintaining === 'boolean'
    && typeof value.isSnapping === 'boolean'
    && typeof value.snapTargetRad === 'number'
    && typeof value.driveX === 'number'
    && typeof value.driveY === 'number'
    && typeof value.turn === 'number'
    && typeof value.batteryVoltage === 'number'
    && typeof value.loopTimeMs === 'number';
}

function isDerivedField(value: unknown): value is DerivedField {
  return isObject(value)
    && typeof value.id === 'string'
    && typeof value.name === 'string'
    && typeof value.expr === 'string'
    && typeof value.color === 'string'
    && typeof value.unit === 'string';
}

function isWorkspaceLayout(value: unknown): value is WorkspaceLayout {
  if (!isObject(value) || !isObject(value.slotHeights) || !isObject(value.slots)) return false;
  return typeof value.leftPct === 'number'
    && typeof value.slotHeights.primary === 'number'
    && typeof value.slotHeights.secondary === 'number'
    && typeof value.slotHeights.sidebarTop === 'number'
    && typeof value.slotHeights.sidebarMiddle === 'number'
    && typeof value.slotHeights.sidebarBottom === 'number'
    && typeof value.slots.primary === 'string'
    && typeof value.slots.secondary === 'string'
    && typeof value.slots.sidebarTop === 'string'
    && typeof value.slots.sidebarMiddle === 'string'
    && typeof value.slots.sidebarBottom === 'string';
}

function normalizeRange(value: unknown): AnalysisRange | null {
  if (!isObject(value)) return null;
  if (typeof value.startIndex !== 'number' || typeof value.endIndex !== 'number') return null;
  return {
    startIndex: Math.min(value.startIndex, value.endIndex),
    endIndex: Math.max(value.startIndex, value.endIndex),
  };
}

function normalizeAnalysis(value: unknown): SessionAnalysisState {
  if (!isObject(value)) {
    return { range: null, bookmarks: [] };
  }
  return {
    range: normalizeRange(value.range),
    bookmarks: Array.isArray(value.bookmarks)
      ? value.bookmarks.filter((item): item is number => typeof item === 'number')
      : [],
  };
}

export function createSessionFile(input: SessionInput): SwerveScopeSessionFile {
  return {
    app: 'SwerveScope',
    version: 1,
    exportedAt: new Date().toISOString(),
    frames: input.frames,
    activeFields: input.activeFields,
    customFields: input.customFields,
    workspace: input.workspace,
    playback: input.playback,
    ui: input.ui,
    analysis: input.analysis,
  };
}

export function serializeSessionFile(input: SessionInput): string {
  return JSON.stringify(createSessionFile(input), null, 2);
}

export function parseSessionFile(json: string): SwerveScopeSessionFile {
  let parsed: unknown;
  try {
    parsed = JSON.parse(json);
  } catch {
    throw new Error('Session file is not valid JSON.');
  }

  if (!isObject(parsed)) throw new Error('Session file must contain an object.');
  if (parsed.app !== 'SwerveScope' || parsed.version !== 1) {
    throw new Error('Unsupported session file format.');
  }
  if (!Array.isArray(parsed.frames) || !parsed.frames.every(isTelemetryFrame)) {
    throw new Error('Session file has invalid telemetry frames.');
  }
  if (!Array.isArray(parsed.activeFields) || !parsed.activeFields.every(v => typeof v === 'string')) {
    throw new Error('Session file has invalid active fields.');
  }
  if (!Array.isArray(parsed.customFields) || !parsed.customFields.every(isDerivedField)) {
    throw new Error('Session file has invalid custom fields.');
  }
  if (!isWorkspaceLayout(parsed.workspace)) {
    throw new Error('Session file has invalid workspace layout.');
  }
  if (!isObject(parsed.playback) || typeof parsed.playback.isLive !== 'boolean' || typeof parsed.playback.scrubIndex !== 'number') {
    throw new Error('Session file has invalid playback state.');
  }
  if (!isObject(parsed.ui) || typeof parsed.ui.showVectors !== 'boolean' || typeof parsed.ui.showTrail !== 'boolean') {
    throw new Error('Session file has invalid UI state.');
  }
  if (typeof parsed.exportedAt !== 'string') {
    throw new Error('Session file is missing export metadata.');
  }

  return {
    app: 'SwerveScope',
    version: 1,
    exportedAt: parsed.exportedAt,
    frames: parsed.frames,
    activeFields: parsed.activeFields,
    customFields: parsed.customFields,
    workspace: parsed.workspace,
    playback: parsed.playback as unknown as SessionPlaybackState,
    ui: parsed.ui as unknown as SessionUiState,
    analysis: normalizeAnalysis(parsed.analysis),
  };
}
