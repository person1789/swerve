# SwerveScope — Remaining Work & How-To Guide

This document covers everything left to make SwerveScope fully runnable and feature-complete through Phase 5.

---

## 1. Get It Running (First Priority)

### Install dependencies
```bash
cd swerve-scope
npm install
npm run dev
# Open http://localhost:5173
```

### Start the Java SITL brain (in a separate terminal)
```bash
# From the root of the Android project
./gradlew :TeamCode:testDebugUnitTest \
  --tests "org.firstinspires.ftc.teamcode.Swerve.Tests.SwerveSimServer.startSITLServer" \
  --no-daemon
```

The Java server opens a WebSocket on `ws://localhost:8080`. SwerveScope auto-reconnects every 2 seconds if the connection drops.

### Connect a gamepad
Plug in an Xbox/PS4/PS5 controller **before** opening the browser tab, then press any button to activate it. The browser only surfaces gamepads after a user gesture.

---

## 2. Build Errors to Fix

**✅ Build/Compile Status (2026-04-21):** No compiler errors found during `npm run build`.



### 2a. Input range thumb styling (scrubber knob)
The timeline scrubber in `ControlBar.tsx` uses an inline `background` gradient trick that works in Chrome/Edge but not Firefox/Safari. Add proper cross-browser CSS to `src/index.css`:

```css
input[type='range'] {
  -webkit-appearance: none;
  appearance: none;
  height: 4px;
  border-radius: 2px;
  outline: none;
  cursor: pointer;
}
input[type='range']::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 12px; height: 12px;
  border-radius: 50%;
  background: #0ea5e9;
  cursor: pointer;
  transition: transform 0.1s;
}
input[type='range']::-webkit-slider-thumb:hover { transform: scale(1.3); }
input[type='range']::-moz-range-thumb {
  width: 12px; height: 12px;
  border-radius: 50%;
  background: #0ea5e9;
  border: none;
  cursor: pointer;
}
```

### 2b. `useHistory.getBuffer()` stability
`getBuffer()` returns the raw array ref, which is stable, but calling it inside a component render without a dependency will not trigger re-renders when new frames arrive. Fix by having `ScopePanel` and `InspectorPanel` accept `historyLength` as a separate prop to drive re-renders:

```tsx
// In App.tsx pass both:
<ScopePanel
  history={history.getBuffer()}
  historyLength={history.length}   // ← add this
  ...
/>
```

```tsx
// In ScopePanel.tsx update the data-feed useEffect:
}, [historyLength, isLive, allFields, windowSec]);  // ← use historyLength not history
```

### 2c. Tailwind `scope-*` color classes not resolving
The custom `scope-*` colors defined in `tailwind.config.js` use the object syntax. If classes like `bg-scope-bg` show as transparent, verify `tailwind.config.js` `content` globs include all `.tsx` files and rebuild:

```bash
npx tailwindcss -i src/index.css -o dist/output.css --watch
```

### 2d. uPlot CSS import path
`ScopePanel.tsx` imports `uplot/dist/uPlot.min.css`. Confirm this path exists after install:
```bash
ls node_modules/uplot/dist/
# Should show: uPlot.cjs.js  uPlot.esm.js  uPlot.iife.js  uPlot.min.css
```
If missing, use: `import 'uplot/dist/uPlot.iife.js'` and add `uPlot.min.css` manually to `index.html`.

---

## 3. Missing Features to Implement

### 3a. Playback rate selector
Add a speed control to `ControlBar.tsx` for replaying history at different rates.

**Where:** Add to `ControlBar` props: `playbackRate: number`, `onRateChange: (r: number) => void`

**In App.tsx** add an interval that advances `scrubIndex` when `!isLive`:
```tsx
useEffect(() => {
  if (isLive || !isPlaying) return;
  const id = setInterval(() => {
    history.seek(prev => Math.min(prev + 1, history.length - 1));
  }, 20 / playbackRate);
  return () => clearInterval(id);
}, [isLive, isPlaying, playbackRate]);
```

