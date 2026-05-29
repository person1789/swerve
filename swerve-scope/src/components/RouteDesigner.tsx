import { useEffect, useMemo, useRef, useState } from 'react';
import { Copy, Plus, RotateCcw, Trash2 } from 'lucide-react';

interface Waypoint {
  id: string;
  xIn: number;
  yIn: number;
  headingDeg: number;
  waitForAzimuth: boolean;
  settleMs: number;
  timeoutMs: number;
}

interface StartPose {
  xIn: number;
  yIn: number;
  headingDeg: number;
}

type DragTarget = { type: 'start' } | { type: 'waypoint'; id: string };

const FIELD_SIZE_IN = 144;
const TILE_SIZE_IN = 24;
const DEFAULT_SETTLE_MS = 0;
const DEFAULT_TIMEOUT_MS = 2500;
const STORAGE_KEY = 'swervescope.decode-command-generator.v1';

const decodeStarts = [
  { label: 'Close Red', xIn: 120, yIn: 127.87, headingDeg: 319.6 },
  { label: 'Far Red', xIn: 89, yIn: 8, headingDeg: 0 },
  { label: 'Close Blue', xIn: 24, yIn: 127.87, headingDeg: 220.4 },
  { label: 'Far Blue', xIn: 55, yIn: 8, headingDeg: 180 },
];

const defaultWaypoints: Waypoint[] = [
  waypoint(86.72, 90, 0),
];

function waypoint(xIn: number, yIn: number, headingDeg: number): Waypoint {
  return {
    id: `w${Date.now()}-${Math.random().toString(16).slice(2)}`,
    xIn,
    yIn,
    headingDeg,
    waitForAzimuth: true,
    settleMs: DEFAULT_SETTLE_MS,
    timeoutMs: DEFAULT_TIMEOUT_MS,
  };
}

function clamp(value: number, min: number, max: number) {
  return Math.max(min, Math.min(max, value));
}

function fmt(value: number) {
  return Number(value.toFixed(3));
}

function normalizeHeading(headingDeg: number) {
  let heading = headingDeg % 360;
  if (heading < 0) heading += 360;
  return Number(heading.toFixed(2));
}

function snapToGrid(value: number) {
  return Math.round(value / TILE_SIZE_IN) * TILE_SIZE_IN;
}

function loadRoute() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    return JSON.parse(raw) as { start: StartPose; waypoints: Waypoint[] };
  } catch {
    return null;
  }
}

