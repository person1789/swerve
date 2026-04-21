// App.tsx — SwerveScope main application shell
import { useState, useCallback, useEffect, useRef } from 'react';
import type { DerivedField, TelemetryFrame } from './types/telemetry';
import { useHistory } from './hooks/useHistory';
import { useSITL } from './hooks/useSITL';
import { useGamepad } from './hooks/useGamepad';

import ControlBar    from './components/ControlBar';
import Arena         from './components/Arena';
import ScopePanel    from './components/ScopePanel';
import ModuleDetail  from './components/ModuleDetail';
import InspectorPanel from './components/InspectorPanel';
import JoystickPanel from './components/JoystickPanel';
import ConsolePanel  from './components/ConsolePanel';
import ExprEditor    from './components/ExprEditor';

// ── Tab / layout definitions ──────────────────────────────────────────────
const TABS = [
  { id: 'drive',      label: 'Drive'      },
  { id: 'modules',   label: 'Modules'    },
  { id: 'scopes',    label: 'Scopes'     },
  { id: 'joystick',  label: 'Joystick'   },
  { id: 'inspector', label: 'Inspector'  },
  { id: 'console',   label: 'Console'    },
  { id: 'exprs',     label: 'Expressions'},
];

// ── Panel wrapper with header ──────────────────────────────────────────────
function Panel({
  title,
  children,
  className = '',
}: {
  title?: string;
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <div className={`panel flex flex-col overflow-hidden ${className}`}>
      {title && (
        <div className="panel-header">
          <span className="title">{title}</span>
        </div>
      )}
      <div className="panel-body flex-1 overflow-hidden">
        {children}
      </div>
    </div>
  );
}

// ── Resizable splitter ────────────────────────────────────────────────────
function HSplitter({ onDrag }: { onDrag: (dx: number) => void }) {
  const dragging = useRef(false);
  const lastX    = useRef(0);

  return (
    <div
      className="w-1 flex-shrink-0 cursor-col-resize hover:bg-scope-accent transition-colors bg-scope-border"
      onMouseDown={e => {
        dragging.current = true;
        lastX.current    = e.clientX;
        const move = (ev: MouseEvent) => {
          if (!dragging.current) return;
          onDrag(ev.clientX - lastX.current);
          lastX.current = ev.clientX;
        };
        const up = () => { dragging.current = false; };
        window.addEventListener('mousemove', move);
        window.addEventListener('mouseup', up, { once: true });
      }}
    />
  );
}

function VSplitter({ onDrag }: { onDrag: (dy: number) => void }) {
  const dragging = useRef(false);
  const lastY    = useRef(0);

  return (
    <div
      className="h-1 flex-shrink-0 cursor-row-resize hover:bg-scope-accent transition-colors bg-scope-border"
      onMouseDown={e => {
        dragging.current = true;
        lastY.current    = e.clientY;
        const move = (ev: MouseEvent) => {
          if (!dragging.current) return;
          onDrag(ev.clientY - lastY.current);
          lastY.current = ev.clientY;
        };
        const up = () => { dragging.current = false; };
        window.addEventListener('mousemove', move);
        window.addEventListener('mouseup', up, { once: true });
      }}
    />
  );
}