**UI:** Add buttons `0.25×  0.5×  1×  2×` in the scrubber row.

### 3b. Keyboard shortcuts
Add a `useEffect` in `App.tsx`:
```tsx
useEffect(() => {
  const handler = (e: KeyboardEvent) => {
    if (e.target instanceof HTMLInputElement) return; // don't steal from inputs
    switch (e.key) {
      case ' ':      history.snapToLive();             break;
      case 'l': case 'L': setShowTrail(v => !v);     break;
      case 'v': case 'V': setShowVectors(v => !v);   break;
      case '1': setActiveTab('drive');      break;
      case '2': setActiveTab('modules');   break;
      case '3': setActiveTab('scopes');    break;
      case '4': setActiveTab('joystick');  break;
      case '5': setActiveTab('inspector'); break;
      case '6': setActiveTab('console');   break;
      case '7': setActiveTab('exprs');     break;
    }
  };
  window.addEventListener('keydown', handler);
  return () => window.removeEventListener('keydown', handler);
}, [history]);
```

### 3c. Persist custom expression fields
In `ExprEditor.tsx` or `App.tsx`, save/load `customFields` and `activeFields` to `localStorage`:
```tsx
// Load on mount
const [customFields, setCustomFields] = useState<DerivedField[]>(() => {
  try { return JSON.parse(localStorage.getItem('scope_custom_fields') || '[]'); }
  catch { return []; }
});

// Save on change
useEffect(() => {
  localStorage.setItem('scope_custom_fields', JSON.stringify(customFields));
}, [customFields]);
```

### 3d. Scope window duration control
Add a slider to `ScopePanel.tsx` header to let the user pick 5s / 15s / 30s / 60s window:
```tsx
const [windowSec, setWindowSec] = useState(30);
// Add in panel header:
<select value={windowSec} onChange={e => setWindowSec(+e.target.value)}>
  {[5,15,30,60].map(s => <option key={s} value={s}>{s}s</option>)}
</select>
```
Remove the `windowSec` prop from `App.tsx` and let `ScopePanel` own it internally.

### 3e. AprilTag field overlays in Arena
For the DECODE (2025-26) season, draw the AprilTag positions on the 12×12 field.

**In `Arena.tsx`**, add after the field boundary draw:
```tsx
const DECODE_TAGS = [
  { id: 1, x: 0.0,  y:  1.5 },
  { id: 2, x: 0.0,  y: -1.5 },
  // ... add all tag positions from FieldConstants
];

DECODE_TAGS.forEach(tag => {
  ctx.save();
  ctx.translate(tag.x * PX_PER_M, -tag.y * PX_PER_M);
  ctx.strokeStyle = '#f59e0b40';
  ctx.lineWidth = 1;
  ctx.strokeRect(-8, -8, 16, 16);
  ctx.fillStyle = '#f59e0b80';
  ctx.font = '600 7px JetBrains Mono';
  ctx.textAlign = 'center';
  ctx.fillText(`T${tag.id}`, 0, 3);
  ctx.restore();
});
```

### 3f. Manual console log injection
Add a textarea + send button at the bottom of `ConsolePanel.tsx` for injecting debug messages during SITL:
```tsx
const [draft, setDraft] = useState('');
// In JSX below the log list:
<div className="flex gap-2 p-2 border-t border-scope-border">
  <input
    className="expr-input flex-1 text-[9px]"
    placeholder="Inject log message…"
    value={draft}
    onChange={e => setDraft(e.target.value)}
    onKeyDown={e => {
      if (e.key === 'Enter' && draft.trim()) {
        setEntries(prev => [...prev, mkEntry(
          performance.now() / 1000, 'info', 'MANUAL', draft.trim()
        )]);
        setDraft('');
      }
    }}
  />
</div>
```

---

## 4. Phase 6+ (Future Work)

These are defined in `swervescope_master_implementation_plan.md` but not yet started:

