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
import SwerveDetailer from './SwerveDetailer';
import JoystickVisualizer from './JoystickVisualizer';
import StatisticsPanel from './StatisticsPanel';
import ConsolePanel from './ConsolePanel';
import SavedSessions from './SavedSessions';
import { type SavedSession } from '../lib/db';
import type { TelemetryEntry } from '../types/telemetry';

export type ViewId = 'arena' | 'graphs' | 'inspector' | 'console' | 'detailer' | 'joystick' | 'statistics' | 'history';

interface MosaicShellProps {
  showModuleVectors: boolean;
  onTelemetryUpdate: (data: any) => void;
  onScrub: (index: number) => void;
  onLoadSession: (session: SavedSession) => void;
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
  arena: '🗺 Field View // 2D Arena',
  graphs: '📉 Line Graph // Telemetry',
  inspector: '🔢 Table // Module States',
  console: '💬 Console // System Log',
  detailer: '🦀 Swerve Detailer // Vectors',
  joystick: '🎮 Joystick // Gamepad Input',
  statistics: '📊 Statistics // Analysis',
  history: '📂 Archive // Saved Sessions',
};

const MosaicShell: React.FC<MosaicShellProps> = (props) => {
  // Default layout: Arena left, Detailer + Graphs right-top, Inspector + Console right-bottom
  const [currentNode, setCurrentNode] = useState<MosaicNode<ViewId> | null>({
    type: 'split',
    direction: 'row',
    children: [
      {
        type: 'split',
        direction: 'column',
        children: [
          'arena',
          'joystick',
        ],
        splitPercentages: [75, 25],
      },
      {
        type: 'split',
        direction: 'column',
        children: [
          {
            type: 'split',
            direction: 'row',
            children: ['detailer', 'statistics'],
            splitPercentages: [60, 40],
          },
          {
            type: 'split',
            direction: 'row',
            children: ['graphs', 'inspector'],
            splitPercentages: [55, 45],
          },
          'history',
        ],
        splitPercentages: [30, 40, 30],
      },
    ],
    splitPercentages: [55, 45],
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
            history={props.history}
            onScrub={props.onScrub}
            isRecording={props.isRecording}
            onStartRecording={props.onStartRecording}
            onStopRecording={props.onStopRecording}
            onClear={props.onClear}
            onExport={props.onExport}
          />
        );
      case 'console':
        return <ConsolePanel history={props.history} isLive={props.isLive} />;
      case 'detailer':
        return <SwerveDetailer currentFrame={props.currentFrame} />;
      case 'joystick':
        return <JoystickVisualizer currentFrame={props.currentFrame} history={props.history} />;
      case 'statistics':
        return <StatisticsPanel history={props.history} />;
      case 'history':
        return <SavedSessions onLoad={props.onLoadSession} />;
      default:
        return <div className="h-full bg-slate-950 flex items-center justify-center text-slate-600 text-xs">Unknown View</div>;
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
