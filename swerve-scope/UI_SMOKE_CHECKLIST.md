# SwerveScope UI Smoke Checklist

Run this after meaningful UI changes in `C:\Users\ajayp\Downloads\FTCcode - Copy\swerve-scope`.

## Startup

- Run `npm run dev` and verify the app opens without console errors.
- Confirm the top bar shows `LOCAL // TS` when Java SITL is not running.
- Start Java SITL and confirm the status flips to `JAVA // SITL`.

## Arena And Playback

- Drive the robot in local mode and confirm the arena feels smooth while the rest of the UI stays responsive.
- Click in the scope graph to scrub and confirm arena, inspector, and module detail jump to the selected frame.
- Drag across the scope graph to define an analysis range and confirm the range stats update.
- Add a bookmark with `B+`, then jump backward and forward with `B-` and `B>`.
- Use `RESET SIM` in local mode and confirm history, bookmarks, and range selection clear.

## Persistence

- Record a short run, click `SAVE`, and confirm a session JSON downloads.
- Click `LOAD` and import that session.
- Confirm history length, scrub position, bookmarks, active fields, custom fields, workspace layout, and arena toggles are restored.

## Pane Behavior

- Put `System Console`, `Inspector`, and `Module Detail` into short slots and confirm each pane scrolls internally.
- Switch panes inside the same workspace slot and confirm inactive panes unmount cleanly without visual leftovers.
- Leave the app running for at least 30 seconds and confirm no visible lag buildup occurs when non-visible panes are hidden.

## Console

- Trigger log activity and verify severity filters, source filter, search, pinning, pinned-only mode, and export all behave correctly.
- Enter a manual log message and confirm it appears immediately.
- Scroll away from the bottom, verify auto-scroll stops, then return to the bottom and confirm live logs resume following.

## Session Analysis

- Load a saved session and verify non-live playback controls appear.
- Step frame-by-frame, change playback rates, and confirm the scrubber and displayed data stay in sync.
- Create a range from the graph, then set range start/end from the inspector and confirm both views stay aligned.
