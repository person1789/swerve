import { useState, useCallback, useRef, useEffect, useMemo } from 'react';
import { MonitorPlay, Wifi, WifiOff, Monitor, Radio, ChevronDown, ChevronRight, Settings2, X, Download, Route, Star, PanelsTopLeft } from 'lucide-react';
import { useConnection } from './hooks/useConnection';
import { useGamepad } from './hooks/useGamepad';
import { FieldView } from './components/FieldView';
import { GraphView } from './components/GraphView';
import { ModuleGauges } from './components/ModuleGauges';
import { GamepadView } from './components/GamepadView';
import { OpModeSelector } from './components/OpModeSelector';
import { ConfigPanel } from './components/ConfigPanel';
import { TimelineBar } from './components/TimelineBar';
import { RouteDesigner } from './components/RouteDesigner';
import { SessionRecorder } from './lib/SessionRecorder';
import { flattenTelemetry, normalizeTelemetryFrame, type PanelLayout } from './lib/schemas';
import { loadJson, saveJson } from './lib/localStore';
import './index.css';

interface CustomGrouping {
  [key: string]: string;
}

type LayoutPresetName = 'Tuning' | 'Auto Review' | 'Driver Practice' | 'Mechanisms';

interface LayoutPreset {
  name: LayoutPresetName;
  showTelemetry: boolean;
  showModules: boolean;
  showConfig: boolean;
  showRouteDesigner: boolean;
  showGamepad: boolean;
}

const LAYOUT_PRESETS: Record<LayoutPresetName, LayoutPreset> = {
  'Tuning': { name: 'Tuning', showTelemetry: true, showModules: true, showConfig: true, showRouteDesigner: true, showGamepad: true },
  'Auto Review': { name: 'Auto Review', showTelemetry: true, showModules: true, showConfig: true, showRouteDesigner: true, showGamepad: false },
  'Driver Practice': { name: 'Driver Practice', showTelemetry: true, showModules: true, showConfig: false, showRouteDesigner: false, showGamepad: true },
  'Mechanisms': { name: 'Mechanisms', showTelemetry: true, showModules: false, showConfig: true, showRouteDesigner: false, showGamepad: true },
};

const LAYOUT_STORAGE_KEY = 'swervescope.layout.v2';

