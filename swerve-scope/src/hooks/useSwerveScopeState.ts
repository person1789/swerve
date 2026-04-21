import { useCallback, useEffect, useMemo, useState } from 'react';
import type {
  AnalysisRange,
  DerivedField,
  SessionUiState,
  SwerveScopeSessionFile,
  WorkspaceLayout,
  WorkspacePaneId,
  WorkspaceSlotId,
} from '../types/telemetry';
import { useGamepad } from './useGamepad';
import { useSITL } from './useSITL';
import { telemetryStore } from '../store/telemetryStore';
import { usePlaybackState, useHistoryLength } from './useTelemetry';
import { createSessionFile } from '../lib/sessionPersistence';

const DEFAULT_ACTIVE_FIELDS = [
  'speed', 'hdg_deg', 'battery', 'loop_ms',
  'total_current', 'm0_spd', 'm1_spd', 'm2_spd', 'm3_spd',
];

const DEFAULT_LAYOUT: WorkspaceLayout = {
  leftPct: 62,
  slotHeights: {
    primary: 58,
    secondary: 42,
    sidebarTop: 34,
    sidebarMiddle: 33,
    sidebarBottom: 33,
  },
  slots: {
    primary: 'arena',
    secondary: 'graph',
    sidebarTop: 'detailer',
    sidebarMiddle: 'inspector',
    sidebarBottom: 'console',
  },
};

function loadJson<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key);
    return raw ? (JSON.parse(raw) as T) : fallback;
  } catch {
    return fallback;
  }
}

function normalizeLayout(input: WorkspaceLayout): WorkspaceLayout {
  return {
    leftPct: Math.max(30, Math.min(75, input.leftPct ?? DEFAULT_LAYOUT.leftPct)),
    slotHeights: {
      primary: Math.max(20, Math.min(80, input.slotHeights?.primary ?? DEFAULT_LAYOUT.slotHeights.primary)),
      secondary: Math.max(20, Math.min(80, input.slotHeights?.secondary ?? DEFAULT_LAYOUT.slotHeights.secondary)),
      sidebarTop: Math.max(15, Math.min(70, input.slotHeights?.sidebarTop ?? DEFAULT_LAYOUT.slotHeights.sidebarTop)),
      sidebarMiddle: Math.max(15, Math.min(70, input.slotHeights?.sidebarMiddle ?? DEFAULT_LAYOUT.slotHeights.sidebarMiddle)),
      sidebarBottom: Math.max(15, Math.min(70, input.slotHeights?.sidebarBottom ?? DEFAULT_LAYOUT.slotHeights.sidebarBottom)),
    },
    slots: {
      primary: input.slots?.primary ?? DEFAULT_LAYOUT.slots.primary,
      secondary: input.slots?.secondary ?? DEFAULT_LAYOUT.slots.secondary,
      sidebarTop: input.slots?.sidebarTop ?? DEFAULT_LAYOUT.slots.sidebarTop,
      sidebarMiddle: input.slots?.sidebarMiddle ?? DEFAULT_LAYOUT.slots.sidebarMiddle,
      sidebarBottom: input.slots?.sidebarBottom ?? DEFAULT_LAYOUT.slots.sidebarBottom,
    },
  };
}

function normalizeRange(range: AnalysisRange | null, historyLength: number): AnalysisRange | null {
  if (!range || historyLength === 0) return null;
  const startIndex = Math.max(0, Math.min(range.startIndex, historyLength - 1));
  const endIndex = Math.max(0, Math.min(range.endIndex, historyLength - 1));
  return {
    startIndex: Math.min(startIndex, endIndex),
    endIndex: Math.max(startIndex, endIndex),
  };
}