export function RouteDesigner() {
  const svgRef = useRef<SVGSVGElement>(null);
  const loaded = useMemo(() => loadRoute(), []);
  const [start, setStart] = useState<StartPose>(loaded?.start ?? decodeStarts[0]);
  const [waypoints, setWaypoints] = useState<Waypoint[]>(loaded?.waypoints ?? defaultWaypoints);
  const [dragTarget, setDragTarget] = useState<DragTarget | null>(null);
  const [snap, setSnap] = useState(true);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ start, waypoints }));
  }, [start, waypoints]);

  const generatedCode = useMemo(() => {
    const capacity = waypoints.length * 2 + 1;
    const lines = [
      `localizer.setPose(${fmt(start.xIn)}, ${fmt(start.yIn)}, Math.toRadians(${fmt(start.headingDeg)}));`,
      `DriveScheduler scheduler = new DriveScheduler(${capacity})`,
      ...waypoints.map((point, index) => {
        const terminator = index === waypoints.length - 1 ? '' : '';
        return `        .addMoveToPose(${fmt(point.xIn)}, ${fmt(point.yIn)}, Math.toRadians(${fmt(point.headingDeg)}), ${point.waitForAzimuth}, ${fmt(point.settleMs)}, ${fmt(point.timeoutMs)})${terminator}`;
      }),
      '        .add(new StopDriveCommand());',
    ];
    return lines.join('\n');
  }, [start, waypoints]);

  const routeDistance = useMemo(() => {
    let distance = 0;
    let previous = start;
    for (const point of waypoints) {
      distance += Math.hypot(point.xIn - previous.xIn, point.yIn - previous.yIn);
      previous = point;
    }
    return distance;
  }, [start, waypoints]);

  const toSvg = (xIn: number, yIn: number) => ({
    x: (xIn / FIELD_SIZE_IN) * 100,
    y: 100 - (yIn / FIELD_SIZE_IN) * 100,
  });

  const fromSvg = (xPct: number, yPct: number) => {
    let xIn = clamp((xPct / 100) * FIELD_SIZE_IN, 0, FIELD_SIZE_IN);
    let yIn = clamp(((100 - yPct) / 100) * FIELD_SIZE_IN, 0, FIELD_SIZE_IN);
    if (snap) {
      xIn = clamp(snapToGrid(xIn), 0, FIELD_SIZE_IN);
      yIn = clamp(snapToGrid(yIn), 0, FIELD_SIZE_IN);
    }
    return { xIn, yIn };
  };

  const pointerToField = (event: React.PointerEvent<SVGSVGElement>) => {
    const svg = svgRef.current;
    if (!svg) return { xIn: 0, yIn: 0 };
    const point = svg.createSVGPoint();
    point.x = event.clientX;
    point.y = event.clientY;
    const ctm = svg.getScreenCTM();
    if (!ctm) return { xIn: 0, yIn: 0 };
    const local = point.matrixTransform(ctm.inverse());
    return fromSvg(local.x, local.y);
  };

  const moveDragTarget = (event: React.PointerEvent<SVGSVGElement>) => {
    if (!dragTarget) return;
    const { xIn, yIn } = pointerToField(event);
    if (dragTarget.type === 'start') {
      setStart(prev => ({ ...prev, xIn, yIn }));
      return;
    }
    setWaypoints(prev => prev.map(point => (
      point.id === dragTarget.id ? { ...point, xIn, yIn } : point
    )));
  };

  const addWaypointFromField = (event: React.MouseEvent<SVGSVGElement>) => {
    if (event.defaultPrevented || dragTarget) return;
    const svg = svgRef.current;
    if (!svg) return;
    const point = svg.createSVGPoint();
    point.x = event.clientX;
    point.y = event.clientY;
    const ctm = svg.getScreenCTM();
    if (!ctm) return;
    const local = point.matrixTransform(ctm.inverse());
    if (local.x < 0 || local.x > 100 || local.y < 0 || local.y > 100) return;
    const { xIn, yIn } = fromSvg(local.x, local.y);
    const previous = waypoints[waypoints.length - 1] ?? start;
    const headingDeg = normalizeHeading((Math.atan2(yIn - previous.yIn, xIn - previous.xIn) * 180) / Math.PI);
    setWaypoints(prev => [...prev, waypoint(xIn, yIn, headingDeg)]);
  };

  const beginDrag = (event: React.PointerEvent<SVGElement>, target: DragTarget) => {
    event.preventDefault();
    event.stopPropagation();
    svgRef.current?.setPointerCapture(event.pointerId);
    setDragTarget(target);
  };

  const updateWaypoint = (id: string, patch: Partial<Waypoint>) => {
    setWaypoints(prev => prev.map(point => point.id === id ? { ...point, ...patch } : point));
  };

  const copyCommands = async () => {
    await navigator.clipboard.writeText(generatedCode);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1200);
  };

  const reset = () => {
    setStart(decodeStarts[0]);
    setWaypoints(defaultWaypoints.map(point => ({ ...point, id: `w${Date.now()}` })));
  };

  const startSvg = toSvg(start.xIn, start.yIn);

  return (
    <div style={shellStyle}>
      <section style={toolbarStyle}>
        <div>
          <h2 style={titleStyle}>DECODE Straight-Line Command Generator</h2>
          <p style={subtleStyle}>Click the 2025-2026 DECODE field to add waypoints. Drag dots to edit. Copy output straight into the scheduler.</p>
        </div>
        <div style={buttonRowStyle}>
          <select
            value={`${start.xIn},${start.yIn},${start.headingDeg}`}
            onChange={(event) => {
              const [xIn, yIn, headingDeg] = event.target.value.split(',').map(Number);
              setStart({ xIn, yIn, headingDeg });
            }}
            style={selectStyle}
          >
            {decodeStarts.map(option => (
              <option key={option.label} value={`${option.xIn},${option.yIn},${option.headingDeg}`}>
                {option.label}
              </option>
            ))}
          </select>
          <label style={checkStyle}>
            <input type="checkbox" checked={snap} onChange={event => setSnap(event.target.checked)} />
            snap 24 in
          </label>
          <button onClick={() => setWaypoints(prev => [...prev, waypoint(72, 72, 0)])} style={buttonStyle}>
            <Plus size={14} /> Add
          </button>
          <button onClick={copyCommands} style={buttonStyle}>
            <Copy size={14} /> {copied ? 'Copied' : 'Copy'}
          </button>
          <button onClick={reset} style={buttonStyle}>
            <RotateCcw size={14} /> Reset
          </button>
        </div>
      </section>

      <section style={contentStyle}>
        <div style={fieldCardStyle}>
          <svg
            ref={svgRef}
            viewBox="-4 -4 108 108"
            style={fieldStyle}
            onClick={addWaypointFromField}
            onPointerMove={moveDragTarget}
            onPointerUp={(event) => {
              if (svgRef.current?.hasPointerCapture(event.pointerId)) {
                svgRef.current.releasePointerCapture(event.pointerId);
              }
              setDragTarget(null);
            }}
            onPointerLeave={() => setDragTarget(null)}
          >
            <defs>
              <pattern id="tile-grid" width="16.6667" height="16.6667" patternUnits="userSpaceOnUse">
                <path d="M 16.6667 0 L 0 0 0 16.6667" fill="none" stroke="rgba(33, 75, 92, 0.42)" strokeWidth="0.32" />
              </pattern>
              <linearGradient id="field-fill" x1="0" x2="1" y1="0" y2="1">
                <stop offset="0%" stopColor="#f4ead8" />
                <stop offset="100%" stopColor="#d8c39b" />
              </linearGradient>
            </defs>

            <rect x="0" y="0" width="100" height="100" rx="1.5" fill="url(#field-fill)" />
            <rect x="0" y="0" width="100" height="100" fill="url(#tile-grid)" />
            <rect x="0" y="0" width="100" height="100" fill="none" stroke="#214b5c" strokeWidth="1.1" />

            <DecodeFieldElements />

            <RouteLines start={start} waypoints={waypoints} toSvg={toSvg} />

            <circle
              cx={startSvg.x}
              cy={startSvg.y}
              r="2.5"
              fill="#0f766e"
              stroke="#ffffff"
              strokeWidth="0.7"
              onPointerDown={event => beginDrag(event, { type: 'start' })}
            />
            <PoseArrow pose={start} toSvg={toSvg} color="#0f766e" />
            <text x={startSvg.x + 2.8} y={startSvg.y - 2.4} fontSize="2.6" fill="#0f172a" fontWeight="800">START</text>

            {waypoints.map((point, index) => {
              const svg = toSvg(point.xIn, point.yIn);
              return (
                <g key={point.id}>
                  <circle
                    cx={svg.x}
                    cy={svg.y}
                    r="2.35"
                    fill="#d9480f"
                    stroke="#ffffff"
                    strokeWidth="0.7"
                    onPointerDown={event => beginDrag(event, { type: 'waypoint', id: point.id })}
                  />
                  <PoseArrow pose={point} toSvg={toSvg} color="#d9480f" />
                  <text x={svg.x + 2.6} y={svg.y - 2.1} fontSize="2.7" fill="#0f172a" fontWeight="900">{index + 1}</text>
                </g>
              );
            })}
          </svg>
        </div>

        <aside style={sideStyle}>
          <div style={summaryStyle}>
            <span>{waypoints.length} commands</span>
            <span>{routeDistance.toFixed(1)} in total</span>
          </div>

          <div style={editorListStyle}>
            <PoseEditor label="Start" pose={start} onChange={setStart} />
            {waypoints.map((point, index) => (
              <WaypointEditor
                key={point.id}
                index={index}
                point={point}
                onChange={patch => updateWaypoint(point.id, patch)}
                onDelete={() => setWaypoints(prev => prev.filter(candidate => candidate.id !== point.id))}
              />
            ))}
          </div>

          <textarea readOnly value={generatedCode} style={codeStyle} />
        </aside>
      </section>
    </div>
  );
}

