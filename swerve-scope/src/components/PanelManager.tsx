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
  const [active, setActive] = useState(initialWidget);
  
  return (
    <div className="glass-panel" style={{ width: '100%', height: '100%', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
      <div className="panel-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <select 
          value={active} 
          onChange={(e) => setActive(e.target.value)}
          style={{ background: 'none', border: 'none', color: 'inherit', fontSize: 'inherit', fontWeight: 'inherit', cursor: 'pointer', outline: 'none' }}
        >
          {Object.entries(WIDGETS).map(([id, w]) => (
            <option key={id} value={id} style={{ backgroundColor: 'var(--bg-primary)' }}>{w.label}</option>
          ))}
        </select>
      </div>
      <div style={{ flex: 1, minHeight: 0, padding: '0.5rem' }}>
        {WIDGETS[active].render(props)}
      </div>
    </div>
  );
}

const ResizeHandle = () => (
  <Separator style={{ width: '6px', margin: '0 -3px', zIndex: 10, cursor: 'col-resize', transition: 'background 0.2s' }} />
);

const VResizeHandle = () => (
  <Separator style={{ height: '6px', margin: '-3px 0', zIndex: 10, cursor: 'row-resize', transition: 'background 0.2s' }} />
);

export function PanelManager(props: PanelManagerProps) {
  return (
    <Group orientation="horizontal" style={{ width: '100%', height: '100%', display: 'flex' }}>
      <Panel defaultSize={60} minSize={30}>
        <Group orientation="vertical">
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
        <Group orientation="vertical">
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

