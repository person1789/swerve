import type { TelemetryFrame } from '../types/telemetry';

interface ModuleDetailProps {
  frame: TelemetryFrame | null;
}

const LABELS = ['FL', 'FR', 'RR', 'RL'];
const COLORS = ['#f43f5e', '#fb923c', '#facc15', '#4ade80'];
const MAX_SPEED = 1.35;
const MAX_AMPS = 18;

function AngleDial({ targetRad, actualRad, color }: { targetRad: number; actualRad: number; color: string }) {
  const cx = 36;
  const cy = 36;
  const radius = 28;
  const toSvg = (angle: number) => -angle;

  const targetX = cx + radius * Math.cos(toSvg(targetRad));
  const targetY = cy + radius * Math.sin(toSvg(targetRad));
  const actualX = cx + radius * Math.cos(toSvg(actualRad));
  const actualY = cy + radius * Math.sin(toSvg(actualRad));

  return (
    <svg width="72" height="72" viewBox="0 0 72 72">
      <circle cx={cx} cy={cy} r={radius} fill="none" stroke="#1a2535" strokeWidth="1" />
      {[0, 90, 180, 270].map(deg => {
        const angle = (deg * Math.PI) / 180;
        return (
          <line
            key={deg}
            x1={cx + (radius - 5) * Math.cos(angle)}
            y1={cy + (radius - 5) * Math.sin(angle)}
            x2={cx + radius * Math.cos(angle)}
            y2={cy + radius * Math.sin(angle)}
            stroke="#2a3a52"
            strokeWidth="1"
          />
        );
      })}
      <line x1={cx} y1={cy} x2={targetX} y2={targetY} stroke={`${color}40`} strokeWidth="2" strokeLinecap="round" />
      <line x1={cx} y1={cy} x2={actualX} y2={actualY} stroke={color} strokeWidth="2" strokeLinecap="round" />
      <circle cx={cx} cy={cy} r="2.5" fill={color} />
    </svg>
  );
}

function SpeedBar({ speedMps, targetMps, color }: { speedMps: number; targetMps: number; color: string }) {
  const pct = Math.min(Math.abs(speedMps) / MAX_SPEED, 1);
  const targetPct = Math.min(Math.abs(targetMps) / MAX_SPEED, 1);
  const isNegative = speedMps < 0;

  return (
    <div className="flex flex-col gap-0.5 w-full">
      <div className="flex justify-between text-[8px] font-mono text-scope-muted">
        <span>SPEED</span>
        <span className="readout" style={{ color }}>{speedMps.toFixed(2)} m/s</span>
      </div>
      <div className="relative h-3 bg-scope-surface rounded overflow-hidden border border-scope-border">
        <div className="absolute top-0 bottom-0 w-0.5 z-10" style={{ left: `${targetPct * 100}%`, background: `${color}60` }} />
        <div
          className="absolute top-0 bottom-0 transition-all duration-75"
          style={{
            left: isNegative ? `${(1 - pct) * 50}%` : '50%',
            width: `${pct * 50}%`,
            background: isNegative ? `linear-gradient(to left, ${color}, ${color}80)` : `linear-gradient(to right, ${color}80, ${color})`,
          }}
        />
        <div className="absolute top-0 bottom-0 w-px left-1/2 bg-scope-border z-10" />
      </div>
    </div>
  );
}

function CurrentBar({ amps, color }: { amps: number; color: string }) {
  const pct = Math.min(amps / MAX_AMPS, 1);
  const dangerColor = pct > 0.8 ? '#ef4444' : pct > 0.6 ? '#f59e0b' : color;

  return (
    <div className="flex flex-col gap-0.5 w-full">
      <div className="flex justify-between text-[8px] font-mono text-scope-muted">
        <span>CURRENT</span>
        <span className="readout" style={{ color: dangerColor }}>{amps.toFixed(1)} A</span>
      </div>
      <div className="relative h-2 bg-scope-surface rounded overflow-hidden border border-scope-border">
        <div className="absolute top-0 left-0 bottom-0 transition-all duration-75 rounded" style={{ width: `${pct * 100}%`, background: `${dangerColor}cc` }} />
      </div>
    </div>
  );
}

function TractionRing({ color, speedMps }: { color: string; speedMps: number }) {
  const pct = Math.min(Math.abs(speedMps) / MAX_SPEED, 1);
  const circumference = 2 * Math.PI * 18;
  const dashOffset = circumference * (1 - pct);

  return (
    <svg width="46" height="46" viewBox="0 0 46 46" className="flex-shrink-0">
      <circle cx="23" cy="23" r="18" fill="none" stroke="#1a2535" strokeWidth="3" />
      <circle
        cx="23"
        cy="23"
        r="18"
        fill="none"
        stroke={color}
        strokeWidth="3"
        strokeDasharray={circumference}
        strokeDashoffset={dashOffset}
        strokeLinecap="round"
        transform="rotate(-90 23 23)"
      />
      <text x="23" y="26" textAnchor="middle" className="fill-scope-text text-[7px] font-mono">
        {(pct * 100).toFixed(0)}%
      </text>
    </svg>
  );
}