function RouteLines({
  start,
  waypoints,
  toSvg,
}: {
  start: StartPose;
  waypoints: Waypoint[];
  toSvg: (xIn: number, yIn: number) => { x: number; y: number };
}) {
  let previous: StartPose | Waypoint = start;
  return (
    <g>
      {waypoints.map(point => {
        const a = toSvg(previous.xIn, previous.yIn);
        const b = toSvg(point.xIn, point.yIn);
        previous = point;
        return (
          <line
            key={point.id}
            x1={a.x}
            y1={a.y}
            x2={b.x}
            y2={b.y}
            stroke="#1d4ed8"
            strokeWidth="0.9"
            strokeLinecap="round"
          />
        );
      })}
    </g>
  );
}

function DecodeFieldElements() {
  return (
    <g pointerEvents="none">
      <rect x="5" y="5" width="18" height="12" rx="2" fill="rgba(220, 38, 38, 0.16)" stroke="#b91c1c" strokeWidth="0.5" />
      <text x="14" y="12.5" textAnchor="middle" fontSize="2.5" fill="#7f1d1d" fontWeight="800">RED GOAL</text>

      <rect x="77" y="83" width="18" height="12" rx="2" fill="rgba(37, 99, 235, 0.16)" stroke="#1d4ed8" strokeWidth="0.5" />
      <text x="86" y="90.5" textAnchor="middle" fontSize="2.5" fill="#1e3a8a" fontWeight="800">BLUE GOAL</text>

      <path d="M 7 38 L 23 31 L 23 55 L 7 48 Z" fill="rgba(220, 38, 38, 0.12)" stroke="#b91c1c" strokeWidth="0.5" />
      <text x="15" y="44" textAnchor="middle" fontSize="2.35" fill="#7f1d1d" fontWeight="800">CLASSIFIER</text>

      <path d="M 93 62 L 77 69 L 77 45 L 93 52 Z" fill="rgba(37, 99, 235, 0.12)" stroke="#1d4ed8" strokeWidth="0.5" />
      <text x="85" y="57" textAnchor="middle" fontSize="2.35" fill="#1e3a8a" fontWeight="800">CLASSIFIER</text>

      <polygon points="50,43 56,50 50,57 44,50" fill="rgba(120, 113, 108, 0.34)" stroke="#44403c" strokeWidth="0.55" />
      <text x="50" y="50.9" textAnchor="middle" fontSize="2.2" fill="#1c1917" fontWeight="900">OBELISK</text>

      {[29, 39, 49].map(x => (
        <g key={`top-spike-${x}`}>
          <circle cx={x} cy="22" r="1.5" fill="#6d28d9" />
          <circle cx={x + 4} cy="22" r="1.5" fill="#16a34a" />
          <circle cx={x + 8} cy="22" r="1.5" fill="#6d28d9" />
        </g>
      ))}
      {[43, 53, 63].map(x => (
        <g key={`bottom-spike-${x}`}>
          <circle cx={x} cy="78" r="1.5" fill="#16a34a" />
          <circle cx={x + 4} cy="78" r="1.5" fill="#6d28d9" />
          <circle cx={x + 8} cy="78" r="1.5" fill="#6d28d9" />
        </g>
      ))}

      <text x="50" y="-1.3" textAnchor="middle" fontSize="2.5" fill="#214b5c" fontWeight="900">FTC 2025-2026 DECODE ONLY</text>
      <text x="50" y="103" textAnchor="middle" fontSize="2.2" fill="#214b5c">Coordinates are 0-144 inches, matching the current auto constants.</text>
    </g>
  );
}