// ── App ───────────────────────────────────────────────────────────────────
export default function App() {
  // ── State ──────────────────────────────────────────────────────────────
  const [activeTab,    setActiveTab]    = useState('drive');
  const [showVectors,  setShowVectors]  = useState(true);
  const [showTrail,    setShowTrail]    = useState(true);
  const [isPlaying,    setIsPlaying]    = useState(false);
  const [playbackRate, setPlaybackRate] = useState(1);
  const [customFields, setCustomFields] = useState<DerivedField[]>(() => {
    try {
      return JSON.parse(localStorage.getItem('scope_custom_fields') || '[]');
    } catch {
      return [];
    }
  });
  const [activeFields, setActiveFields] = useState<string[]>(() => {
    try {
      return JSON.parse(localStorage.getItem('scope_active_fields') || '["speed","hdg_deg","battery","loop_ms","m0_spd","m1_spd","m2_spd","m3_spd"]');
    } catch {
      return ['speed', 'hdg_deg', 'battery', 'loop_ms', 'm0_spd', 'm1_spd', 'm2_spd', 'm3_spd'];
    }
  });

  // Resizable left/right split (percent 0–100)
  const [leftPct, setLeftPct] = useState(62);
  // Resizable top/bottom within right column
  const [topPct, setTopPct]   = useState(55);

  // ── History ────────────────────────────────────────────────────────────
  const history = useHistory();
  const prevFrameRef = useRef<TelemetryFrame | null>(null);

  const handleFrame = useCallback((frame: TelemetryFrame) => {
    history.push(frame);
    prevFrameRef.current = history.currentFrame;
  }, [history]);

  // ── SITL bridge ────────────────────────────────────────────────────────
  const { status, sendInput } = useSITL(handleFrame);
  useGamepad(sendInput, status === 'connected');

  // ── Odometry trail ─────────────────────────────────────────────────────
  const trailRef = useRef<{ x: number; y: number }[]>([]);
  if (history.currentFrame) {
    const { x, y } = history.currentFrame;
    const last = trailRef.current[trailRef.current.length - 1];
    if (!last || Math.hypot(x - last.x, y - last.y) > 0.01) {
      trailRef.current.push({ x, y });
      if (trailRef.current.length > 2000) trailRef.current.shift();
    }
  }

  // ── Expression field management ─────────────────────────────────────────
  const addField = useCallback((f: DerivedField) => {
    setCustomFields(prev => [...prev, f]);
    setActiveFields(prev => [...prev, f.id]);
  }, []);

  const removeField = useCallback((id: string) => {
    setCustomFields(prev => prev.filter(f => f.id !== id));
    setActiveFields(prev => prev.filter(i => i !== id));
  }, []);

  const toggleField = useCallback((id: string) => {
    setActiveFields(prev =>
      prev.includes(id) ? prev.filter(i => i !== id) : [...prev, id],
    );
  }, []);

  useEffect(() => {
    localStorage.setItem('scope_custom_fields', JSON.stringify(customFields));
  }, [customFields]);

  useEffect(() => {
    localStorage.setItem('scope_active_fields', JSON.stringify(activeFields));
  }, [activeFields]);

  // ── Clear ────────────────────────────────────────────────────────────
  const handleClear = useCallback(() => {
    history.clear();
    trailRef.current = [];
    setIsPlaying(false);
  }, [history]);

  // ── Resize handlers ──────────────────────────────────────────────────
  const handleHDrag = useCallback((dx: number) => {
    setLeftPct(p => Math.max(25, Math.min(80, p + (dx / window.innerWidth) * 100)));
  }, []);

  const handleVDrag = useCallback((dy: number) => {
    const mainH = window.innerHeight - 70; // subtract header
    setTopPct(p => Math.max(20, Math.min(80, p + (dy / mainH) * 100)));
  }, []);

  const frame    = history.currentFrame;
  const prevFrame = prevFrameRef.current;

  // ── Playback loop ──────────────────────────────────────────────────────
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

  // ── Keyboard shortcuts ────────────────────────────────────────────────
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (
        e.target instanceof HTMLInputElement ||
        e.target instanceof HTMLTextAreaElement ||
        e.target instanceof HTMLSelectElement
      ) return;

      switch (e.key) {
        case ' ':
          e.preventDefault();
          history.snapToLive();
          setIsPlaying(false);
          break;
        case 'l':
        case 'L':
          setShowTrail(v => !v);
          break;
        case 'v':
        case 'V':
          setShowVectors(v => !v);
          break;
        case 'k':
        case 'K':
          if (!history.isLive) setIsPlaying(v => !v);
          break;
        case 'ArrowLeft':
          if (!history.isLive) history.stepBy(-1);
          break;
        case 'ArrowRight':
          if (!history.isLive) history.stepBy(1);
          break;
        case '1': setActiveTab('drive'); break;
        case '2': setActiveTab('modules'); break;
        case '3': setActiveTab('scopes'); break;
        case '4': setActiveTab('joystick'); break;
        case '5': setActiveTab('inspector'); break;
        case '6': setActiveTab('console'); break;
        case '7': setActiveTab('exprs'); break;
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [history]);

  // ── Right-column secondary content ─────────────────────────────────
  const renderMain = () => {
    switch (activeTab) {
      case 'drive':
        return (
          <div className="split-h flex-1 min-h-0">
            {/* Left: Arena */}
            <div className="flex flex-col min-w-0" style={{ width: `${leftPct}%` }}>
              <Panel title="Field View // Arena" className="flex-1">
                <Arena
                  frame={frame}
                  trail={trailRef.current}
                  showVectors={showVectors}
                  showTrail={showTrail}
                />
              </Panel>
            </div>

            <HSplitter onDrag={handleHDrag} />

            {/* Right: Inspector top, Console bottom */}
            <div className="flex flex-col flex-1 min-w-0 gap-1">
              <div style={{ height: `${topPct}%` }}>
                <Panel title="Inspection Engine // Live" className="h-full">
                  <InspectorPanel
                    history={history.getBuffer()}
                    historyLength={history.length}
                    currentFrame={frame}
                    isLive={history.isLive}
                  />
                </Panel>
              </div>
              <VSplitter onDrag={handleVDrag} />
              <div className="flex-1 min-h-0">
                <Panel title="System Console // Events" className="h-full">
                  <ConsolePanel
                    frame={frame}
                    prevFrame={prevFrame}
                    isLive={history.isLive}
                  />
                </Panel>
              </div>
            </div>
          </div>
        );

      case 'modules':
        return (
          <div className="split-h flex-1 min-h-0">
            <div className="flex flex-col min-w-0" style={{ width: `${leftPct}%` }}>
              <Panel title="Field View // Arena" className="flex-1">
                <Arena
                  frame={frame}
                  trail={trailRef.current}
                  showVectors={showVectors}
                  showTrail={showTrail}
                />
              </Panel>
            </div>
            <HSplitter onDrag={handleHDrag} />
            <div className="flex-1 min-w-0">
              <Panel title="Module Detailer // FL FR RR RL" className="h-full">
                <ModuleDetail frame={frame} />
              </Panel>
            </div>
          </div>
        );

      case 'scopes':
        return (
          <div className="split-h flex-1 min-h-0">
            <div className="flex flex-col min-w-0" style={{ width: `${leftPct}%` }}>
              <Panel title="Diagnostic Scopes // Time-Series" className="flex-1">
                <ScopePanel
                  history={history.getBuffer()}
                  historyLength={history.length}
                  isLive={history.isLive}
                  activeFields={activeFields}
                  customFields={customFields}
                />
              </Panel>
            </div>
            <HSplitter onDrag={handleHDrag} />
            <div className="flex-1 min-w-0">
              <Panel title="Expression Editor // Virtual Fields" className="h-full">
                <ExprEditor
                  customFields={customFields}
                  activeFields={activeFields}
                  onAdd={addField}
                  onRemove={removeField}
                  onToggle={toggleField}
                  sampleFrame={frame}
                />
              </Panel>
            </div>
          </div>
        );

      case 'joystick':
        return (
          <div className="flex-1 min-h-0">
            <Panel title="Joystick Visualizer // Gamepad State" className="h-full">
              <JoystickPanel frame={frame} />
            </Panel>
          </div>
        );

      case 'inspector':
        return (
          <div className="split-h flex-1 min-h-0">
            <div className="flex flex-col min-w-0" style={{ width: `${leftPct}%` }}>
              <Panel title="Inspection Engine // Telemetry" className="flex-1">
                  <InspectorPanel
                    history={history.getBuffer()}
                    historyLength={history.length}
                    currentFrame={frame}
                    isLive={history.isLive}
                  />
              </Panel>
            </div>
            <HSplitter onDrag={handleHDrag} />
            <div className="flex-1 min-w-0">
              <Panel title="Module Detailer" className="h-full">
                <ModuleDetail frame={frame} />
              </Panel>
            </div>
          </div>
        );

      case 'console':
        return (
          <div className="flex-1 min-h-0">
            <Panel title="System Console // Event Log" className="h-full">
              <ConsolePanel
                frame={frame}
                prevFrame={prevFrame}
                isLive={history.isLive}
              />
            </Panel>
          </div>
        );

      case 'exprs':
        return (
          <div className="split-h flex-1 min-h-0">
            <div className="flex flex-col min-w-0" style={{ width: `${leftPct}%` }}>
              <Panel title="Diagnostic Scopes" className="flex-1">
                <ScopePanel
                  history={history.getBuffer()}
                  historyLength={history.length}
                  isLive={history.isLive}
                  activeFields={activeFields}
                  customFields={customFields}
                />
              </Panel>
            </div>
            <HSplitter onDrag={handleHDrag} />
            <div className="flex-1 min-w-0">
              <Panel title="Expression Editor" className="h-full">
                <ExprEditor
                  customFields={customFields}
                  activeFields={activeFields}
                  onAdd={addField}
                  onRemove={removeField}
                  onToggle={toggleField}
                  sampleFrame={frame}
                />
              </Panel>
            </div>
          </div>
        );

      default: return null;
    }
  };

  return (
    <div className="flex flex-col w-screen h-screen overflow-hidden bg-scope-bg">
      <ControlBar
        status={status}
        isLive={history.isLive}
        isPlaying={isPlaying}
        playbackRate={playbackRate}
        historyLength={history.length}
        scrubIndex={history.scrubIndex}
        durationSec={history.length * 0.02}
        onSeek={history.seek}
        onTogglePlay={() => setIsPlaying(v => !v)}
        onStep={(delta) => history.stepBy(delta)}
        onRateChange={setPlaybackRate}
        onSnapToLive={history.snapToLive}
        onClearHistory={handleClear}
        showVectors={showVectors}
        showTrail={showTrail}
        onToggleVectors={() => setShowVectors(v => !v)}
        onToggleTrail={() => setShowTrail(v => !v)}
        activeTab={activeTab}
        onTabChange={setActiveTab}
        tabs={TABS}
      />

      <div className="flex-1 min-h-0 p-1 flex flex-col">
        {renderMain()}
      </div>
    </div>
  );
}
