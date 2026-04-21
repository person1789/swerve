// ModuleDetail.tsx — per-module diagnostic: steering angle, speed bar, current
import type { TelemetryFrame } from '../types/telemetry';

interface ModuleDetailProps {
  frame: TelemetryFrame | null;
}

const LABELS  = ['FL', 'FR', 'RR', 'RL'];
const COLORS  = ['#f43f5e', '#fb923c', '#facc15', '#4ade80'];
const MAX_SPEED = 1.35; // m/s
const MAX_AMPS  = 12;

function AngleDial({
  targetRad,
  actualRad,
  color,
}: {
  targetRad: number;
  actualRad: number;
  color: string;
}) {
  const cx = 36, cy = 36, r = 28;
  // Convert rad → SVG angle (0=right, +CW in SVG space)
  const toSVG = (a: number) => -a; // our angles are CCW positive

  const targetX = cx + r * Math.cos(toSVG(targetRad));
  const targetY = cy + r * Math.sin(toSVG(targetRad));
  const actualX = cx + r * Math.cos(toSVG(actualRad));
  const actualY = cy + r * Math.sin(toSVG(actualRad));

  return (
    <svg width="72" height="72" viewBox="0 0 72 72">
      {/* Outer ring */}
      <circle cx={cx} cy={cy} r={r} fill="none" stroke="#1a2535" strokeWidth="1" />
      {/* Cardinal ticks */}
      {[0, 90, 180, 270].map(deg => {
        const a = (deg * Math.PI) / 180;
        return (
          <line
            key={deg}
            x1={cx + (r - 5) * Math.cos(a)} y1={cy + (r - 5) * Math.sin(a)}
            x2={cx + r       * Math.cos(a)} y2={cy + r       * Math.sin(a)}
            stroke="#2a3a52" strokeWidth="1"
          />
        );
      })}
      {/* Target needle (dim) */}
      <line
        x1={cx} y1={cy} x2={targetX} y2={targetY}
        stroke={color + '40'} strokeWidth="2" strokeLinecap="round"
      />
      {/* Actual needle */}
      <line
        x1={cx} y1={cy} x2={actualX} y2={actualY}
        stroke={color} strokeWidth="2" strokeLinecap="round"
      />
      {/* Center dot */}
      <circle cx={cx} cy={cy} r="2.5" fill={color} />
    </svg>
  );
}

function SpeedBar({
  speedMps,
  targetMps,
  color,
}: {
  speedMps: number;
  targetMps: number;
  color: string;
}) {
  const pct    = Math.min(Math.abs(speedMps) / MAX_SPEED, 1);
  const tPct   = Math.min(Math.abs(targetMps) / MAX_SPEED, 1);
  const isNeg  = speedMps < 0;

  return (
    <div className="flex flex-col gap-0.5 w-full">
      <div className="flex justify-between text-[8px] font-mono text-scope-muted">
        <span>SPEED</span>
        <span className="readout" style={{ color }}>
          {speedMps.toFixed(2)} m/s
        </span>
      </div>
      <div className="relative h-3 bg-scope-surface rounded overflow-hidden border border-scope-border">
        {/* Target marker */}
        <div
          className="absolute top-0 bottom-0 w-0.5 z-10"
          style={{ left: `${tPct * 100}%`, background: color + '60' }}
        />
        {/* Actual fill */}
        <div
          className="absolute top-0 bottom-0 transition-all duration-75"
          style={{
            left:    isNeg ? `${(1 - pct) * 50}%` : '50%',
            width:   `${pct * 50}%`,
            background: isNeg
              ? `linear-gradient(to left, ${color}, ${color}80)`
              : `linear-gradient(to right, ${color}80, ${color})`,
          }}
        />
        {/* Center line */}
        <div className="absolute top-0 bottom-0 w-px left-1/2 bg-scope-border z-10" />
      </div>
    </div>
  );
}

function CurrentBar({
  amps,
  color,
}: {
  amps: number;
  color: string;
}) {
  const pct = Math.min(amps / MAX_AMPS, 1);
  const dangerColor = pct > 0.8 ? '#ef4444' : pct > 0.6 ? '#f59e0b' : color;

  return (
    <div className="flex flex-col gap-0.5 w-full">
      <div className="flex justify-between text-[8px] font-mono text-scope-muted">
        <span>CURRENT</span>
        <span className="readout" style={{ color: dangerColor }}>
          {amps.toFixed(1)} A
        </span>
      </div>
      <div className="relative h-2 bg-scope-surface rounded overflow-hidden border border-scope-border">
        <div
          className="absolute top-0 left-0 bottom-0 transition-all duration-75 rounded"
          style={{ width: `${pct * 100}%`, background: dangerColor + 'cc' }}
        />
      </div>
    </div>
  );
}

function ModuleCard({
  index,
  frame,
}: {
  index: number;
  frame: TelemetryFrame | null;
}) {
  const color  = COLORS[index];
  const label  = LABELS[index];
  const target = frame?.targets?.[index] ?? [0, 0];
  const actual = frame?.actuals?.[index] ?? [0, 0];

  const targetSpeedMps = target[0];
  const targetAngleRad = target[1];
  const actualSpeedMps = actual[0];
  const actualAngleRad = actual[1];

  const errorRad = actualAngleRad - targetAngleRad;
  const errorDeg = (errorRad * 180) / Math.PI;

  return (
    <div className="flex flex-col h-full p-2 bg-scope-bg border border-scope-border rounded gap-2 min-h-0">
      {/* Header */}
      <div className="flex items-center justify-between flex-shrink-0">
        <span className="text-[10px] font-mono font-bold" style={{ color }}>
          {label}
        </span>
        <span className="text-[8px] font-mono text-scope-muted readout">
          err: {errorDeg.toFixed(1)}°
        </span>
      </div>

      {/* Dial */}
      <div className="flex justify-center flex-shrink-0">
        <AngleDial
          targetRad={targetAngleRad}
          actualRad={actualAngleRad}
          color={color}
        />
      </div>

      {/* Angle readouts */}
      <div className="flex justify-between text-[8px] font-mono flex-shrink-0">
        <span className="text-scope-muted">
          T: <span className="readout" style={{ color: color + '80' }}>
            {(targetAngleRad * 180 / Math.PI).toFixed(1)}°
          </span>
        </span>
        <span className="text-scope-muted">
          A: <span className="readout" style={{ color }}>
            {(actualAngleRad * 180 / Math.PI).toFixed(1)}°
          </span>
        </span>
      </div>

      {/* Speed bar */}
      <SpeedBar
        speedMps={actualSpeedMps}
        targetMps={targetSpeedMps}
        color={color}
      />

      {/* Current bar */}
      <CurrentBar
        amps={Math.max(0, 1.2 + Math.abs(actualSpeedMps) * 2.5)}
        color={color}
      />
    </div>
  );
}

export default function ModuleDetail({ frame }: ModuleDetailProps) {
  return (
    <div className="w-full h-full p-1 grid grid-cols-2 grid-rows-2 gap-1">
      {[0, 1, 2, 3].map(i => (
        <ModuleCard key={i} index={i} frame={frame} />
      ))}
    </div>
  );
}