function PoseArrow({
  pose,
  toSvg,
  color,
}: {
  pose: StartPose | Waypoint;
  toSvg: (xIn: number, yIn: number) => { x: number; y: number };
  color: string;
}) {
  const origin = toSvg(pose.xIn, pose.yIn);
  const heading = (pose.headingDeg * Math.PI) / 180;
  const tip = {
    x: origin.x + Math.cos(heading) * 6,
    y: origin.y - Math.sin(heading) * 6,
  };
  return (
    <line
      x1={origin.x}
      y1={origin.y}
      x2={tip.x}
      y2={tip.y}
      stroke={color}
      strokeWidth="0.85"
      strokeLinecap="round"
    />
  );
}

function PoseEditor({
  label,
  pose,
  onChange,
}: {
  label: string;
  pose: StartPose;
  onChange: (pose: StartPose) => void;
}) {
  return (
    <div style={cardStyle}>
      <strong style={smallTitleStyle}>{label}</strong>
      <NumberInput label="X" value={pose.xIn} onChange={xIn => onChange({ ...pose, xIn })} />
      <NumberInput label="Y" value={pose.yIn} onChange={yIn => onChange({ ...pose, yIn })} />
      <NumberInput label="Heading" value={pose.headingDeg} onChange={headingDeg => onChange({ ...pose, headingDeg: normalizeHeading(headingDeg) })} />
    </div>
  );
}

