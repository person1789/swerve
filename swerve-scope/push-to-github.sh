#!/bin/bash
# Run this from inside the swerve-sim/ folder (or wherever you cloned it)
# It pushes the SwerveScope work into the Pedro-Pathing branch of the main repo

set -e

echo "=== SwerveScope → GitHub push script ==="
echo ""

# 1. Make sure we have the remote pointing to your repo
git remote set-url origin https://github.com/person1789/swerve.git 2>/dev/null || \
  git remote add origin https://github.com/person1789/swerve.git

# 2. Fetch so we know what's on the remote
echo "Fetching remote..."
git fetch origin

# 3. The files we built live in a standalone swervescope/ subdirectory.
#    We want to push them INTO swerve-sim/ inside the existing Pedro-Pathing branch
#    without clobbering anything else already on that branch.

# Checkout Pedro-Pathing (create tracking branch if it doesn't exist locally)
git checkout -B Pedro-Pathing origin/Pedro-Pathing 2>/dev/null || \
  git checkout -b Pedro-Pathing

# 4. Copy the swervescope build into swerve-sim/ (merge, not overwrite unrelated files)
#    If you ran this script from inside the swervescope/ dir, go up one level first.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEST="swerve-sim"

mkdir -p "$DEST"
rsync -av --exclude='.git' --exclude='node_modules' "$SCRIPT_DIR/" "$DEST/"

git add "$DEST"
git commit -m "feat(swerve-sim): SwerveScope v2 — Phases 1-5

Complete rewrite of the swerve-sim frontend diagnostic suite.

Architecture:
- src/lib/SwerveLogic.ts: full TS port of Java kinematics, auditor, smoother, heading controller, module emulators, odometry
- src/lib/ExprEval.ts: safe JS expression evaluator for virtual telemetry fields
- src/hooks/useSITL.ts: WebSocket bridge to Java SwerveSimServer with auto-reconnect
- src/hooks/useHistory.ts: 36,000-frame ring buffer with scrubbing
- src/hooks/useGamepad.ts: Gamepad API polling with deadband

Components (Phase 1-5):
- Arena.tsx: 2D field canvas — odometry trail, robot chassis, module vectors, angle dials
- ScopePanel.tsx: uPlot multi-series oscilloscope with expression fields + ResizeObserver
- ModuleDetail.tsx: 4-pane per-module SVG cards — AngleDial, SpeedBar, CurrentBar
- InspectorPanel.tsx: live/table/raw views, CSV export, recording
- JoystickPanel.tsx: SVG dual-stick + triggers + 16-button grid, Gamepad API
- ConsolePanel.tsx: event log with severity filter, search, auto-scroll
- ExprEditor.tsx: add/remove/toggle derived telemetry expression fields
- ControlBar.tsx: connection status, tab navigation, timeline scrubber

See swerve-sim/TODO.md for remaining build fixes and Phase 6+ roadmap." || echo "Nothing new to commit"

# 5. Push
echo ""
echo "Pushing to origin/Pedro-Pathing..."
git push origin Pedro-Pathing

echo ""
echo "Done! Check: https://github.com/person1789/swerve/tree/Pedro-Pathing"
