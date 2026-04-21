import { useCallback, useEffect, useRef, useState } from 'react';
import type { DerivedField, TelemetryFrame, WorkspaceLayout, WorkspacePaneId, WorkspaceSlotId } from '../types/telemetry';
import { useGamepad } from './useGamepad';
import { useHistory } from './useHistory';
import { useSITL } from './useSITL';

const DEFAULT_ACTIVE_FIELDS = ['speed', 'hdg_deg', 'battery', 'loop_ms', 'total_current', 'm0_spd', 'm1_spd', 'm2_spd', 'm3_spd'];
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
    return raw ? JSON.parse(raw) as T : fallback;
  } catch {
    return fallback;
  }
}

function normalizeLayout(input: WorkspaceLayout): WorkspaceLayout {
  return {
    leftPct: Math.max(35, Math.min(75, input.leftPct ?? DEFAULT_LAYOUT.leftPct)),
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

export function useSwerveScopeState() {
  const history = useHistory();
  const prevFrameRef = useRef<TelemetryFrame | null>(null);
  const trailRef = useRef<{ x: number; y: number }[]>([]);

  const [showVectors, setShowVectors] = useState(true);
  const [showTrail, setShowTrail] = useState(true);
  const [isPlaying, setIsPlaying] = useState(false);
  const [playbackRate, setPlaybackRate] = useState(1);
  const [customFields, setCustomFields] = useState<DerivedField[]>(() => loadJson('scope_custom_fields', []));
  const [activeFields, setActiveFields] = useState<string[]>(() => loadJson('scope_active_fields', DEFAULT_ACTIVE_FIELDS));
  const [workspace, setWorkspace] = useState<WorkspaceLayout>(() => normalizeLayout(loadJson('scope_workspace_layout', DEFAULT_LAYOUT)));

  const handleFrame = useCallback((frame: TelemetryFrame) => {
    prevFrameRef.current = history.currentFrame;
    history.push(frame);
  }, [history]);

  const { status, runtimeMode, sendInput, schemaMismatch } = useSITL(handleFrame);
  useGamepad(sendInput, true);

  const frame = history.currentFrame;
  if (frame) {
    const last = trailRef.current[trailRef.current.length - 1];
    if (!last || Math.hypot(frame.x - last.x, frame.y - last.y) > 0.01) {
      trailRef.current.push({ x: frame.x, y: frame.y });
      if (trailRef.current.length > 2500) trailRef.current.shift();
    }
  }

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
    if (history.isLive || !isPlaying) return;
    const id = window.setInterval(() => {
      if (history.scrubIndex >= history.length - 1) {
        setIsPlaying(false);
        return;
      }
      history.stepBy(1);
    }, Math.max(5, 20 / playbackRate));
    return () => window.clearInterval(id);
  }, [history, isPlaying, playbackRate]);

  const setPaneForSlot = useCallback((slot: WorkspaceSlotId, pane: WorkspacePaneId) => {
    setWorkspace(prev => ({ ...prev, slots: { ...prev.slots, [slot]: pane } }));
  }, []);

  const swapSlots = useCallback((from: WorkspaceSlotId, to: WorkspaceSlotId) => {
    if (from === to) return;
    setWorkspace(prev => ({
      ...prev,
      slots: {
        ...prev.slots,
        [from]: prev.slots[to],
        [to]: prev.slots[from],
      },
    }));
  }, []);

  const setLeftPct = useCallback((next: number) => {
    setWorkspace(prev => ({ ...prev, leftPct: Math.max(35, Math.min(75, next)) }));
  }, []);

  const setSlotHeight = useCallback((slot: 'primary' | 'secondary' | 'sidebarTop' | 'sidebarMiddle' | 'sidebarBottom', next: number) => {
    setWorkspace(prev => ({
      ...prev,
      slotHeights: {
        ...prev.slotHeights,
        [slot]: Math.max(15, Math.min(80, next)),
      },
    }));
  }, []);

  return {
    status,
    runtimeMode,
    schemaMismatch,
    history,
    frame,
    prevFrame: prevFrameRef.current,
    trail: trailRef.current,
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
    setPaneForSlot,
    swapSlots,
    setLeftPct,
    setSlotHeight,
    resetWorkspace: () => setWorkspace(DEFAULT_LAYOUT),
    handleClear: () => {
      history.clear();
      trailRef.current = [];
      prevFrameRef.current = null;
      setIsPlaying(false);
    },
  };
}
