# Running SwerveScope

## Fastest path: local simulator

This now works without the Java backend.

```powershell
cd "C:\Users\ajayp\Downloads\FTCcode - Copy\swerve-scope"
npm run dev
```

Open `http://localhost:5173`.

If the Java WebSocket server is not running, SwerveScope automatically falls back to `LOCAL // TS` mode.

Controls in local mode:

- `W/A/S/D`: translate
- `Q/E`: rotate
- Arrow keys: snap heading
- Gamepad also works if connected

## Java SITL mode

In a second terminal:

```powershell
cd "C:\Users\ajayp\Downloads\FTCcode - Copy\swerve-scope"
npm run sitl:java
```

When the Java backend is available on `ws://localhost:8080`, the UI switches to `JAVA // SITL`.

## What to test first

1. Verify the top bar shows either `LOCAL // TS` or `JAVA // SITL`.
2. Drive the robot in the field view and confirm pose, trail, and module vectors update.
3. Open the detailer and confirm target vs actual module behavior changes while steering.
4. Open graphs and toggle fields from the field registry.
5. Open inspector table and click a row to scrub history.
