import React, { useState } from 'react';
import {
  Mosaic,
  MosaicWindow,
} from 'react-mosaic-component';
import type { MosaicNode } from 'react-mosaic-component';
import 'react-mosaic-component/react-mosaic-component.css';
import '../styles/mosaic-overrides.css';

import SwerveSim from './SwerveSim';
import SwerveScopes from './SwerveScopes';
import TelemetryPanel from './TelemetryPanel';
import type { TelemetryEntry } from '../types/telemetry';

export type ViewId = 'arena' | 'graphs' | 'inspector' | 'console';

interface MosaicShellProps {
  showModuleVectors: boolean;
  onTelemetryUpdate: (data: any) => void;
  currentFrame: TelemetryEntry | null;
  history: TelemetryEntry[];
  isLive: boolean;
  isRecording: boolean;
  onStartRecording: () => void;
  onStopRecording: () => void;
  onClear: () => void;
  onExport: () => void;
}

const TITLE_MAP: Record<ViewId, string> = {
  arena: 'Swerve Arena // WorldView',
  graphs: 'Diagnostic Scopes // Telemetry',
  inspector: 'Inspection Engine // Packets',
  console: 'System Console // Logs'
};

const MosaicShell: React.FC<MosaicShellProps> = (props) => {
  const [currentNode, setCurrentNode] = useState<MosaicNode<ViewId> | null>({
    type: 'split',
    direction: 'row',
    children: [
      'arena',
      {
        type: 'split',
        direction: 'column',
        children: ['inspector', 'graphs'],
        splitPercentages: [50, 50],
      },
    ],
    splitPercentages: [66, 34],
  });

  const renderTile = (id: ViewId) => {
    switch (id) {
      case 'arena':
        return (
          <SwerveSim 
            showModuleVectors={props.showModuleVectors}
            onTelemetryUpdate={props.onTelemetryUpdate}
            frameData={props.currentFrame}
          />
        );
      case 'graphs':
        return <SwerveScopes history={props.history} isLive={props.isLive} />;
      case 'inspector':
        return (
          <TelemetryPanel 
            data={props.isLive ? props.history.slice(-500) : [props.currentFrame].filter(Boolean) as any}
            isRecording={props.isRecording}
            onStartRecording={props.onStartRecording}
            onStopRecording={props.onStopRecording}
            onClear={props.onClear}
            onExport={props.onExport}
          />
        );
      case 'console':
        return (
          <div className="h-full bg-slate-950 p-4 font-mono text-[10px] text-slate-500">
            System Console Initializing... [Phase 1 Foundation Active]
          </div>
        );
      default:
        return <div>Unknown View</div>;
    }
  };

  return (
    <div className="flex-1 overflow-hidden bg-slate-950 swatch-mosaic">
      <Mosaic<ViewId>
        renderTile={(id, path) => (
          <MosaicWindow<ViewId>
            path={path}
            title={TITLE_MAP[id]}
            className="mosaic-blueprint-theme"
          >
            {renderTile(id)}
          </MosaicWindow>
        )}
        value={currentNode}
        onChange={setCurrentNode}
      />
    </div>
  );
};

export default MosaicShell;
