# Auto Planning Hub

This folder is the one-stop shop for planning auto without adding a custom pathing stack.

## What To Use

- Pedro Visualizer local mirror: `tools/PedroVisualizer`
- Hosted Pedro Visualizer: https://visualizer.pedropathing.com
- Hosted Pedro Path Generator fallback: https://pedro-path-generator.vercel.app
- Robot-side visual truth: FTC Dashboard field overlay from `MiniDriveAuto`

The local Pedro Visualizer has been pinned to the FTC 2025-2026 DECODE field only.

## One Command

From the repo root:

```powershell
.\tools\auto-planning\open-auto-planning.ps1
```

That script opens this hub and starts the local Pedro Visualizer when dependencies are installed.

If `node_modules` is missing inside `tools/PedroVisualizer`, run:

```powershell
.\tools\auto-planning\install-pedro-visualizer.ps1
```

## Workflow

1. Open the local or hosted Pedro Visualizer.
2. Select or confirm the DECODE field.
3. Create only straight-line segments for now.
4. Read each endpoint's `x`, `y`, and heading from the visualizer.
5. Paste those values into our scheduler format.
6. Run `Mini Drive Auto` with FTC Dashboard open to see actual Pinpoint pose and the route overlay.

## Scheduler Format

```java
localizer.setPose(startX, startY, Math.toRadians(startHeadingDeg));

DriveScheduler scheduler = new DriveScheduler(commandCount * 2 + 1)
        .addMoveToPose(x1, y1, Math.toRadians(h1), true, 0.0, 2500.0)
        .addMoveToPose(x2, y2, Math.toRadians(h2), true, 0.0, 2500.0)
        .add(new StopDriveCommand());
```

Use `true` for azimuth wait unless you intentionally want the robot to begin translating before the modules are aimed.

## Coordinate Notes

- All coordinates are inches.
- The Pedro Visualizer local clone uses the official DECODE field image.
- Keep coordinates consistent with Pinpoint and `AutoRoute`.
- For now, treat visualizer curves as unsupported. Use straight segments only.

## Files In This Hub

- `open-auto-planning.ps1`: starts the local visualizer and opens the hub page.
- `install-pedro-visualizer.ps1`: installs local visualizer dependencies.
- `run-pedro-visualizer.ps1`: starts the local Pedro Visualizer.
- `scheduler-template.java`: copy-paste template for generated route commands.
- `index.html`: local launcher page with links and command templates.