function WaypointEditor({
  index,
  point,
  onChange,
  onDelete,
}: {
  index: number;
  point: Waypoint;
  onChange: (patch: Partial<Waypoint>) => void;
  onDelete: () => void;
}) {
  return (
    <div style={cardStyle}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <strong style={smallTitleStyle}>Command {index + 1}</strong>
        <button onClick={onDelete} style={iconButtonStyle}><Trash2 size={13} /></button>
      </div>
      <NumberInput label="X" value={point.xIn} onChange={xIn => onChange({ xIn })} />
      <NumberInput label="Y" value={point.yIn} onChange={yIn => onChange({ yIn })} />
      <NumberInput label="Heading" value={point.headingDeg} onChange={headingDeg => onChange({ headingDeg: normalizeHeading(headingDeg) })} />
      <NumberInput label="Settle ms" value={point.settleMs} onChange={settleMs => onChange({ settleMs })} />
      <NumberInput label="Timeout ms" value={point.timeoutMs} onChange={timeoutMs => onChange({ timeoutMs })} />
      <label style={checkStyle}>
        <input
          type="checkbox"
          checked={point.waitForAzimuth}
          onChange={event => onChange({ waitForAzimuth: event.target.checked })}
        />
        wait for azimuth
      </label>
    </div>
  );
}

function NumberInput({
  label,
  value,
  onChange,
}: {
  label: string;
  value: number;
  onChange: (value: number) => void;
}) {
  return (
    <label style={labelStyle}>
      <span>{label}</span>
      <input
        type="number"
        value={value}
        onChange={event => onChange(Number(event.target.value))}
        style={inputStyle}
      />
    </label>
  );
}

const shellStyle: React.CSSProperties = {
  height: '100%',
  display: 'flex',
  flexDirection: 'column',
  gap: '0.65rem',
  padding: '0.65rem',
  background: 'radial-gradient(circle at top left, rgba(33,75,92,0.18), transparent 34%), #10151d',
};

const toolbarStyle: React.CSSProperties = {
  display: 'flex',
  justifyContent: 'space-between',
  alignItems: 'center',
  gap: '0.75rem',
  flexWrap: 'wrap',
};

const titleStyle: React.CSSProperties = {
  margin: 0,
  fontSize: '1rem',
  color: '#f8fafc',
  letterSpacing: '0.01em',
};

const subtleStyle: React.CSSProperties = {
  margin: '0.15rem 0 0',
  color: 'rgba(226,232,240,0.68)',
  fontSize: '0.68rem',
};

const buttonRowStyle: React.CSSProperties = {
  display: 'flex',
  alignItems: 'center',
  gap: '0.45rem',
  flexWrap: 'wrap',
};

const contentStyle: React.CSSProperties = {
  minHeight: 0,
  flex: 1,
  display: 'grid',
  gridTemplateColumns: 'minmax(360px, 1.15fr) minmax(320px, 0.85fr)',
  gap: '0.75rem',
};

