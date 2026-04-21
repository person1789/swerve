import { useEffect, useRef } from 'react';
import type { ChangeEvent } from 'react';
import type { WorkspacePaneId } from './types/telemetry';
import { parseSessionFile } from './lib/sessionPersistence';
import { useSwerveScopeState } from './hooks/useSwerveScopeState';

import ControlBar from './components/ControlBar';
import Arena from './components/Arena';
import ScopePanel from './components/ScopePanel';
import ModuleDetail from './components/ModuleDetail';
import InspectorPanel from './components/InspectorPanel';
import JoystickPanel from './components/JoystickPanel';
import ConsolePanel from './components/ConsolePanel';
import ExprEditor from './components/ExprEditor';
import WorkspaceShell from './components/WorkspaceShell';
import { telemetryStore } from './store/telemetryStore';

export default function App() {
  const state = useSwerveScopeState();
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    const handler = (event: KeyboardEvent) => {
      if (
        event.target instanceof HTMLInputElement ||
        event.target instanceof HTMLTextAreaElement ||
        event.target instanceof HTMLSelectElement
      ) return;

      switch (event.key) {
        case ' ':
          event.preventDefault();
          telemetryStore.snapToLive();
          state.setIsPlaying(false);
          break;
        case 'l':
        case 'L':
          state.setShowTrail(v => !v);
          break;
        case 'v':
        case 'V':
          state.setShowVectors(v => !v);
          break;
        case 'k':
        case 'K':
          if (!state.history.isLive) state.setIsPlaying(v => !v);
          break;
        case 'ArrowLeft':
          if (!state.history.isLive) telemetryStore.stepBy(-1);
          break;
        case 'ArrowRight':
          if (!state.history.isLive) telemetryStore.stepBy(1);
          break;
        case 'b':
        case 'B':
          state.toggleBookmark(state.history.scrubIndex);
          break;
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [state]);

  const paneTitles: Record<WorkspacePaneId, string> = {
    arena: 'Field View // Arena',
    detailer: 'Swerve Detailer // Modules',
    graph: 'Diagnostic Graphs // Time-Series',
    inspector: 'Inspection Engine // Telemetry',
    console: 'System Console // Events',
    joystick: 'Joystick Visualizer // Inputs',
    expressions: 'Expression Engine // Fields',
  };

  const handleExportSession = () => {
    const session = state.exportSession();
    const blob = new Blob([JSON.stringify(session, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `swervescope-session-${new Date().toISOString().slice(0, 19).replace(/:/g, '-')}.json`;
    anchor.click();
    URL.revokeObjectURL(url);
  };

  const handleImportClick = () => {
    fileInputRef.current?.click();
  };

  const handleImportFile = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;

    try {
      const text = await file.text();
      const session = parseSessionFile(text);
      state.importSession(session);
    } catch (error) {
      state.setSessionError(error instanceof Error ? error.message : 'Failed to import session file.');
    }
  };

  const renderPane = (pane: WorkspacePaneId) => {
    switch (pane) {
      case 'arena':
        return (
          <Arena
            showVectors={state.showVectors}
            showTrail={state.showTrail}
          />
        );
      case 'detailer':
        return <ModuleDetail />;
      case 'graph':
        return (
          <ScopePanel
            activeFields={state.activeFields}
            customFields={state.customFields}
            scrubIndex={state.history.scrubIndex}
            analysisRange={state.analysisRange}
            bookmarks={state.bookmarks}
            onSetRange={state.setAnalysisRange}
            onClearRange={state.clearAnalysisRange}
            onJumpToIndex={telemetryStore.seek.bind(telemetryStore)}
            onToggleField={(fieldId) => {
              state.setActiveFields(prev =>
                prev.includes(fieldId)
                  ? prev.filter(id => id !== fieldId)
                  : [...prev, fieldId],
              );
            }}
          />
        );
      case 'inspector':
        return (
          <InspectorPanel
            scrubIndex={state.history.scrubIndex}
            analysisRange={state.analysisRange}
            bookmarks={state.bookmarks}
            onJumpToIndex={telemetryStore.seek.bind(telemetryStore)}
            onSetRangeStart={state.setRangeStart}
            onSetRangeEnd={state.setRangeEnd}
            onToggleBookmark={state.toggleBookmark}
          />
        );
      case 'console':
        return <ConsolePanel isLive={state.history.isLive} />;
      case 'joystick':
        return <JoystickPanel />;
      case 'expressions':
        return (
          <ExprEditor
            customFields={state.customFields}
            activeFields={state.activeFields}
            onAdd={(field) => {
              state.setCustomFields(prev => {
                const existing = prev.findIndex(item => item.id === field.id);
                if (existing >= 0) {
                  const next = [...prev];
                  next[existing] = field;
                  return next;
                }
                return [...prev, field];
              });
              state.setActiveFields(prev =>
                prev.includes(field.id) ? prev : [...prev, field.id],
              );
            }}
            onRemove={(id) => {
              state.setCustomFields(prev => prev.filter(f => f.id !== id));
              state.setActiveFields(prev => prev.filter(fid => fid !== id));
            }}
            onToggle={(id) => {
              state.setActiveFields(prev =>
                prev.includes(id) ? prev.filter(fid => fid !== id) : [...prev, id],
              );
            }}
          />
        );
    }
  };

  return (
    <div className="flex flex-col w-screen h-screen overflow-hidden bg-scope-bg">
      <input
        ref={fileInputRef}
        type="file"
        accept=".json,application/json"
        className="hidden"
        onChange={handleImportFile}
      />

      <ControlBar
        status={state.status}
        runtimeMode={state.runtimeMode}
        schemaMismatch={state.schemaMismatch}
        isLive={state.history.isLive}
        isPlaying={state.isPlaying}
        playbackRate={state.playbackRate}
        historyLength={state.history.length}
        scrubIndex={state.history.scrubIndex}
        durationSec={state.history.length * 0.02}
        onSeek={telemetryStore.seek.bind(telemetryStore)}
        onTogglePlay={() => state.setIsPlaying(v => !v)}
        onStep={(delta) => telemetryStore.stepBy(delta)}
        onRateChange={state.setPlaybackRate}
        onSnapToLive={telemetryStore.snapToLive.bind(telemetryStore)}
        onClearHistory={state.handleClear}
        showVectors={state.showVectors}
        showTrail={state.showTrail}
        onToggleVectors={() => state.setShowVectors(v => !v)}
        onToggleTrail={() => state.setShowTrail(v => !v)}
        onResetWorkspace={state.resetWorkspace}
        onResetSimulation={state.resetSimulation}
        onExportSession={handleExportSession}
        onImportSession={handleImportClick}
        sessionError={state.sessionError}
        bookmarkCount={state.bookmarks.length}
        analysisRange={state.analysisRange}
        onAddBookmark={() => state.toggleBookmark(state.history.scrubIndex)}
        onPrevBookmark={state.jumpToPreviousBookmark}
        onNextBookmark={state.jumpToNextBookmark}
        onClearRange={state.clearAnalysisRange}
      />

      <div className="flex-1 min-h-0 p-1">
        <WorkspaceShell
          leftPct={state.workspace.leftPct}
          slotHeights={state.workspace.slotHeights}
          slots={state.workspace.slots}
          paneTitles={paneTitles}
          paneChoices={['arena', 'detailer', 'graph', 'inspector', 'console', 'joystick', 'expressions']}
          onLeftPctChange={state.setLeftPct}
          onSlotHeightChange={state.setSlotHeight}
          onPaneChange={state.setPaneForSlot}
          onSwapSlots={state.swapSlots}
          renderPane={renderPane}
        />
      </div>
    </div>
  );
}
