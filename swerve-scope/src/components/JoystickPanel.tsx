import { useEffect, useRef, useState } from 'react';
import type { TelemetryFrame } from '../types/telemetry';

interface JoystickPanelProps {
  frame: TelemetryFrame | null;
}

function Stick({ label, x, y, color = '#0ea5e9' }: { label: string; x: number; y: number; color?: string }) {
  const cx = 50;
  const cy = 50;
  const r = 38;
  const dotX = cx + x * r;
  const dotY = cy - y * r;

  return (
    <div className="flex flex-col items-center gap-1">
      <svg width="100" height="100" viewBox="0 0 100 100">
        <circle cx={cx} cy={cy} r={r + 2} fill="#0d1520" stroke="#1a2535" strokeWidth="1" />
        <circle cx={cx} cy={cy} r={r * 0.07} fill="none" stroke="#2a3a52" strokeWidth="1" />
        <line x1={cx - r} y1={cy} x2={cx + r} y2={cy} stroke="#1a2535" strokeWidth="0.5" />
        <line x1={cx} y1={cy - r} x2={cx} y2={cy + r} stroke="#1a2535" strokeWidth="0.5" />
        {Math.hypot(x, y) > 0.05 && (
          <circle cx={cx} cy={cy} r={Math.hypot(x, y) * r} fill="none" stroke={`${color}30`} strokeWidth="0.5" />
        )}
        <circle cx={cx + x * r} cy={cy - y * r} r={3} fill={`${color}20`} />
        <circle cx={dotX} cy={dotY} r={5} fill={color} className="joy-dot" />
        <circle cx={dotX} cy={dotY} r={5} fill="none" stroke={`${color}60`} strokeWidth="1.5" />
      </svg>
      <span className="text-[8px] font-mono text-scope-muted">{label}</span>
      <span className="text-[8px] font-mono readout text-scope-text">
        {x.toFixed(2)}, {y.toFixed(2)}
      </span>
    </div>
  );
}

function Trigger({ label, value }: { label: string; value: number }) {
  return (
    <div className="flex flex-col items-center gap-0.5">
      <span className="text-[8px] font-mono text-scope-muted">{label}</span>
      <div className="w-5 h-16 bg-scope-surface border border-scope-border rounded relative overflow-hidden">
        <div className="absolute bottom-0 left-0 right-0 bg-scope-accent transition-all duration-75 rounded" style={{ height: `${value * 100}%` }} />
      </div>
      <span className="text-[8px] font-mono readout text-scope-text">{value.toFixed(2)}</span>
    </div>
  );
}

const BUTTON_LABELS = ['A', 'B', 'X', 'Y', 'LB', 'RB', 'LT', 'RT', 'Select', 'Start', 'LS', 'RS', 'Up', 'Down', 'Left', 'Right'];

function ButtonGrid({ buttons }: { buttons: boolean[] }) {
  return (
    <div className="flex flex-wrap gap-1 justify-center">
      {BUTTON_LABELS.map((label, index) => (
        <div
          key={index}
          className="w-8 h-6 flex items-center justify-center rounded text-[8px] font-mono border transition-all duration-75"
          style={{
            background: buttons[index] ? '#0ea5e930' : 'transparent',
            borderColor: buttons[index] ? '#0ea5e9' : '#1a2535',
            color: buttons[index] ? '#0ea5e9' : '#2a3a52',
          }}
        >
          {label}
        </div>
      ))}
    </div>
  );
}

export default function JoystickPanel({ frame }: JoystickPanelProps) {
  const [gp, setGp] = useState<Gamepad | null>(null);
  const [lx, setLx] = useState(0);
  const [ly, setLy] = useState(0);
  const [rx, setRx] = useState(0);
  const [ry, setRy] = useState(0);
  const [lt, setLt] = useState(0);
  const [rt, setRt] = useState(0);
  const [buttons, setButtons] = useState<boolean[]>([]);
  const rafRef = useRef<number>(0);

  useEffect(() => {
    const poll = () => {
      const gamepads = navigator.getGamepads();
      const gamepad = gamepads[0] ?? gamepads[1];
      if (gamepad) {
        const deadband = (value: number) => Math.abs(value) < 0.07 ? 0 : value;
        setGp(gamepad);
        setLx(deadband(gamepad.axes[0] ?? 0));
        setLy(-deadband(gamepad.axes[1] ?? 0));
        setRx(deadband(gamepad.axes[2] ?? 0));
        setRy(-deadband(gamepad.axes[3] ?? 0));
        setLt(gamepad.buttons[6]?.value ?? 0);
        setRt(gamepad.buttons[7]?.value ?? 0);
        setButtons(Array.from(gamepad.buttons).map(button => button.pressed));
      } else if (frame) {
        setLx(frame.gamepad?.lx ?? frame.driveX);
        setLy(frame.gamepad?.ly ?? frame.driveY);
        setRx(frame.gamepad?.rx ?? 0);
        setRy(frame.gamepad?.ry ?? frame.turn);
        setLt(frame.gamepad?.lt ?? 0);
        setRt(frame.gamepad?.rt ?? 0);
        setButtons(frame.gamepad?.buttons ?? [
          false, false, false, false,
          false, false, false, false,
          false, false, false, false,
          !!frame.gamepad?.dpad_up,
          !!frame.gamepad?.dpad_down,
          !!frame.gamepad?.dpad_left,
          !!frame.gamepad?.dpad_right,
        ]);
      }
      rafRef.current = requestAnimationFrame(poll);
    };
    rafRef.current = requestAnimationFrame(poll);
    return () => cancelAnimationFrame(rafRef.current);
  }, [frame]);

  return (
    <div className="flex flex-col h-full bg-scope-bg items-center justify-center gap-4 py-3 overflow-auto">
      <div className="flex items-center gap-2 text-[9px] font-mono">
        <div className={`w-2 h-2 rounded-full ${gp ? 'dot-live' : 'dot-dead'}`} />
        <span className="text-scope-muted">
          {gp ? `${gp.id.slice(0, 28)}...` : 'No gamepad - showing telemetry inputs'}
        </span>
      </div>

      <div className="flex items-center gap-6">
        <Trigger label="LT" value={lt} />
        <Stick label="Left Stick" x={lx} y={ly} color="#0ea5e9" />
        <Stick label="Right Stick" x={rx} y={ry} color="#a855f7" />
        <Trigger label="RT" value={rt} />
      </div>

      <ButtonGrid buttons={buttons.length > 0 ? buttons : new Array(16).fill(false)} />

      {frame && (
        <div className="flex gap-6 text-[9px] font-mono text-scope-muted">
          <span>DRIVE_X: <span className="text-scope-text readout">{frame.driveX.toFixed(3)}</span></span>
          <span>DRIVE_Y: <span className="text-scope-text readout">{frame.driveY.toFixed(3)}</span></span>
          <span>TURN: <span className="text-scope-text readout">{frame.turn.toFixed(3)}</span></span>
          {frame.gamepad && (
            <span>RAW_RX: <span className="text-scope-text readout">{frame.gamepad.rx.toFixed(3)}</span></span>
          )}
        </div>
      )}
    </div>
  );
}
