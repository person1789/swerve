# Local Pedro Visualizer Notes

This is a local mirror of the official Pedro Pathing Visualizer:

https://github.com/Pedro-Pathing/Visualizer

Local change:

- `src/config/defaults.ts` exposes only `decode.webp`, the FTC 2025-2026 DECODE field.

Run from repo root:

```powershell
.\tools\auto-planning\install-pedro-visualizer.ps1
.\tools\auto-planning\run-pedro-visualizer.ps1
```

Use the visualizer for geometry only. Our robot auto uses straight-line `DriveScheduler.addMoveToPose(...)` commands, not Pedro path following.
