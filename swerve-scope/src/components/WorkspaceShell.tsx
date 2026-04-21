import { memo, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import type { WorkspacePaneId, WorkspaceSlotId } from '../types/telemetry';

interface WorkspaceShellProps {
  leftPct: number;
  slotHeights: {
    primary: number;
    secondary: number;
    sidebarTop: number;
    sidebarMiddle: number;
    sidebarBottom: number;
  };
  slots: Record<WorkspaceSlotId, WorkspacePaneId>;
  paneTitles: Record<WorkspacePaneId, string>;
  paneChoices: WorkspacePaneId[];
  onLeftPctChange: (next: number) => void;
  onSlotHeightChange: (slot: 'primary' | 'secondary' | 'sidebarTop' | 'sidebarMiddle' | 'sidebarBottom', next: number) => void;
  onPaneChange: (slot: WorkspaceSlotId, pane: WorkspacePaneId) => void;
  onSwapSlots: (from: WorkspaceSlotId, to: WorkspaceSlotId) => void;
  renderPane: (pane: WorkspacePaneId) => ReactNode;
}

function ResizeX({ onDrag }: { onDrag: (dx: number) => void }) {
  const dragging = useRef(false);
  const lastX = useRef(0);

  return (
    <div
      className="workspace-resize-x"
      onMouseDown={event => {
        dragging.current = true;
        lastX.current = event.clientX;
        const move = (next: MouseEvent) => {
          if (!dragging.current) return;
          onDrag(next.clientX - lastX.current);
          lastX.current = next.clientX;
        };
        const up = () => {
          dragging.current = false;
          window.removeEventListener('mousemove', move);
        };
        window.addEventListener('mousemove', move);
        window.addEventListener('mouseup', up, { once: true });
      }}
    />
  );
}

function ResizeY({ onDrag }: { onDrag: (dy: number) => void }) {
  const dragging = useRef(false);
  const lastY = useRef(0);

  return (
    <div
      className="workspace-resize-y"
      onMouseDown={event => {
        dragging.current = true;
        lastY.current = event.clientY;
        const move = (next: MouseEvent) => {
          if (!dragging.current) return;
          onDrag(next.clientY - lastY.current);
          lastY.current = next.clientY;
        };
        const up = () => {
          dragging.current = false;
          window.removeEventListener('mousemove', move);
        };
        window.addEventListener('mousemove', move);
        window.addEventListener('mouseup', up, { once: true });
      }}
    />
  );
}

const WorkspacePanel = memo(function WorkspacePanel({
  slot,
  pane,
  title,
  paneTitles,
  paneChoices,
  onPaneChange,
  onSwapSlots,
  children,
}: {
  slot: WorkspaceSlotId;
  pane: WorkspacePaneId;
  title: string;
  paneTitles: Record<WorkspacePaneId, string>;
  paneChoices: WorkspacePaneId[];
  onPaneChange: (slot: WorkspaceSlotId, pane: WorkspacePaneId) => void;
  onSwapSlots: (from: WorkspaceSlotId, to: WorkspaceSlotId) => void;
  children: ReactNode;
}) {
  const [dragOver, setDragOver] = useState(false);

  return (
    <div
      className={`panel flex flex-col h-full min-h-0 overflow-hidden ${dragOver ? 'tile-drag-over' : ''}`}
      onDragOver={event => {
        event.preventDefault();
        setDragOver(true);
      }}
      onDragLeave={() => setDragOver(false)}
      onDrop={event => {
        event.preventDefault();
        const from = event.dataTransfer.getData('text/workspace-slot') as WorkspaceSlotId;
        setDragOver(false);
        if (from) onSwapSlots(from, slot);
      }}
    >
      <div className="panel-header gap-2">
        <div
          className="flex items-center gap-2 min-w-0 cursor-grab"
          draggable
          onDragStart={event => event.dataTransfer.setData('text/workspace-slot', slot)}
        >
          <span className="text-scope-muted">::</span>
          <span className="title truncate">{title}</span>
        </div>
        <select
          value={pane}
          onChange={event => onPaneChange(slot, event.target.value as WorkspacePaneId)}
          className="workspace-select"
        >
          {paneChoices.map(choice => (
            <option key={choice} value={choice}>
              {paneTitles[choice]}
            </option>
          ))}
        </select>
      </div>
      <div className="panel-body h-full min-h-0">{children}</div>
    </div>
  );
});

export default function WorkspaceShell({
  leftPct,
  slotHeights,
  slots,
  paneTitles,
  paneChoices,
  onLeftPctChange,
  onSlotHeightChange,
  onPaneChange,
  onSwapSlots,
  renderPane,
}: WorkspaceShellProps) {
  const sidebarTotal = slotHeights.sidebarTop + slotHeights.sidebarMiddle + slotHeights.sidebarBottom;
  const makePaneRenderer = (slot: WorkspaceSlotId) =>
    (pane: WorkspacePaneId) => slots[slot] === pane ? renderPane(pane) : null;

  return (
    <div className="workspace-shell">
      <div className="workspace-column" style={{ width: `${leftPct}%` }}>
        <div style={{ height: `${slotHeights.primary}%` }} className="min-h-0 overflow-hidden">
          <WorkspacePanel slot="primary" pane={slots.primary} title={paneTitles[slots.primary]} paneTitles={paneTitles} paneChoices={paneChoices} onPaneChange={onPaneChange} onSwapSlots={onSwapSlots}>
            {makePaneRenderer('primary')(slots.primary)}
          </WorkspacePanel>
        </div>
        <ResizeY onDrag={dy => {
          const delta = (dy / Math.max(window.innerHeight - 120, 1)) * 100;
          onSlotHeightChange('primary', slotHeights.primary + delta);
          onSlotHeightChange('secondary', slotHeights.secondary - delta);
        }} />
        <div style={{ height: `${slotHeights.secondary}%` }} className="min-h-0 overflow-hidden">
          <WorkspacePanel slot="secondary" pane={slots.secondary} title={paneTitles[slots.secondary]} paneTitles={paneTitles} paneChoices={paneChoices} onPaneChange={onPaneChange} onSwapSlots={onSwapSlots}>
            {makePaneRenderer('secondary')(slots.secondary)}
          </WorkspacePanel>
        </div>
      </div>

      <ResizeX onDrag={dx => onLeftPctChange(leftPct + (dx / Math.max(window.innerWidth, 1)) * 100)} />

      <div className="workspace-sidebar">
        <div style={{ height: `${(slotHeights.sidebarTop / sidebarTotal) * 100}%` }} className="min-h-0 overflow-hidden">
          <WorkspacePanel slot="sidebarTop" pane={slots.sidebarTop} title={paneTitles[slots.sidebarTop]} paneTitles={paneTitles} paneChoices={paneChoices} onPaneChange={onPaneChange} onSwapSlots={onSwapSlots}>
            {makePaneRenderer('sidebarTop')(slots.sidebarTop)}
          </WorkspacePanel>
        </div>
        <ResizeY onDrag={dy => {
          const delta = (dy / Math.max(window.innerHeight - 120, 1)) * 100;
          onSlotHeightChange('sidebarTop', slotHeights.sidebarTop + delta);
          onSlotHeightChange('sidebarMiddle', slotHeights.sidebarMiddle - delta);
        }} />
        <div style={{ height: `${(slotHeights.sidebarMiddle / sidebarTotal) * 100}%` }} className="min-h-0 overflow-hidden">
          <WorkspacePanel slot="sidebarMiddle" pane={slots.sidebarMiddle} title={paneTitles[slots.sidebarMiddle]} paneTitles={paneTitles} paneChoices={paneChoices} onPaneChange={onPaneChange} onSwapSlots={onSwapSlots}>
            {makePaneRenderer('sidebarMiddle')(slots.sidebarMiddle)}
          </WorkspacePanel>
        </div>
        <ResizeY onDrag={dy => {
          const delta = (dy / Math.max(window.innerHeight - 120, 1)) * 100;
          onSlotHeightChange('sidebarMiddle', slotHeights.sidebarMiddle + delta);
          onSlotHeightChange('sidebarBottom', slotHeights.sidebarBottom - delta);
        }} />
        <div style={{ height: `${(slotHeights.sidebarBottom / sidebarTotal) * 100}%` }} className="min-h-0 overflow-hidden">
          <WorkspacePanel slot="sidebarBottom" pane={slots.sidebarBottom} title={paneTitles[slots.sidebarBottom]} paneTitles={paneTitles} paneChoices={paneChoices} onPaneChange={onPaneChange} onSwapSlots={onSwapSlots}>
            {makePaneRenderer('sidebarBottom')(slots.sidebarBottom)}
          </WorkspacePanel>
        </div>
      </div>
    </div>
  );
}
