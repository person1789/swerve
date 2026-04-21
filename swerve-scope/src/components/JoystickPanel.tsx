// JoystickPanel.tsx — SVG gamepad visualizer (axes + buttons)
import { useEffect, useRef, useState } from 'react';
import type { TelemetryFrame } from '../types/telemetry';

interface JoystickPanelProps {
  frame: TelemetryFrame | null;
}

// ── Stick SVG ───────────────────────────────────────────────────────────────
function Stick({
  label, x, y, color = '#0ea5e9',
}: {
  label: string; x: number; y: number; color?: string;
}) {
  const CX = 50, CY = 50, R = 38;
  const dotX = CX + x * R;
  const dotY = CY - y * R; // invert Y so up=positive

  return (
    <div className="flex flex-col items-center gap-1">
      <svg width="100" height="100" viewBox="0 0 100 100">
        {/* Background */}
        <circle cx={CX} cy={CY} r={R + 2} fill="#0d1520" stroke="#1a2535" strokeWidth="1" />
        {/* Dead zone ring */}
        <circle cx={CX} cy={CY} r={R * 0.07} fill="none" stroke="#2a3a52" strokeWidth="1" />
        {/* Cross-hair */}
        <line x1={CX - R} y1={CY} x2={CX + R} y2={CY} stroke="#1a2535" strokeWidth="0.5" />
        <line x1={CX} y1={CY - R} x2={CX} y2={CY + R} stroke="#1a2535" strokeWidth="0.5" />
        {/* Magnitude ring */}
        {(() => {
          const mag = Math.hypot(x, y);
          if (mag > 0.05) {
            return (
              <circle
                cx={CX} cy={CY}
                r={mag * R}
                fill="none"
                stroke={color + '30'}
                strokeWidth="0.5"
              />
            );
          }
          return null;
        })()}
        {/* Trail dot (dim) */}
        <circle cx={CX + x * R} cy={CY - y * R} r={3} fill={color + '20'} />
        {/* Thumb dot */}
        <circle cx={dotX} cy={dotY} r={5} fill={color} className="joy-dot" />
        <circle cx={dotX} cy={dotY} r={5} fill="none" stroke={color + '60'} strokeWidth="1.5" />
      </svg>
      <span className="text-[8px] font-mono text-scope-muted">{label}</span>
      <span className="text-[8px] font-mono readout text-scope-text">
        {x.toFixed(2)}, {y.toFixed(2)}
      </span>
    </div>
  );
}

// ── Trigger bar ─────────────────────────────────────────────────────────────
function Trigger({ label, value }: { label: string; value: number }) {
  return (
    <div className="flex flex-col items-center gap-0.5">
      <span className="text-[8px] font-mono text-scope-muted">{label}</span>
      <div className="w-5 h-16 bg-scope-surface border border-scope-border rounded relative overflow-hidden">
        <div
          className="absolute bottom-0 left-0 right-0 bg-scope-accent transition-all duration-75 rounded"
          style={{ height: `${value * 100}%` }}
        />
      </div>
      <span className="text-[8px] font-mono readout text-scope-text">{value.toFixed(2)}</span>
    </div>
  );
}

// ── Button grid ──────────────────────────────────────────────────────────────
const BUTTON_LABELS = [
  'A','B','X','Y',         // 0-3
  'LB','RB',               // 4-5
  'LT','RT',               // 6-7
  'Select','Start',        // 8-9
  'LS','RS',               // 10-11
  '↑','↓','←','→',        // 12-15
];

function ButtonGrid({ buttons }: { buttons: boolean[] }) {
  return (
    <div className="flex flex-wrap gap-1 justify-center">
      {BUTTON_LABELS.map((label, i) => (
        <div
          key={i}
          className="w-8 h-6 flex items-center justify-center rounded text-[8px] font-mono border transition-all duration-75"
          style={{
            background:   buttons[i] ? '#0ea5e930' : 'transparent',
            borderColor:  buttons[i] ? '#0ea5e9'   : '#1a2535',
            color:        buttons[i] ? '#0ea5e9'   : '#2a3a52',
          }}
        >
          {label}
        </div>
      ))}
    </div>
  );
}

// ── Main component ───────────────────────────────────────────────────────────
export default function JoystickPanel({ frame }: JoystickPanelProps) {
  const [gp, setGp] = useState<Gamepad | null>(null);
  const [lx, setLx] = useState(0); const [ly, setLy] = useState(0);
  const [rx, setRx] = useState(0); const [ry, setRy] = useState(0);
  const [lt, setLt] = useState(0); const [rt, setRt] = useState(0);
  const [btns, setBtns] = useState<boolean[]>([]);
  const rafRef = useRef<number>(0);

  useEffect(() => {
    const poll = () => {
      const gamepads = navigator.getGamepads();
      const g = gamepads[0] ?? gamepads[1];
      if (g) {
        const db = (v: number) => Math.abs(v) < 0.07 ? 0 : v;
        setGp(g);
        setLx(db(g.axes[0] ?? 0)); setLy(-db(g.axes[1] ?? 0));
        setRx(db(g.axes[2] ?? 0)); setRy(-db(g.axes[3] ?? 0));
        setLt(g.buttons[6]?.value ?? 0);
        setRt(g.buttons[7]?.value ?? 0);
        setBtns(Array.from(g.buttons).map(b => b.pressed));
      } else {
        // Fall back to showing frame inputs if no gamepad
        if (frame) {
          setLx(frame.driveX); setLy(frame.driveY);
          setRx(0); setRy(frame.turn);
        }
      }
      rafRef.current = requestAnimationFrame(poll);
    };
    rafRef.current = requestAnimationFrame(poll);
    return () => cancelAnimationFrame(rafRef.current);
  }, [frame]);

  return (
    <div className="flex flex-col h-full bg-scope-bg items-center justify-center gap-4 py-3 overflow-auto">
      {/* Connection status */}
      <div className="flex items-center gap-2 text-[9px] font-mono">
        <div className={`w-2 h-2 rounded-full ${gp ? 'dot-live' : 'dot-dead'}`} />
        <span className="text-scope-muted">
          {gp ? `${gp.id.slice(0, 28)}…` : 'No gamepad — showing frame inputs'}
        </span>
      </div>

      {/* Sticks + triggers row */}
      <div className="flex items-center gap-6">
        <Trigger label="LT" value={lt} />
        <Stick label="Left Stick" x={lx} y={ly} color="#0ea5e9" />
        <Stick label="Right Stick" x={rx} y={ry} color="#a855f7" />
        <Trigger label="RT" value={rt} />
      </div>

      {/* Button grid */}
      <ButtonGrid buttons={btns.length > 0 ? btns : new Array(16).fill(false)} />

      {/* Drive vector from frame */}
      {frame && (
        <div className="flex gap-6 text-[9px] font-mono text-scope-muted">
          <span>DRIVE_X: <span className="text-scope-text readout">{frame.driveX.toFixed(3)}</span></span>
          <span>DRIVE_Y: <span className="text-scope-text readout">{frame.driveY.toFixed(3)}</span></span>
          <span>TURN:    <span className="text-scope-text readout">{frame.turn.toFixed(3)}</span></span>
        </div>
      )}
    </div>
  );
}