function TelemetryTable({
  data,
  onDownloadCSV,
}: {
  data: Record<string, unknown>,
  onDownloadCSV: () => void
}) {
  const [collapsed, setCollapsed] = useState<Record<string, boolean>>(() => loadJson('swervescope_collapsed_bundles', {}));
  const [customGrouping, setCustomGrouping] = useState<CustomGrouping>(() => loadJson('swervescope_custom_groups', {}));
  const [favoriteKeys, setFavoriteKeys] = useState<string[]>(() => loadJson('swervescope_telemetry_favorites', []));
  const [showFavoritesOnly, setShowFavoritesOnly] = useState(() => loadJson('swervescope_telemetry_favorites_only', false));
  const [showSettings, setShowSettings] = useState(false);
  const [search, setSearch] = useState('');
  const [lastChanged, setLastChanged] = useState<Record<string, number>>({});
  const [lastSeen, setLastSeen] = useState<Record<string, number>>({});
  const previousFlatRef = useRef<Record<string, string>>({});

  useEffect(() => { saveJson('swervescope_collapsed_bundles', collapsed); }, [collapsed]);
  useEffect(() => { saveJson('swervescope_custom_groups', customGrouping); }, [customGrouping]);
  useEffect(() => { saveJson('swervescope_telemetry_favorites', favoriteKeys); }, [favoriteKeys]);
  useEffect(() => { saveJson('swervescope_telemetry_favorites_only', showFavoritesOnly); }, [showFavoritesOnly]);

  const toggleBundle = (name: string) => {
    setCollapsed(prev => ({ ...prev, [name]: !prev[name] }));
  };

  const flatTelemetry = useMemo(() => flattenTelemetry(data), [data]);

  useEffect(() => {
    const now = Date.now();
    const changed: Record<string, number> = {};
    const seen: Record<string, number> = {};
    for (const [key, value] of Object.entries(flatTelemetry)) {
      seen[key] = now;
      if (previousFlatRef.current[key] !== value) {
        changed[key] = now;
      }
    }
    if (Object.keys(changed).length > 0) {
      setLastChanged(prev => ({ ...prev, ...changed }));
    }
    setLastSeen(prev => ({ ...prev, ...seen }));
    previousFlatRef.current = flatTelemetry;
  }, [flatTelemetry]);

  const bundles = useMemo(() => {
    const rawBundles = (data.opModeTelemetry as Record<string, Record<string, string>>) || {};
    const processed: Record<string, Record<string, string>> = {};

    Object.entries(rawBundles).forEach(([bundleName, content]) => {
      processed[bundleName] = { ...content };
    });

    const pose = data.pose as Record<string, number> | undefined;
    if (pose) {
      processed['Robot Pose'] = {
        'X (m)': Number(pose.xMeters ?? 0).toFixed(3),
        'Y (m)': Number(pose.yMeters ?? 0).toFixed(3),
        'Heading': Number(pose.headingDegrees ?? 0).toFixed(1) + ' deg',
      };
    }

    const vel = data.actualVelocity as Record<string, number> | undefined;
    if (vel) {
      processed['Robot Velocity'] = {
        'VX (m/s)': Number(vel.x ?? 0).toFixed(2),
        'VY (m/s)': Number(vel.y ?? 0).toFixed(2),
        'Omega (rad/s)': Number(vel.omega ?? 0).toFixed(2),
      };
    }

    processed['System'] = {
      'Drivetrain': String(data.drivetrainState || 'UNKNOWN'),
      'Heading Hold': String(data.headingHold || 'false'),
      'Snapping': String(data.snapping || 'false'),
      'Gamepad': data.gamepadConnected ? 'CONNECTED' : 'DISCONNECTED',
      'OpMode State': String(data.opModeState || 'STOPPED'),
    };

    Object.entries(data).forEach(([k, v]) => {
      if (['opModeTelemetry', 'pose', 'actualVelocity', 'modules', 'drivetrainState', 'headingHold', 'snapping', 'gamepadConnected', 'activeSnap', 'availableOpModes', 'activeOpMode', 'opModeState', 'snapTargetRadians'].includes(k)) return;
      if (typeof v === 'object') return;
      if (!processed['General']) processed['General'] = {};
      processed['General'][k] = String(v);
    });

    const final: Record<string, Record<string, string>> = {};
    Object.entries(processed).forEach(([bundleName, content]) => {
      Object.entries(content).forEach(([k, v]) => {
        const customTarget = customGrouping[k];
        const targetBundle = customTarget || bundleName;
        if (!final[targetBundle]) final[targetBundle] = {};
        final[targetBundle][k] = v;
      });
    });

    const loweredSearch = search.trim().toLowerCase();
    const favoriteSet = new Set(favoriteKeys);
    const filtered: Record<string, Record<string, string>> = {};
    Object.entries(final).forEach(([bundleName, content]) => {
      const kept = Object.fromEntries(
        Object.entries(content).filter(([key, value]) => {
          const matchesSearch = !loweredSearch
            || key.toLowerCase().includes(loweredSearch)
            || String(value).toLowerCase().includes(loweredSearch)
            || bundleName.toLowerCase().includes(loweredSearch);
          const matchesFavorite = !showFavoritesOnly || favoriteSet.has(key);
          return matchesSearch && matchesFavorite;
        }),
      );
      if (Object.keys(kept).length > 0) {
        filtered[bundleName] = kept;
      }
    });

    return filtered;
  }, [data, customGrouping, search, favoriteKeys, showFavoritesOnly]);

  const allKeys = useMemo(() => {
    const keys = new Set<string>();
    Object.keys(flatTelemetry).forEach(key => keys.add(key));
    return Array.from(keys).sort();
  }, [flatTelemetry]);

  const favoriteSet = useMemo(() => new Set(favoriteKeys), [favoriteKeys]);

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', padding: '0.3rem 0.45rem', borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
        <input
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          placeholder="Search telemetry"
          style={toolbarInputStyle}
        />
        <button onClick={() => setShowFavoritesOnly(prev => !prev)} style={{ ...toolbarBtnStyle, color: showFavoritesOnly ? '#ffd740' : 'var(--text-dim)' }} title="Show favorites only">
          <Star size={12} fill={showFavoritesOnly ? '#ffd740' : 'none'} />
        </button>
        <button onClick={onDownloadCSV} title="Download Background Telemetry History (CSV)" style={toolbarBtnStyle}>
          <Download size={12} />
        </button>
        <button onClick={() => setShowSettings(!showSettings)} title="Custom Grouping Settings" style={toolbarBtnStyle}>
          <Settings2 size={12} />
        </button>
      </div>

      <div style={{ flex: 1, overflow: 'auto', padding: '0 0.25rem' }}>
        {Object.entries(bundles).sort().map(([name, content]) => (
          <div key={name} style={{ marginBottom: '2px' }}>
            <div onClick={() => toggleBundle(name)} style={{ padding: '0.35rem 0.5rem', backgroundColor: 'rgba(255,255,255,0.03)', borderRadius: '4px', display: 'flex', alignItems: 'center', gap: '6px', cursor: 'pointer', userSelect: 'none' }}>
              {collapsed[name] ? <ChevronRight size={10} color="var(--text-dim)" /> : <ChevronDown size={10} color="var(--text-dim)" />}
              <span style={{ fontSize: '0.6rem', fontWeight: 800, color: 'var(--text-dim)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>{name}</span>
              <span style={{ marginLeft: 'auto', fontSize: '0.55rem', color: 'rgba(255,255,255,0.15)' }}>{Object.keys(content).length} keys</span>
            </div>

            {!collapsed[name] && (
              <table className="telem-table" style={{ marginLeft: '12px', width: 'calc(100% - 12px)' }}>
                <tbody>
                  {Object.entries(content).map(([k, v]) => {
                    const changedAgoMs = Date.now() - (lastChanged[k] ?? 0);
                    const seenAgoMs = Date.now() - (lastSeen[k] ?? 0);
                    const isFresh = changedAgoMs < 1600;
                    const isStale = seenAgoMs > 5000;
                    return (
                      <tr key={k} style={{ background: isFresh ? 'rgba(124,77,255,0.09)' : undefined, opacity: isStale ? 0.45 : 1 }}>
                        <td className="telem-key" style={{ fontSize: '0.6rem', display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                          <button onClick={(event) => {
                            event.stopPropagation();
                            setFavoriteKeys(prev => prev.includes(k) ? prev.filter(item => item !== k) : [...prev, k].sort());
                          }} style={{ ...toolbarBtnStyle, padding: 0, color: favoriteSet.has(k) ? '#ffd740' : 'rgba(255,255,255,0.18)' }}>
                            <Star size={10} fill={favoriteSet.has(k) ? '#ffd740' : 'none'} />
                          </button>
                          {k}
                        </td>
                        <td className="telem-val" style={{ fontSize: '0.6rem' }}>{v}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            )}
          </div>
        ))}

        {Object.keys(bundles).length === 0 && (
          <div style={{ padding: '1rem', color: 'var(--text-dim)', fontSize: '0.7rem', textAlign: 'center' }}>No telemetry data</div>
        )}
      </div>

      {showSettings && (
        <div style={{ position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(0,0,0,0.85)', backdropFilter: 'blur(8px)', zIndex: 1000, display: 'flex', flexDirection: 'column', padding: '1rem', borderRadius: 'var(--radius)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '1rem' }}>
            <h3 style={{ fontSize: '0.8rem', margin: 0, color: 'var(--accent)' }}>Telemetry Bundles</h3>
            <button onClick={() => setShowSettings(false)} style={{ background: 'none', border: 'none', color: 'white', cursor: 'pointer' }}><X size={16} /></button>
          </div>

          <div style={{ flex: 1, overflow: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr style={{ color: 'var(--text-dim)', fontSize: '0.6rem', textAlign: 'left' }}>
                  <th style={{ padding: '0.5rem' }}>KEY</th>
                  <th style={{ padding: '0.5rem' }}>TARGET BUNDLE</th>
                </tr>
              </thead>
              <tbody>
                {allKeys.map(key => (
                  <tr key={key} style={{ borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
                    <td style={{ padding: '0.4rem', fontSize: '0.65rem', fontFamily: 'var(--font-mono)' }}>{key}</td>
                    <td style={{ padding: '0.4rem' }}>
                      <input
                        type="text"
                        placeholder="Inherit"
                        value={customGrouping[key] || ''}
                        onChange={(e) => {
                          const val = e.target.value;
                          setCustomGrouping(prev => {
                            const next = { ...prev };
                            if (!val) delete next[key];
                            else next[key] = val;
                            return next;
                          });
                        }}
                        style={{ backgroundColor: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.1)', borderRadius: '4px', color: 'white', fontSize: '0.65rem', padding: '0.2rem 0.4rem', width: '100%', outline: 'none' }}
                      />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}

function App() {
  const [telemetry, setTelemetry] = useState<Record<string, unknown>>({});
  const [config, setConfig] = useState<Record<string, unknown>>({});
  const [availableOpModes, setAvailableOpModes] = useState<string[]>([]);
  const [activeOpMode, setActiveOpMode] = useState('');
  const [opModeState, setOpModeState] = useState('STOPPED');
  const [routeDesignerMode, setRouteDesignerMode] = useState<'panel' | 'full'>('panel');
  const [layoutPresetName, setLayoutPresetName] = useState<LayoutPresetName>(() => loadJson(LAYOUT_STORAGE_KEY, { name: 'Tuning' as LayoutPresetName }).name);
  const recorderRef = useRef(new SessionRecorder());
  const historyRef = useRef<Record<string, unknown>[]>([]);
  const currentFrameRef = useRef(normalizeTelemetryFrame({}, 'sim', 0));

  const layoutPreset = LAYOUT_PRESETS[layoutPresetName];

  useEffect(() => {
    const payload: PanelLayout = {
      schemaVersion: 2,
      name: layoutPresetName,
      mode: routeDesignerMode === 'full' ? 'route-builder' : 'dashboard',
      updatedAtMs: Date.now(),
      items: [
        { id: 'telemetry', visible: layoutPreset.showTelemetry },
        { id: 'modules', visible: layoutPreset.showModules },
        { id: 'config', visible: layoutPreset.showConfig },
        { id: 'route', visible: layoutPreset.showRouteDesigner },
        { id: 'gamepad', visible: layoutPreset.showGamepad },
      ],
    };
    saveJson(LAYOUT_STORAGE_KEY, payload);
    void fetch(`http://${window.location.hostname || 'localhost'}:8080/api/layouts`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ current: payload }),
    }).catch(() => undefined);
  }, [layoutPreset, layoutPresetName, routeDesignerMode]);

  const handleMessage = useCallback((msg: Record<string, unknown>) => {
    const type = msg.type as string;
    const data = msg.data as Record<string, unknown>;
    switch (type) {
      case 'state':
        setTelemetry(data);
        currentFrameRef.current = normalizeTelemetryFrame(data, 'sim', Date.now(), config);
        historyRef.current.push(data);
        if (historyRef.current.length > 15000) {
          historyRef.current.shift();
        }
        if (data.availableOpModes) setAvailableOpModes(data.availableOpModes as string[]);
        if (data.activeOpMode !== undefined) setActiveOpMode(data.activeOpMode as string);
        if (data.opModeState) setOpModeState(data.opModeState as string);
        break;
      case 'config':
        setConfig(data);
        break;
      case 'opmodes':
        setAvailableOpModes(data as unknown as string[]);
        break;
    }
  }, [config]);

  const downloadTelemetryCSV = useCallback(() => {
    if (historyRef.current.length === 0) {
      alert('No telemetry data to download yet.');
      return;
    }

    const sessionFrames = historyRef.current.map((data, i) => normalizeTelemetryFrame(data, 'sim', i * 20, config));
    const dummySession = {
      manifest: {
        schemaVersion: 2 as const,
        id: 'telemetry-export',
        name: 'telemetry_export',
        source: 'sim' as const,
        startedAtMs: Date.now(),
        endedAtMs: Date.now(),
        frameCount: sessionFrames.length,
        tags: [],
      },
      frames: sessionFrames,
    };
    const csv = SessionRecorder.exportCSV(dummySession);

    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `SwerveScope_Telemetry_${new Date().toISOString().replace(/[:.]/g, '-')}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }, [config]);

  const { mode, ws, connected, setManualMode } = useConnection(handleMessage);
  const { bindings, updateBindings } = useGamepad(ws, connected);

  const handleReplayFrame = useCallback((data: Record<string, unknown> | null) => {
    if (data) {
      setTelemetry(data);
    }
  }, []);

  const fieldTelemetry = useMemo(() => ({
    ...telemetry,
    _currentFrame: currentFrameRef.current,
  }), [telemetry]);

  return (
    <div style={{ height: '100vh', width: '100vw', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
      <header className="glass-panel" style={{ margin: '0.5rem 0.5rem 0', padding: '0.5rem 1.25rem', display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexShrink: 0, borderRadius: 'var(--radius)' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
          <MonitorPlay style={{ color: 'var(--accent)', width: 20, height: 20 }} />
          <span style={{ fontSize: '1.05rem', fontWeight: 700, letterSpacing: '0.02em' }}>SwerveScope</span>

          <div style={{ width: 1, height: 20, backgroundColor: 'var(--border)', margin: '0 0.25rem' }} />

          <div style={{ display: 'flex', gap: '2px', backgroundColor: 'rgba(255,255,255,0.04)', borderRadius: '8px', padding: '2px' }}>
            <button onClick={() => setManualMode('sim')} style={{ ...modeBtn, backgroundColor: mode === 'sim' ? 'var(--accent-soft)' : 'transparent', color: mode === 'sim' ? '#b388ff' : 'var(--text-dim)' }}>
              <Monitor size={11} /> Sim
            </button>
            <button onClick={() => setManualMode('robot')} style={{ ...modeBtn, backgroundColor: mode === 'robot' ? 'rgba(0,230,118,0.15)' : 'transparent', color: mode === 'robot' ? '#00e676' : 'var(--text-dim)' }}>
              <Radio size={11} /> Robot
            </button>
          </div>

          <span style={{ display: 'inline-flex', alignItems: 'center', gap: '0.3rem', fontSize: '0.7rem', padding: '0.15rem 0.55rem', borderRadius: 9999, backgroundColor: connected ? 'rgba(0,230,118,0.08)' : 'rgba(255,82,82,0.08)', color: connected ? 'var(--success)' : 'var(--danger)', border: `1px solid ${connected ? 'rgba(0,230,118,0.15)' : 'rgba(255,82,82,0.15)'}`, fontWeight: 600, animation: connected ? 'pulse 2s infinite' : 'none' }}>
            {connected ? <Wifi size={11} /> : <WifiOff size={11} />}
            {connected ? mode.toUpperCase() : 'OFFLINE'}
          </span>

          <div style={{ width: 1, height: 20, backgroundColor: 'var(--border)', margin: '0 0.25rem' }} />

          <OpModeSelector availableOpModes={availableOpModes} activeOpMode={activeOpMode} opModeState={opModeState} ws={ws} />

          <div style={{ display: 'inline-flex', alignItems: 'center', gap: '0.35rem', padding: '0.1rem 0.35rem', borderRadius: '8px', border: '1px solid rgba(255,255,255,0.08)', background: 'rgba(255,255,255,0.03)' }}>
            <PanelsTopLeft size={12} color="var(--text-dim)" />
            <select
              value={layoutPresetName}
              onChange={(event) => setLayoutPresetName(event.target.value as LayoutPresetName)}
              style={{ background: 'transparent', color: 'var(--text-primary)', border: 'none', outline: 'none', fontSize: '0.68rem', fontWeight: 600 }}
            >
              {Object.keys(LAYOUT_PRESETS).map(name => (
                <option key={name} value={name} style={{ color: '#111' }}>{name}</option>
              ))}
            </select>
          </div>

          <button onClick={() => setRouteDesignerMode(prev => prev === 'full' ? 'panel' : 'full')} style={{ ...modeBtn, backgroundColor: routeDesignerMode === 'full' ? 'rgba(124,77,255,0.15)' : 'transparent', color: routeDesignerMode === 'full' ? '#d9ccff' : 'var(--text-dim)', border: '1px solid rgba(124,77,255,0.2)' }}>
            <Route size={11} /> Path Builder
          </button>
        </div>
      </header>

      <div style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column', gap: '0.5rem', padding: '0.5rem' }}>
        {routeDesignerMode === 'full' ? (
          <div className="glass-panel" style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
            <div className="panel-header">Route Designer Workspace</div>
            <div style={{ flex: 1, minHeight: 0 }}>
              <RouteDesigner />
            </div>
          </div>
        ) : (
          <>
            <div className="glass-panel" style={{ flex: '1.3 1 0%', display: 'flex', flexDirection: 'column', minHeight: 0, overflow: 'hidden' }}>
              <div className="panel-header">Field View</div>
              <div style={{ flex: 1, minHeight: 0 }}>
                <FieldView telemetry={fieldTelemetry} />
              </div>
            </div>

            <div style={{ flex: '1 1 0%', minHeight: 0, display: 'flex', gap: '0.5rem' }}>
              <div className="glass-panel" style={{ flex: '1.2 1 0%', display: 'flex', flexDirection: 'column', minHeight: 0, overflow: 'hidden' }}>
                <div className="panel-header">Telemetry Graph</div>
                <div style={{ flex: 1, minHeight: 0 }}>
                  <GraphView telemetry={telemetry} />
                </div>
              </div>

              <div style={{ flex: '1 1 0%', display: 'flex', flexDirection: 'column', gap: '0.5rem', minHeight: 0 }}>
                {layoutPreset.showGamepad && (
                  <div className="glass-panel" style={{ flex: '0.8 1 0%', display: 'flex', flexDirection: 'column', minHeight: 0, overflow: 'hidden' }}>
                    <div style={{ flex: 1, minHeight: 0 }}>
                      <GamepadView bindings={bindings} onUpdateBindings={updateBindings} />
                    </div>
                  </div>
                )}

                <div style={{ flex: '1 1 0%', display: 'flex', gap: '0.5rem', minHeight: 0 }}>
                  {layoutPreset.showTelemetry && (
                    <div className="glass-panel" style={{ flex: '1 1 0%', display: 'flex', flexDirection: 'column', minHeight: 0, overflow: 'hidden', position: 'relative' }}>
                      <div className="panel-header">Telemetry</div>
                      <div style={{ flex: 1, minHeight: 0 }}>
                        <TelemetryTable data={telemetry} onDownloadCSV={downloadTelemetryCSV} />
                      </div>
                    </div>
                  )}

                  {layoutPreset.showModules && (
                    <div className="glass-panel" style={{ flex: '1 1 0%', display: 'flex', flexDirection: 'column', minHeight: 0, overflow: 'hidden' }}>
                      <div className="panel-header">Modules</div>
                      <div style={{ flex: 1, minHeight: 0, padding: '0.35rem', overflow: 'auto' }}>
                        <ModuleGauges modules={(telemetry as Record<string, unknown>).modules as Record<string, unknown>[]} />
                      </div>
                    </div>
                  )}

                  {layoutPreset.showConfig && (
                    <div className="glass-panel" style={{ flex: '1 1 0%', display: 'flex', flexDirection: 'column', minHeight: 0, overflow: 'hidden' }}>
                      <div className="panel-header">Config</div>
                      <div style={{ flex: 1, minHeight: 0 }}>
                        <ConfigPanel config={config} ws={ws} />
                      </div>
                    </div>
                  )}

                  {layoutPreset.showRouteDesigner && (
                    <div className="glass-panel" style={{ flex: '1.3 1 0%', display: 'flex', flexDirection: 'column', minHeight: 0, overflow: 'hidden' }}>
                      <div className="panel-header">Route Designer</div>
                      <div style={{ flex: 1, minHeight: 0 }}>
                        <RouteDesigner />
                      </div>
                    </div>
                  )}
                </div>
              </div>
            </div>
          </>
        )}
      </div>

      {routeDesignerMode !== 'full' && (
        <TimelineBar recorder={recorderRef.current} telemetry={telemetry} onReplayFrame={handleReplayFrame} />
      )}
    </div>
  );
}

const modeBtn: React.CSSProperties = {
  display: 'flex', alignItems: 'center', gap: '4px',
  padding: '0.2rem 0.5rem', fontSize: '0.6rem', fontWeight: 600,
  border: 'none', borderRadius: '6px', cursor: 'pointer',
  background: 'none', transition: 'all 0.15s ease',
};

const toolbarBtnStyle: React.CSSProperties = {
  background: 'none',
  border: 'none',
  color: 'var(--text-dim)',
  cursor: 'pointer',
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  gap: '4px',
  padding: '0.2rem',
};

const toolbarInputStyle: React.CSSProperties = {
  flex: 1,
  minWidth: 0,
  backgroundColor: 'rgba(255,255,255,0.05)',
  border: '1px solid rgba(255,255,255,0.08)',
  borderRadius: 6,
  color: 'var(--text-primary)',
  fontSize: '0.66rem',
  padding: '0.32rem 0.45rem',
  outline: 'none',
};

export default App;