function ModuleCard({ index, frame }: { index: number; frame: TelemetryFrame | null }) {
  const color = COLORS[index];
  const label = LABELS[index];
  const target = frame?.targets?.[index] ?? [0, 0];
  const actual = frame?.actuals?.[index] ?? [0, 0];

  const targetSpeedMps = target[0];
  const targetAngleRad = target[1];
  const actualSpeedMps = actual[0];
  const actualAngleRad = actual[1];
  const amps = frame?.currentDraw?.[index] ?? Math.max(0, 1.2 + Math.abs(actualSpeedMps) * 2.5);
  const errorDeg = ((actualAngleRad - targetAngleRad) * 180) / Math.PI;

  return (
    <div className="flex flex-col h-full p-2 bg-scope-bg border border-scope-border rounded gap-2 min-h-0">
      <div className="flex items-center justify-between flex-shrink-0">
        <span className="text-[10px] font-mono font-bold" style={{ color }}>{label}</span>
        <span className="text-[8px] font-mono text-scope-muted readout">err: {errorDeg.toFixed(1)} deg</span>
      </div>

      <div className="flex items-center justify-center gap-2 flex-shrink-0">
        <AngleDial targetRad={targetAngleRad} actualRad={actualAngleRad} color={color} />
        <TractionRing color={color} speedMps={actualSpeedMps} />
      </div>

      <div className="flex justify-between text-[8px] font-mono flex-shrink-0">
        <span className="text-scope-muted">
          T: <span className="readout" style={{ color: `${color}80` }}>{(targetAngleRad * 180 / Math.PI).toFixed(1)} deg</span>
        </span>
        <span className="text-scope-muted">
          A: <span className="readout" style={{ color }}>{(actualAngleRad * 180 / Math.PI).toFixed(1)} deg</span>
        </span>
      </div>

      <SpeedBar speedMps={actualSpeedMps} targetMps={targetSpeedMps} color={color} />
      <CurrentBar amps={amps} color={color} />
    </div>
  );
}

function ChassisSummary({ frame }: { frame: TelemetryFrame | null }) {
  const actualModules = frame?.actuals ?? [[0, 0], [0, 0], [0, 0], [0, 0]];
  const targetModules = frame?.targets ?? [[0, 0], [0, 0], [0, 0], [0, 0]];
  const actualVx = actualModules.reduce((sum, module) => sum + module[0] * Math.cos(module[1]), 0) / 4;
  const actualVy = actualModules.reduce((sum, module) => sum + module[0] * Math.sin(module[1]), 0) / 4;
  const targetVx = targetModules.reduce((sum, module) => sum + module[0] * Math.cos(module[1]), 0) / 4;
  const targetVy = targetModules.reduce((sum, module) => sum + module[0] * Math.sin(module[1]), 0) / 4;
  const actualMag = Math.hypot(actualVx, actualVy);
  const targetMag = Math.hypot(targetVx, targetVy);
  const omega = frame?.observerVel?.omega ?? frame?.smootherState?.omega ?? 0;
  const maxArrow = 42;
  const scale = maxArrow / Math.max(MAX_SPEED, targetMag, actualMag, 0.1);

  return (
    <div className="flex flex-col h-full p-2 bg-scope-bg border border-scope-border rounded gap-2">
      <div className="flex items-center justify-between">
        <span className="text-[10px] font-mono font-bold text-scope-accent">CHASSIS</span>
        <span className="text-[8px] font-mono text-scope-muted">{frame?.controllerMode ?? 'manual'}</span>
      </div>

      <div className="flex items-center justify-center">
        <svg width="150" height="120" viewBox="0 0 150 120">
          <rect x="38" y="23" width="74" height="74" rx="8" fill="#0d1520" stroke="#1a2535" />
          <line x1="75" y1="60" x2={75 + targetVx * scale} y2={60 - targetVy * scale} stroke="#ef444480" strokeWidth="3" strokeLinecap="round" />
          <line x1="75" y1="60" x2={75 + actualVx * scale} y2={60 - actualVy * scale} stroke="#0ea5e9" strokeWidth="3" strokeLinecap="round" />
          <circle cx="75" cy="60" r="4" fill="#8ba4c0" />
          <path
            d={`M 113 40 A 16 16 0 0 1 113 ${80 - Math.min(Math.abs(omega) * 8, 18)}`}
            fill="none"
            stroke={omega >= 0 ? '#22c55e' : '#f59e0b'}
            strokeWidth="2"
          />
          <text x="75" y="108" textAnchor="middle" className="fill-scope-muted text-[8px] font-mono">
            red target // blue actual
          </text>
        </svg>
      </div>

      <div className="grid grid-cols-2 gap-2 text-[8px] font-mono">
        <div className="rounded border border-scope-border bg-scope-surface px-2 py-1">
          <div className="text-scope-muted">actual</div>
          <div className="text-scope-text readout">{actualMag.toFixed(2)} m/s</div>
        </div>
        <div className="rounded border border-scope-border bg-scope-surface px-2 py-1">
          <div className="text-scope-muted">target</div>
          <div className="text-scope-text readout">{targetMag.toFixed(2)} m/s</div>
        </div>
        <div className="rounded border border-scope-border bg-scope-surface px-2 py-1">
          <div className="text-scope-muted">vx / vy</div>
          <div className="text-scope-text readout">{actualVx.toFixed(2)} / {actualVy.toFixed(2)}</div>
        </div>
        <div className="rounded border border-scope-border bg-scope-surface px-2 py-1">
          <div className="text-scope-muted">omega</div>
          <div className="text-scope-text readout">{omega.toFixed(2)} rad/s</div>
        </div>
      </div>
    </div>
  );
}

export default function ModuleDetail({ frame }: ModuleDetailProps) {
  return (
    <div className="w-full h-full p-1 grid grid-cols-3 grid-rows-3 gap-1">
      <div className="col-start-1 row-start-1"><ModuleCard index={0} frame={frame} /></div>
      <div className="col-start-3 row-start-1"><ModuleCard index={1} frame={frame} /></div>
      <div className="col-start-1 row-start-3"><ModuleCard index={3} frame={frame} /></div>
      <div className="col-start-3 row-start-3"><ModuleCard index={2} frame={frame} /></div>
      <div className="col-start-2 row-start-2">
        <ChassisSummary frame={frame} />
      </div>
    </div>
  );
}
