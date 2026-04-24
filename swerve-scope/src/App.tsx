import { useState, useCallback, useRef, useEffect, useMemo } from 'react';
import { MonitorPlay, Wifi, WifiOff, Monitor, Radio, ChevronDown, ChevronRight, Settings2, X, Download } from 'lucide-react';
import { useConnection } from './hooks/useConnection';
import { useGamepad } from './hooks/useGamepad';
import { FieldView } from './components/FieldView';
import { GraphView } from './components/GraphView';
import { ModuleGauges } from './components/ModuleGauges';
import { GamepadView } from './components/GamepadView';
import { OpModeSelector } from './components/OpModeSelector';
import { ConfigPanel } from './components/ConfigPanel';
import { TimelineBar } from './components/TimelineBar';
import { SessionRecorder } from './lib/SessionRecorder';
import './index.css';

/* ── Custom Bundle Manager ── */
interface CustomGrouping {
  [key: string]: string; // Key -> BundleName
}

function TelemetryTable({ data, onDownloadCSV }: { data: Record<string, unknown>, onDownloadCSV: () => void }) {
  const [collapsed, setCollapsed] = useState<Record<string, boolean>>(() => {
    const saved = localStorage.getItem('swervescope_collapsed_bundles');
    return saved ? JSON.parse(saved) : {};
  });

  const [customGrouping, setCustomGrouping] = useState<CustomGrouping>(() => {
    const saved = localStorage.getItem('swervescope_custom_groups');
    return saved ? JSON.parse(saved) : {};
  });

  const [showSettings, setShowSettings] = useState(false);

  // Persist collapsed state
  useEffect(() => {
    localStorage.setItem('swervescope_collapsed_bundles', JSON.stringify(collapsed));
  }, [collapsed]);

  // Persist custom grouping
  useEffect(() => {
    localStorage.setItem('swervescope_custom_groups', JSON.stringify(customGrouping));
  }, [customGrouping]);

  const toggleBundle = (name: string) => {
    setCollapsed(prev => ({ ...prev, [name]: !prev[name] }));
  };

  const bundles = useMemo(() => {
    const rawBundles = (data.opModeTelemetry as Record<string, Record<string, string>>) || {};
    const processed: Record<string, Record<string, string>> = {};

    // 1. Start with original bundles from Java
    Object.entries(rawBundles).forEach(([bundleName, content]) => {
      processed[bundleName] = { ...content };
    });

    // 2. Bundle core simulator state by default
    const pose = data.pose as Record<string, number> | undefined;
    if (pose) {
      processed['Robot Pose'] = {
        'X (m)': pose.xMeters.toFixed(3),
        'Y (m)': pose.yMeters.toFixed(3),
        'Heading': pose.headingDegrees.toFixed(1) + '°',
      };
    }

    const vel = data.actualVelocity as Record<string, number> | undefined;
    if (vel) {
      processed['Robot Velocity'] = {
        'VX (m/s)': vel.x.toFixed(2),
        'VY (m/s)': vel.y.toFixed(2),
        'Omega (rad/s)': vel.omega.toFixed(2),
      };
    }

    processed['System'] = {
      'Drivetrain': String(data.drivetrainState || 'UNKNOWN'),
      'Heading Hold': String(data.headingHold || 'false'),
      'Snapping': String(data.snapping || 'false'),
      'Gamepad': data.gamepadConnected ? 'CONNECTED' : 'DISCONNECTED',
    };

    // 3. Handle any other flat telemetry (fallback to General)
    Object.entries(data).forEach(([k, v]) => {
      if (['opModeTelemetry', 'pose', 'actualVelocity', 'modules', 'drivetrainState', 'headingHold', 'snapping', 'gamepadConnected', 'activeSnap', 'availableOpModes', 'activeOpMode', 'opModeState', 'snapTargetRadians'].includes(k)) return;
      if (typeof v === 'object') return;
      if (!processed['General']) processed['General'] = {};
      processed['General'][k] = String(v);
    });

    // 3. Apply custom overrides
    const final: Record<string, Record<string, string>> = {};
    const movedKeys = new Set<string>();

    Object.entries(processed).forEach(([bundleName, content]) => {
      Object.entries(content).forEach(([k, v]) => {
        const customTarget = customGrouping[k];
        if (customTarget) {
          if (!final[customTarget]) final[customTarget] = {};
          final[customTarget][k] = v;
          movedKeys.add(k);
        } else {
          if (!final[bundleName]) final[bundleName] = {};
          final[bundleName][k] = v;
        }
      });
    });

    return final;
  }, [data, customGrouping]);

  const allKeys = useMemo(() => {
    const keys = new Set<string>();
    const rawBundles = (data.opModeTelemetry as Record<string, Record<string, string>>) || {};
    Object.values(rawBundles).forEach(c => Object.keys(c).forEach(k => keys.add(k)));
    Object.keys(data).forEach(k => {
      if (k !== 'opModeTelemetry' && typeof data[k] !== 'object') keys.add(k);
    });
    return Array.from(keys).sort();
  }, [data]);

  return (
    <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <div style={{ display: 'flex', justifyContent: 'flex-end', padding: '0.2rem 0.5rem', gap: '0.5rem' }}>
        <button
          onClick={onDownloadCSV}
          title="Download Background Telemetry History (CSV)"
          style={{ background: 'none', border: 'none', color: 'var(--text-dim)', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '4px' }}
        >
          <Download size={12} />
        </button>
        <button
          onClick={() => setShowSettings(!showSettings)}
          title="Custom Grouping Settings"
          style={{ background: 'none', border: 'none', color: 'var(--text-dim)', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '4px' }}
        >
          <Settings2 size={12} />
        </button>
      </div>

      <div style={{ flex: 1, overflow: 'auto', padding: '0 0.25rem' }}>
        {Object.entries(bundles).sort().map(([name, content]) => (
          <div key={name} style={{ marginBottom: '2px' }}>
            <div
              onClick={() => toggleBundle(name)}
              style={{
                padding: '0.35rem 0.5rem',
                backgroundColor: 'rgba(255,255,255,0.03)',
                borderRadius: '4px',
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                cursor: 'pointer',
                userSelect: 'none',
              }}
            >
              {collapsed[name] ? <ChevronRight size={10} color="var(--text-dim)" /> : <ChevronDown size={10} color="var(--text-dim)" />}
              <span style={{ fontSize: '0.6rem', fontWeight: 800, color: 'var(--text-dim)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                {name}
              </span>
              <span style={{ marginLeft: 'auto', fontSize: '0.55rem', color: 'rgba(255,255,255,0.15)' }}>{Object.keys(content).length} keys</span>
            </div>

            {!collapsed[name] && (
              <table className="telem-table" style={{ marginLeft: '12px', width: 'calc(100% - 12px)' }}>
                <tbody>
                  {Object.entries(content).map(([k, v]) => (
                    <tr key={k}>
                      <td className="telem-key" style={{ fontSize: '0.6rem' }}>{k}</td>
                      <td className="telem-val" style={{ fontSize: '0.6rem' }}>{v}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        ))}

        {Object.keys(bundles).length === 0 && (
          <div style={{ padding: '1rem', color: 'var(--text-dim)', fontSize: '0.7rem', textAlign: 'center' }}>No telemetry data</div>
        )}
      </div>

      {/* Settings Modal */}
      {showSettings && (
        <div style={{
          position: 'absolute', top: 0, left: 0, right: 0, bottom: 0,
          backgroundColor: 'rgba(0,0,0,0.85)', backdropFilter: 'blur(8px)',
          zIndex: 1000, display: 'flex', flexDirection: 'column', padding: '1rem',
          borderRadius: 'var(--radius)',
        }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '1rem' }}>
            <h3 style={{ fontSize: '0.8rem', margin: 0, color: 'var(--accent)' }}>Custom Telemetry Grouping</h3>
            <button onClick={() => setShowSettings(false)} style={{ background: 'none', border: 'none', color: 'white', cursor: 'pointer' }}><X size={16} /></button>
          </div>

          <div style={{ flex: 1, overflow: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr style={{ color: 'var(--text-dim)', fontSize: '0.6rem', textAlign: 'left' }}>
                  <th style={{ padding: '0.5rem' }}>KEY</th>
                  <th style={{ padding: '0.5rem' }}>TARGET BUNDLE</th>
                  <th style={{ padding: '0.5rem' }}></th>
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
                        style={{
                          backgroundColor: 'rgba(255,255,255,0.05)',
                          border: '1px solid rgba(255,255,255,0.1)',
                          borderRadius: '4px',
                          color: 'white',
                          fontSize: '0.65rem',
                          padding: '0.2rem 0.4rem',
                          width: '100%',
                          outline: 'none',
                        }}
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
  const recorderRef = useRef(new SessionRecorder());
  const historyRef = useRef<Record<string, unknown>[]>([]);

  const handleMessage = useCallback((msg: Record<string, unknown>) => {
    const type = msg.type as string;
    const data = msg.data as Record<string, unknown>;
    switch (type) {
      case 'state':
        setTelemetry(data);
        
        // Background continuous recording (keep last ~5 minutes at 50Hz)
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
  }, []);

  const downloadTelemetryCSV = useCallback(() => {
    if (historyRef.current.length === 0) {
      alert("No telemetry data to download yet.");
      return;
    }
    
    // Convert background history to a temporary Session to use the existing CSV exporter
    const frames = historyRef.current.map((data, i) => ({ timestampMs: i * 20, data }));
    const dummySession = { name: 'telemetry_export', date: Date.now(), frames };
    const csv = SessionRecorder.exportCSV(dummySession);
    
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `SwerveScope_Telemetry_${new Date().toISOString().replace(/[:.]/g, '-')}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }, []);

  const { mode, ws, connected, setManualMode } = useConnection(handleMessage);
  const { bindings, updateBindings } = useGamepad(ws, connected);

  const handleReplayFrame = useCallback((data: Record<string, unknown> | null) => {
    if (data) setTelemetry(data);
  }, []);

  return (
    <div style={{ height: '100vh', width: '100vw', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>

      {/* ═══ Header ═══ */}
      <header className="glass-panel" style={{
        margin: '0.5rem 0.5rem 0',
        padding: '0.5rem 1.25rem',
        display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexShrink: 0,
        borderRadius: 'var(--radius)',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <MonitorPlay style={{ color: 'var(--accent)', width: 20, height: 20 }} />
          <span style={{ fontSize: '1.05rem', fontWeight: 700, letterSpacing: '0.02em' }}>SwerveScope</span>

          <div style={{ width: 1, height: 20, backgroundColor: 'var(--border)', margin: '0 0.25rem' }} />

          {/* Mode Toggle */}
          <div style={{ display: 'flex', gap: '2px', backgroundColor: 'rgba(255,255,255,0.04)', borderRadius: '8px', padding: '2px' }}>
            <button onClick={() => setManualMode('sim')} style={{
              ...modeBtn,
              backgroundColor: mode === 'sim' ? 'var(--accent-soft)' : 'transparent',
              color: mode === 'sim' ? '#b388ff' : 'var(--text-dim)',
            }}>
              <Monitor size={11} /> Sim
            </button>
            <button onClick={() => setManualMode('robot')} style={{
              ...modeBtn,
              backgroundColor: mode === 'robot' ? 'rgba(0,230,118,0.15)' : 'transparent',
              color: mode === 'robot' ? '#00e676' : 'var(--text-dim)',
            }}>
              <Radio size={11} /> Robot
            </button>
          </div>

          {/* Connection Pill */}
          <span style={{
            display: 'inline-flex', alignItems: 'center', gap: '0.3rem',
            fontSize: '0.7rem', padding: '0.15rem 0.55rem', borderRadius: 9999,
            backgroundColor: connected ? 'rgba(0,230,118,0.08)' : 'rgba(255,82,82,0.08)',
            color: connected ? 'var(--success)' : 'var(--danger)',
            border: `1px solid ${connected ? 'rgba(0,230,118,0.15)' : 'rgba(255,82,82,0.15)'}`,
            fontWeight: 600, animation: connected ? 'pulse 2s infinite' : 'none',
          }}>
            {connected ? <Wifi size={11} /> : <WifiOff size={11} />}
            {connected ? mode.toUpperCase() : 'OFFLINE'}
          </span>

          <div style={{ width: 1, height: 20, backgroundColor: 'var(--border)', margin: '0 0.25rem' }} />

          <OpModeSelector availableOpModes={availableOpModes} activeOpMode={activeOpMode} opModeState={opModeState} ws={ws} />
        </div>
      </header>

      {/* ═══ Dashboard Body ═══ */}
      <div style={{ flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column', gap: '0.5rem', padding: '0.5rem' }}>

        {/* Row 1: Field View */}
        <div className="glass-panel" style={{ flex: '1.3 1 0%', display: 'flex', flexDirection: 'column', minHeight: 0, overflow: 'hidden' }}>
          <div className="panel-header">Field View</div>
          <div style={{ flex: 1, minHeight: 0 }}>
            <FieldView telemetry={telemetry} />
          </div>
        </div>

        {/* Row 2: Bottom panels */}
        <div style={{ flex: '1 1 0%', minHeight: 0, display: 'flex', gap: '0.5rem' }}>

          {/* Graph */}
          <div className="glass-panel" style={{ flex: '1.2 1 0%', display: 'flex', flexDirection: 'column', minHeight: 0, overflow: 'hidden' }}>
            <div className="panel-header">Telemetry Graph</div>
            <div style={{ flex: 1, minHeight: 0 }}>
              <GraphView telemetry={telemetry} />
            </div>
          </div>

          {/* Right stack */}
          <div style={{ flex: '1 1 0%', display: 'flex', flexDirection: 'column', gap: '0.5rem', minHeight: 0 }}>

            {/* Gamepad */}
            <div className="glass-panel" style={{ flex: '0.8 1 0%', display: 'flex', flexDirection: 'column', minHeight: 0, overflow: 'hidden' }}>
              <div style={{ flex: 1, minHeight: 0 }}>
                <GamepadView bindings={bindings} onUpdateBindings={updateBindings} />
              </div>
            </div>

            {/* Telemetry | Modules | Config */}
            <div style={{ flex: '1 1 0%', display: 'flex', gap: '0.5rem', minHeight: 0 }}>

              {/* Telemetry */}
              <div className="glass-panel" style={{ flex: '1 1 0%', display: 'flex', flexDirection: 'column', minHeight: 0, overflow: 'hidden', position: 'relative' }}>
                <div className="panel-header">Telemetry</div>
                <div style={{ flex: 1, minHeight: 0 }}>
                  <TelemetryTable data={telemetry} onDownloadCSV={downloadTelemetryCSV} />
                </div>
              </div>

              {/* Modules */}
              <div className="glass-panel" style={{ flex: '1 1 0%', display: 'flex', flexDirection: 'column', minHeight: 0, overflow: 'hidden' }}>
                <div className="panel-header">Modules</div>
                <div style={{ flex: 1, minHeight: 0, padding: '0.35rem', overflow: 'auto' }}>
                  <ModuleGauges modules={(telemetry as Record<string, unknown>).modules as Record<string, unknown>[]} />
                </div>
              </div>

              {/* Config */}
              <div className="glass-panel" style={{ flex: '1 1 0%', display: 'flex', flexDirection: 'column', minHeight: 0, overflow: 'hidden' }}>
                <div className="panel-header">Config</div>
                <div style={{ flex: 1, minHeight: 0 }}>
                  <ConfigPanel config={config} ws={ws} />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* ═══ Timeline Bar ═══ */}
      <TimelineBar recorder={recorderRef.current} telemetry={telemetry} onReplayFrame={handleReplayFrame} />
    </div>
  );
}

const modeBtn: React.CSSProperties = {
  display: 'flex', alignItems: 'center', gap: '4px',
  padding: '0.2rem 0.5rem', fontSize: '0.6rem', fontWeight: 600,
  border: 'none', borderRadius: '6px', cursor: 'pointer',
  background: 'none', transition: 'all 0.15s ease',
};

export default App;