const fieldCardStyle: React.CSSProperties = {
  minHeight: 0,
  borderRadius: 18,
  overflow: 'hidden',
  border: '1px solid rgba(148,163,184,0.16)',
  background: 'rgba(15,23,42,0.72)',
  boxShadow: '0 20px 70px rgba(0,0,0,0.32)',
};

const fieldStyle: React.CSSProperties = {
  width: '100%',
  height: '100%',
  display: 'block',
  cursor: 'crosshair',
};

const sideStyle: React.CSSProperties = {
  minHeight: 0,
  display: 'grid',
  gridTemplateRows: 'auto minmax(0, 1fr) 220px',
  gap: '0.6rem',
};

const summaryStyle: React.CSSProperties = {
  display: 'flex',
  justifyContent: 'space-between',
  gap: '0.5rem',
  color: '#dbeafe',
  fontSize: '0.72rem',
  fontWeight: 800,
  padding: '0.65rem',
  borderRadius: 14,
  background: 'rgba(37,99,235,0.16)',
  border: '1px solid rgba(96,165,250,0.18)',
};

const editorListStyle: React.CSSProperties = {
  minHeight: 0,
  overflow: 'auto',
  display: 'grid',
  alignContent: 'start',
  gap: '0.55rem',
};

const cardStyle: React.CSSProperties = {
  display: 'grid',
  gridTemplateColumns: 'repeat(3, minmax(0, 1fr))',
  gap: '0.45rem',
  padding: '0.65rem',
  borderRadius: 14,
  background: 'rgba(255,255,255,0.045)',
  border: '1px solid rgba(255,255,255,0.075)',
};

const smallTitleStyle: React.CSSProperties = {
  gridColumn: '1 / -1',
  color: '#f8fafc',
  fontSize: '0.72rem',
};

const labelStyle: React.CSSProperties = {
  display: 'grid',
  gap: '0.2rem',
  color: 'rgba(226,232,240,0.62)',
  fontSize: '0.58rem',
  fontWeight: 800,
  textTransform: 'uppercase',
  letterSpacing: '0.04em',
};

const inputStyle: React.CSSProperties = {
  minWidth: 0,
  width: '100%',
  boxSizing: 'border-box',
  background: 'rgba(15,23,42,0.78)',
  color: '#f8fafc',
  border: '1px solid rgba(148,163,184,0.18)',
  borderRadius: 8,
  padding: '0.42rem 0.48rem',
  fontSize: '0.68rem',
  outline: 'none',
};

const selectStyle: React.CSSProperties = {
  ...inputStyle,
  width: 150,
};

const checkStyle: React.CSSProperties = {
  display: 'inline-flex',
  alignItems: 'center',
  gap: '0.35rem',
  color: 'rgba(226,232,240,0.78)',
  fontSize: '0.66rem',
  fontWeight: 700,
};

const buttonStyle: React.CSSProperties = {
  display: 'inline-flex',
  alignItems: 'center',
  gap: '0.35rem',
  border: '1px solid rgba(34,197,94,0.22)',
  borderRadius: 10,
  background: 'rgba(34,197,94,0.13)',
  color: '#bbf7d0',
  padding: '0.46rem 0.62rem',
  fontSize: '0.68rem',
  fontWeight: 850,
  cursor: 'pointer',
};

const iconButtonStyle: React.CSSProperties = {
  ...buttonStyle,
  padding: '0.25rem',
  justifyContent: 'center',
};

const codeStyle: React.CSSProperties = {
  width: '100%',
  height: '100%',
  resize: 'none',
  boxSizing: 'border-box',
  border: '1px solid rgba(148,163,184,0.16)',
  borderRadius: 16,
  background: 'rgba(2,6,23,0.82)',
  color: '#dbeafe',
  padding: '0.8rem',
  fontFamily: 'ui-monospace, SFMono-Regular, Menlo, Consolas, monospace',
  fontSize: '0.7rem',
  lineHeight: 1.5,
  outline: 'none',
};