export function useSwerveScopeState() {
  const { scrubIndex, isLive } = usePlaybackState();
  const historyLength = useHistoryLength();

  const [showVectors, setShowVectors] = useState(true);
  const [showTrail, setShowTrail] = useState(true);
  const [isPlaying, setIsPlaying] = useState(false);
  const [playbackRate, setPlaybackRate] = useState(1);
  const [sessionError, setSessionError] = useState<string | null>(null);

  const [customFields, setCustomFields] = useState<DerivedField[]>(() =>
    loadJson('scope_custom_fields', []));
  const [activeFields, setActiveFields] = useState<string[]>(() =>
    loadJson('scope_active_fields', DEFAULT_ACTIVE_FIELDS));
  const [workspace, setWorkspace] = useState<WorkspaceLayout>(() =>
    normalizeLayout(loadJson('scope_workspace_layout', DEFAULT_LAYOUT)));
  const [analysisRange, setAnalysisRangeState] = useState<AnalysisRange | null>(() =>
    loadJson('scope_analysis_range', null));
  const [bookmarks, setBookmarks] = useState<number[]>(() =>
    loadJson('scope_bookmarks', []));

  const handleFrame = useCallback((frame: Parameters<typeof telemetryStore.push>[0]) => {
    telemetryStore.push(frame);
  }, []);

  const { status, runtimeMode, sendInput, schemaMismatch, resetSimulation } = useSITL(handleFrame);
  useGamepad(sendInput, true);

  useEffect(() => {
    if (isLive || !isPlaying) return;
    const id = window.setInterval(() => {
      const idx = telemetryStore.getScrubIndex();
      const length = telemetryStore.getLength();
      if (idx >= length - 1) {
        setIsPlaying(false);
        return;
      }
      telemetryStore.stepBy(1);
    }, Math.max(5, 20 / playbackRate));
    return () => window.clearInterval(id);
  }, [isLive, isPlaying, playbackRate]);

  useEffect(() => {
    localStorage.setItem('scope_custom_fields', JSON.stringify(customFields));
  }, [customFields]);

  useEffect(() => {
    localStorage.setItem('scope_active_fields', JSON.stringify(activeFields));
  }, [activeFields]);

  useEffect(() => {
    localStorage.setItem('scope_workspace_layout', JSON.stringify(workspace));
  }, [workspace]);

  useEffect(() => {
    localStorage.setItem('scope_analysis_range', JSON.stringify(analysisRange));
  }, [analysisRange]);

  useEffect(() => {
    localStorage.setItem('scope_bookmarks', JSON.stringify(bookmarks));
  }, [bookmarks]);

  useEffect(() => {
    setAnalysisRangeState(prev => normalizeRange(prev, historyLength));
    setBookmarks(prev =>
      Array.from(new Set(prev.filter(index => index >= 0 && index < historyLength))).sort((a, b) => a - b),
    );
  }, [historyLength]);

  const setPaneForSlot = useCallback((slot: WorkspaceSlotId, pane: WorkspacePaneId) => {
    setWorkspace(prev => ({ ...prev, slots: { ...prev.slots, [slot]: pane } }));
  }, []);

  const swapSlots = useCallback((from: WorkspaceSlotId, to: WorkspaceSlotId) => {
    if (from === to) return;
    setWorkspace(prev => ({
      ...prev,
      slots: { ...prev.slots, [from]: prev.slots[to], [to]: prev.slots[from] },
    }));
  }, []);

  const setLeftPct = useCallback((next: number) => {
    setWorkspace(prev => ({ ...prev, leftPct: Math.max(30, Math.min(75, next)) }));
  }, []);

  const setSlotHeight = useCallback(
    (slot: 'primary' | 'secondary' | 'sidebarTop' | 'sidebarMiddle' | 'sidebarBottom', next: number) => {
      setWorkspace(prev => ({
        ...prev,
        slotHeights: { ...prev.slotHeights, [slot]: Math.max(15, Math.min(80, next)) },
      }));
    }, []);

  const setAnalysisRange = useCallback((range: AnalysisRange | null) => {
    setAnalysisRangeState(normalizeRange(range, telemetryStore.getLength()));
  }, []);

  const setRangeStart = useCallback((index: number) => {
    setAnalysisRangeState(prev => {
      const nextEnd = prev?.endIndex ?? index;
      return normalizeRange({ startIndex: index, endIndex: nextEnd }, telemetryStore.getLength());
    });
  }, []);

  const setRangeEnd = useCallback((index: number) => {
    setAnalysisRangeState(prev => {
      const nextStart = prev?.startIndex ?? index;
      return normalizeRange({ startIndex: nextStart, endIndex: index }, telemetryStore.getLength());
    });
  }, []);

  const clearAnalysisRange = useCallback(() => {
    setAnalysisRangeState(null);
  }, []);

  const toggleBookmark = useCallback((index: number) => {
    setBookmarks(prev => {
      if (prev.includes(index)) {
        return prev.filter(value => value !== index);
      }
      return [...prev, index].sort((a, b) => a - b);
    });
  }, []);

  const jumpToNearestBookmark = useCallback((direction: -1 | 1) => {
    if (bookmarks.length === 0) return;
    const sorted = [...bookmarks].sort((a, b) => a - b);
    const current = telemetryStore.getScrubIndex();
    const next = direction > 0
      ? sorted.find(index => index > current) ?? sorted[0]
      : [...sorted].reverse().find(index => index < current) ?? sorted[sorted.length - 1];
    telemetryStore.seek(next);
  }, [bookmarks]);

  const history = {
    push: telemetryStore.push.bind(telemetryStore),
    seek: telemetryStore.seek.bind(telemetryStore),
    stepBy: telemetryStore.stepBy.bind(telemetryStore),
    snapToLive: telemetryStore.snapToLive.bind(telemetryStore),
    clear: telemetryStore.clear.bind(telemetryStore),
    getBuffer: telemetryStore.getBuffer.bind(telemetryStore),
    length: historyLength,
    scrubIndex,
    isLive,
    currentFrame: telemetryStore.getBuffer()[scrubIndex] ?? null,
  };

  const exportSession = useCallback(() => createSessionFile({
    frames: [...telemetryStore.getBuffer()],
    activeFields,
    customFields,
    workspace,
    playback: {
      isLive,
      scrubIndex,
    },
    ui: {
      showVectors,
      showTrail,
    } satisfies SessionUiState,
    analysis: {
      range: analysisRange,
      bookmarks,
    },
  }), [
    activeFields,
    customFields,
    workspace,
    isLive,
    scrubIndex,
    showVectors,
    showTrail,
    analysisRange,
    bookmarks,
  ]);

  const importSession = useCallback((session: SwerveScopeSessionFile) => {
    setCustomFields(session.customFields);
    setActiveFields(session.activeFields);
    setWorkspace(normalizeLayout(session.workspace));
    setShowVectors(session.ui.showVectors);
    setShowTrail(session.ui.showTrail);
    setAnalysisRangeState(normalizeRange(session.analysis?.range ?? null, session.frames.length));
    setBookmarks(Array.from(new Set((session.analysis?.bookmarks ?? []).filter(index => index >= 0 && index < session.frames.length))).sort((a, b) => a - b));
    telemetryStore.replaceHistory(session.frames, {
      isLive: session.playback.isLive,
      scrubIndex: session.playback.scrubIndex,
    });
    setIsPlaying(false);
    setPlaybackRate(1);
    setSessionError(null);
  }, []);

  const normalizedRange = useMemo(
    () => normalizeRange(analysisRange, historyLength),
    [analysisRange, historyLength],
  );

  return {
    status,
    runtimeMode,
    schemaMismatch,
    history,
    frame: history.currentFrame,
    prevFrame: telemetryStore.getPrevFrame(),
    trail: telemetryStore.getTrail(),
    showVectors,
    showTrail,
    setShowVectors,
    setShowTrail,
    isPlaying,
    setIsPlaying,
    playbackRate,
    setPlaybackRate,
    customFields,
    activeFields,
    setCustomFields,
    setActiveFields,
    workspace,
    analysisRange: normalizedRange,
    bookmarks,
    sessionError,
    setSessionError,
    exportSession,
    importSession,
    setAnalysisRange,
    setRangeStart,
    setRangeEnd,
    clearAnalysisRange,
    toggleBookmark,
    jumpToPreviousBookmark: () => jumpToNearestBookmark(-1),
    jumpToNextBookmark: () => jumpToNearestBookmark(1),
    setPaneForSlot,
    swapSlots,
    setLeftPct,
    setSlotHeight,
    resetWorkspace: () => setWorkspace(DEFAULT_LAYOUT),
    resetSimulation: () => {
      telemetryStore.clear();
      resetSimulation();
      setIsPlaying(false);
      setAnalysisRangeState(null);
      setBookmarks([]);
      setSessionError(null);
    },
    handleClear: () => {
      telemetryStore.clear();
      setIsPlaying(false);
      setAnalysisRangeState(null);
      setBookmarks([]);
    },
  };
}