| Phase | Feature | Notes |
|-------|---------|-------|
| 6 | FFT Analysis pane | Use `dsp.js` (already in original deps). Wire `ScopePanel` to expose raw float arrays, run `FFT.forward()`, render frequency bins with uPlot. |
| 6 | Statistics engine | Histogram + StdDev + RMS for any selected scrub range. Add a "Stats" popover on each scope series. |
| 6 | Auto-tuning profiler | Record a kS/kV/kA characterisation run, fit a line through voltage vs velocity data using least-squares. |
| 7 | Video sync | Use `<video>` element with `currentTime` linked to `scrubIndex * 0.02`. Add a file picker for `.mp4`. |
| 7 | Multi-session overlay | Load a second `.csv` export and overlay it as ghost traces on the scopes. |
| 8 | AprilTag 3D overlay | Needs Three.js. Place tag quads at known field positions, show robot-estimated pose as a frustum. |

---

## Canonical Planning Note

The authoritative implementation direction now lives in:

- `SWERVESCOPE_CANONICAL_IMPLEMENTATION_PLAN.md`
- `SWERVESCOPE_CHAT_CONTEXT.md`

Treat this file as historical/prototype guidance only when it conflicts with the canonical plan.

## 5. File Map

```
swerve-scope/
├── src/
│   ├── App.tsx                  ← main shell, tab layout, splitters
│   ├── main.tsx                 ← React 18 entry
│   ├── index.css                ← global styles + design tokens
│   ├── components/
│   │   ├── Arena.tsx            ← 2D field canvas
│   │   ├── ScopePanel.tsx       ← uPlot oscilloscope
│   │   ├── ModuleDetail.tsx     ← 4-pane module cards
│   │   ├── InspectorPanel.tsx   ← telemetry table + CSV export
│   │   ├── JoystickPanel.tsx    ← SVG gamepad visualizer
│   │   ├── ConsolePanel.tsx     ← scrolling event log
│   │   ├── ExprEditor.tsx       ← expression field manager
│   │   └── ControlBar.tsx       ← header, scrubber, tabs
│   ├── hooks/
│   │   ├── useSITL.ts           ← WebSocket bridge w/ auto-reconnect
│   │   ├── useHistory.ts        ← 36k-frame ring buffer + scrub
│   │   └── useGamepad.ts        ← Gamepad API polling
│   ├── lib/
│   │   ├── SwerveLogic.ts       ← TS port of Java swerve math
│   │   └── ExprEval.ts          ← safe expression evaluator
│   └── types/
│       └── telemetry.ts         ← shared TypeScript types
├── package.json
├── vite.config.ts
├── tailwind.config.js
├── tsconfig.json
└── TODO.md                      ← this file
```

---

## 6. Quick Reference — Available Expression Variables

When writing custom scope expressions in the Expression Editor:

| Variable | Description | Unit |
|----------|-------------|------|
| `t` | Timestamp since session start | s |
| `x`, `y` | Field position | m |
| `heading` | Heading | rad |
| `headingDeg` | Heading | ° |
| `speed` | Chassis speed magnitude | m/s |
| `battery` | Battery voltage | V |
| `loopMs` | Control loop period | ms |
| `driveX`, `driveY`, `turn` | Driver stick inputs | [-1,1] |
| `m0_speed` … `m3_speed` | Module actual speeds (FL/FR/RR/RL) | m/s |
| `m0_angle` … `m3_angle` | Module actual angles | rad |
| `abs`, `sqrt`, `sin`, `cos`, `atan2`, `hypot`, `PI` | Math helpers | — |

**Example expressions:**
```js
hypot(driveX, driveY)          // driver input magnitude
m0_speed - m1_speed            // FL vs FR speed differential
battery * 0.95                 // derated battery estimate
loopMs > 25 ? 1 : 0            // overrun flag (shows as 0/1 step)
abs(headingDeg) % 90           // heading modulo quadrant
```
