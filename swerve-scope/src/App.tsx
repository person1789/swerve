// App.tsx - SwerveScope canonical docked workspace shell
import { useEffect } from 'react';
import type { WorkspacePaneId } from './types/telemetry';
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

export default function App() {
  const state = useSwerveScopeState();

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
          state.history.snapToLive();
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
          if (!state.history.isLive) state.history.stepBy(-1);
          break;
        case 'ArrowRight':
          if (!state.history.isLive) state.history.stepBy(1);
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

  const renderPane = (pane: WorkspacePaneId) => {
    switch (pane) {
      case 'arena':
        return (
          <Arena
            frame={state.frame}
            trail={state.trail}
            showVectors={state.showVectors}
            showTrail={state.showTrail}
          />
        );
      case 'detailer':
        return <ModuleDetail frame={state.frame} />;
      case 'graph':
        return (
          <ScopePanel
            history={state.history.getBuffer()}
            historyLength={state.history.length}
            isLive={state.history.isLive}
            activeFields={state.activeFields}
            customFields={state.customFields}
          />
        );
      case 'inspector':
        return (
          <InspectorPanel
            history={state.history.getBuffer()}
            historyLength={state.history.length}
            currentFrame={state.frame}
            isLive={state.history.isLive}
          />
        );
      case 'console':
        return (
          <ConsolePanel
            frame={state.frame}
            prevFrame={state.prevFrame}
            isLive={state.history.isLive}
          />
        );
      case 'joystick':
        return <JoystickPanel frame={state.frame} />;
      case 'expressions':
        return (
          <ExprEditor
            customFields={state.customFields}
            activeFields={state.activeFields}
            onAdd={(field) => {
              state.setCustomFields(prev => [...prev, field]);
              state.setActiveFields(prev => prev.includes(field.id) ? prev : [...prev, field.id]);
            }}
            onRemove={(id) => {
              state.setCustomFields(prev => prev.filter(field => field.id !== id));
              state.setActiveFields(prev => prev.filter(fieldId => fieldId !== id));
            }}
            onToggle={(id) => {
              state.setActiveFields(prev => prev.includes(id) ? prev.filter(fieldId => fieldId !== id) : [...prev, id]);
            }}
            sampleFrame={state.frame}
          />
        );
    }
  };

  return (
    <div className="flex flex-col w-screen h-screen overflow-hidden bg-scope-bg">
      <ControlBar
        status={state.status}
        schemaMismatch={state.schemaMismatch}
        isLive={state.history.isLive}
        isPlaying={state.isPlaying}
        playbackRate={state.playbackRate}
        historyLength={state.history.length}
        scrubIndex={state.history.scrubIndex}
        durationSec={state.history.length * 0.02}
        onSeek={state.history.seek}
        onTogglePlay={() => state.setIsPlaying(v => !v)}
        onStep={(delta) => state.history.stepBy(delta)}
        onRateChange={state.setPlaybackRate}
        onSnapToLive={state.history.snapToLive}
        onClearHistory={state.handleClear}
        showVectors={state.showVectors}
        showTrail={state.showTrail}
        onToggleVectors={() => state.setShowVectors(v => !v)}
        onToggleTrail={() => state.setShowTrail(v => !v)}
        onResetWorkspace={state.resetWorkspace}
      />

      <div className="flex-1 min-h-0 p-1 flex flex-col">
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
