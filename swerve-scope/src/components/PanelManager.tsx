import React, { useState } from 'react';
import { Panel, Group, Separator } from 'react-resizable-panels';
import { FieldView } from './FieldView';
import { GraphView } from './GraphView';
import { ModuleGauges } from './ModuleGauges';
import { GamepadView } from './GamepadView';
import type { KeyBindings } from '../hooks/useGamepad';

interface PanelManagerProps {
  telemetry: Record<string, any>;
  bindings: KeyBindings;
  onUpdateBindings: (newBindings: KeyBindings) => void;
}

const WIDGETS: Record<string, { label: string, render: (props: PanelManagerProps) => React.ReactNode }> = {
  field: { label: 'Field View', render: (p) => <FieldView telemetry={p.telemetry} /> },
  graph: { label: 'Telemetry Graph', render: (p) => <GraphView telemetry={p.telemetry} /> },
  gauges: { label: 'Module Gauges', render: (p) => <ModuleGauges modules={p.telemetry.modules} /> },
  gamepad: { label: 'Gamepad & Config', render: (p) => <GamepadView bindings={p.bindings} onUpdateBindings={p.onUpdateBindings} /> },
  raw: { 
    label: 'Raw Telemetry', 
    render: (p) => (
      <div style={{ width: '100%', height: '100%', overflow: 'auto', backgroundColor: 'rgba(0,0,0,0.2)', padding: '0.75rem', borderRadius: '0.5rem', fontFamily: 'monospace', fontSize: '0.75rem', color: 'rgba(255,255,255,0.7)', whiteSpace: 'pre-wrap' }}>
        {JSON.stringify(p.telemetry, null, 2)}
      </div>
    ) 
  }
};

function WidgetWrapper({ initialWidget, props }: { initialWidget: string, props: PanelManagerProps }) {
  const [activeWidget, setActiveWidget] = useState(initialWidget);

  return (
    <div style={{ width: '100%', height: '100%', display: 'flex', flexDirection: 'column', padding: '0.5rem' }}>
      <div className="glass-panel" style={{ flex: 1, display: 'flex', flexDirection: 'column', minHeight: 0 }}>
        {/* Header with Dropdown */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.5rem 1rem', borderBottom: '1px solid rgba(255,255,255,0.05)', backgroundColor: 'rgba(0,0,0,0.2)' }}>
          <select 
            value={activeWidget} 
            onChange={(e) => setActiveWidget(e.target.value)}
            style={{ backgroundColor: 'transparent', border: 'none', color: 'rgba(255,255,255,0.8)', fontSize: '0.75rem', fontWeight: 'bold', textTransform: 'uppercase', outline: 'none', cursor: 'pointer' }}
          >
            {Object.entries(WIDGETS).map(([key, w]) => (
              <option key={key} value={key} style={{ color: 'black' }}>{w.label}</option>
            ))}
          </select>
        </div>
        {/* Content */}
        <div style={{ flex: 1, position: 'relative', overflow: 'hidden', padding: '0.5rem' }}>
          {WIDGETS[activeWidget]?.render(props)}
        </div>
      </div>
    </div>
  );
}

function ResizeHandle() {
  return (
    <Separator style={{ width: '8px', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'col-resize' }}>
      <div style={{ width: '2px', height: '24px', backgroundColor: 'rgba(255,255,255,0.2)', borderRadius: '2px' }} />
    </Separator>
  );
}

function VResizeHandle() {
  return (
    <Separator style={{ height: '8px', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'row-resize' }}>
      <div style={{ height: '2px', width: '24px', backgroundColor: 'rgba(255,255,255,0.2)', borderRadius: '2px' }} />
    </Separator>
  );
}

export function PanelManager(props: PanelManagerProps) {
  return (
    <Group direction="horizontal">
      <Panel defaultSize={60} minSize={30}>
        <Group direction="vertical">
          <Panel defaultSize={65} minSize={20}>
            <WidgetWrapper initialWidget="field" props={props} />
          </Panel>
          <VResizeHandle />
          <Panel defaultSize={35} minSize={20}>
            <WidgetWrapper initialWidget="graph" props={props} />
          </Panel>
        </Group>
      </Panel>
      
      <ResizeHandle />
      
      <Panel defaultSize={40} minSize={30}>
        <Group direction="vertical">
          <Panel defaultSize={50} minSize={20}>
            <WidgetWrapper initialWidget="gauges" props={props} />
          </Panel>
          <VResizeHandle />
          <Panel defaultSize={50} minSize={20}>
            <WidgetWrapper initialWidget="gamepad" props={props} />
          </Panel>
        </Group>
      </Panel>
    </Group>
  );
}
