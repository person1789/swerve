import { useEffect, useMemo, useState, useCallback } from 'react';
import type { DerivedField } from '../types/telemetry';
import { evalExpr, BUILTIN_EXPRS } from '../lib/ExprEval';
import { telemetryStore } from '../store/telemetryStore';

interface ExprEditorProps {
  customFields: DerivedField[];
  activeFields: string[];
  onAdd: (field: DerivedField) => void;
  onRemove: (id: string) => void;
  onToggle: (id: string) => void;
}

const PALETTE = [
  '#f43f5e', '#fb923c', '#facc15', '#4ade80',
  '#34d399', '#22d3ee', '#60a5fa', '#a78bfa',
  '#e879f9', '#f87171',
];

function FieldRow({
  field,
  active,
  removable,
  onToggle,
  onRemove,
  onEdit,
}: {
  field: DerivedField;
  active: boolean;
  removable: boolean;
  onToggle: () => void;
  onRemove?: () => void;
  onEdit?: () => void;
}) {
  const sampleFrame = telemetryStore.getBuffer()[telemetryStore.getScrubIndex()] ?? null;
  const preview = sampleFrame ? evalExpr(field.expr, sampleFrame) : null;
  const isNaNValue = preview !== null && isNaN(preview);

  return (
    <div className={`flex items-center gap-2 px-2 py-1.5 border-b border-scope-border hover:bg-scope-surface transition-colors ${active ? '' : 'opacity-50'}`}>
      <button
        onClick={onToggle}
        className="w-3 h-3 rounded-sm flex-shrink-0 transition-opacity"
        style={{ background: field.color, opacity: active ? 1 : 0.3 }}
        title={active ? 'Hide' : 'Show'}
      />

      <span className="text-[9px] font-mono text-scope-text w-24 truncate flex-shrink-0">
        {field.name}
      </span>

      <span className="text-[9px] font-mono text-scope-muted flex-1 truncate">
        {field.expr}
        {field.unit && <span className="text-scope-muted opacity-50"> [{field.unit}]</span>}
      </span>

      <span className={`text-[9px] font-mono readout w-16 text-right flex-shrink-0 ${isNaNValue ? 'text-red-400' : 'text-scope-text'}`}>
        {preview !== null ? (isNaNValue ? 'ERR' : preview.toFixed(3)) : '---'}
      </span>

      {onEdit && (
        <button onClick={onEdit} className="text-[9px] text-scope-muted hover:text-scope-accent transition-colors flex-shrink-0" title="Edit">
          edit
        </button>
      )}

      {removable && onRemove && (
        <button
          onClick={onRemove}
          className="text-[9px] text-scope-muted hover:text-red-400 transition-colors flex-shrink-0"
          title="Remove"
        >
          x
        </button>
      )}
    </div>
  );
}

function AddFieldForm({
  onAdd,
  editField,
  onCancelEdit,
}: {
  onAdd: (f: DerivedField) => void;
  editField: DerivedField | null;
  onCancelEdit: () => void;
}) {
  const [name, setName] = useState('');
  const [expr, setExpr] = useState('');
  const [unit, setUnit] = useState('');
  const [color, setColor] = useState(PALETTE[0]);
  const [error, setError] = useState('');

  const sampleFrame = telemetryStore.getBuffer()[telemetryStore.getScrubIndex()] ?? null;

  const currentPreview = useMemo(() => {
    if (!sampleFrame || !expr.trim()) return null;
    return evalExpr(expr, sampleFrame);
  }, [sampleFrame, expr]);

  const validate = useCallback(() => {
    if (!name.trim()) return 'Name is required';
    if (!expr.trim()) return 'Expression is required';
    if (sampleFrame) {
      const value = evalExpr(expr, sampleFrame);
      if (isNaN(value)) return 'Expression returned NaN - check syntax';
    }
    return '';
  }, [name, expr, sampleFrame]);

  const submit = useCallback(() => {
    const err = validate();
    if (err) {
      setError(err);
      return;
    }
    onAdd({
      id: editField?.id ?? `custom_${Date.now()}`,
      name: name.trim(),
      expr: expr.trim(),
      unit: unit.trim(),
      color,
    });
    setName('');
    setExpr('');
    setUnit('');
    setColor(PALETTE[0]);
    setError('');
    onCancelEdit();
  }, [validate, onAdd, editField, name, expr, unit, color, onCancelEdit]);

  useEffect(() => {
    if (!editField) return;
    setName(editField.name);
    setExpr(editField.expr);
    setUnit(editField.unit);
    setColor(editField.color);
    setError('');
  }, [editField]);

  return (
    <div className="px-2 py-2 border-t border-scope-border bg-scope-surface flex-shrink-0">
      <div className="flex items-center justify-between mb-2">
        <div className="text-[8px] font-mono text-scope-muted uppercase tracking-widest">
          {editField ? 'Edit Expression Field' : 'New Expression Field'}
        </div>
        {editField && (
          <button onClick={onCancelEdit} className="btn btn-ghost text-[8px] py-0.5 px-2">
            cancel
          </button>
        )}
      </div>

      <div className="flex flex-col gap-1.5">
        <div className="flex gap-1.5">
          <input
            className="expr-input flex-1"
            placeholder="Name"
            value={name}
            onChange={e => { setName(e.target.value); setError(''); }}
          />
          <input
            className="expr-input w-14"
            placeholder="unit"
            value={unit}
            onChange={e => setUnit(e.target.value)}
          />
        </div>

        <input
          className={`expr-input ${error ? 'expr-error' : ''}`}
          placeholder="Expression  e.g.  hypot(driveX, driveY)"
          value={expr}
          onChange={e => { setExpr(e.target.value); setError(''); }}
          onKeyDown={e => e.key === 'Enter' && submit()}
        />

        <div className="grid grid-cols-2 gap-2 text-[8px] font-mono">
          <div className="rounded border border-scope-border bg-scope-bg px-2 py-1">
            <div className="text-scope-muted">preview</div>
            <div className={`${currentPreview !== null && isNaN(currentPreview) ? 'text-red-400' : 'text-scope-text'} readout`}>
              {currentPreview === null ? '---' : isNaN(currentPreview) ? 'ERR' : currentPreview.toFixed(4)}
            </div>
          </div>
          <div className="rounded border border-scope-border bg-scope-bg px-2 py-1">
            <div className="text-scope-muted">field id</div>
            <div className="text-scope-text readout">{editField?.id ?? 'new custom field'}</div>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <div className="flex gap-1">
            {PALETTE.map(c => (
              <button
                key={c}
                onClick={() => setColor(c)}
                className="w-4 h-4 rounded-sm transition-transform"
                style={{
                  background: c,
                  transform: color === c ? 'scale(1.3)' : 'scale(1)',
                  outline: color === c ? `1px solid ${c}` : 'none',
                  outlineOffset: '2px',
                }}
              />
            ))}
          </div>
          <div className="flex-1" />
          <button onClick={submit} className="btn btn-accent text-[9px]">
            {editField ? 'Save Field' : 'Add Field'}
          </button>
        </div>

        {error && <span className="text-[9px] font-mono text-red-400">{error}</span>}
      </div>

      <details className="mt-2">
        <summary className="text-[8px] font-mono text-scope-muted cursor-pointer select-none">
          Available variables
        </summary>
        <div className="text-[8px] font-mono text-scope-muted mt-1 grid grid-cols-2 gap-x-4 gap-y-0.5">
          {[
            't', 'x', 'y', 'heading', 'headingDeg', 'speed',
            'battery', 'loopMs', 'driveX', 'driveY', 'turn',
            'm0_speed', 'm1_speed', 'm2_speed', 'm3_speed',
            'm0_angle', 'm1_angle', 'm2_angle', 'm3_angle',
            'abs', 'sqrt', 'sin', 'cos', 'atan2', 'hypot', 'PI',
          ].map(v => (
            <span key={v} className="text-scope-accent opacity-70">{v}</span>
          ))}
        </div>
      </details>
    </div>
  );
}

export default function ExprEditor({
  customFields,
  activeFields,
  onAdd,
  onRemove,
  onToggle,
}: ExprEditorProps) {
  const [editingFieldId, setEditingFieldId] = useState<string | null>(null);
  const editField = customFields.find(field => field.id === editingFieldId) ?? null;

  return (
    <div className="flex flex-col h-full min-h-0 w-full bg-scope-bg overflow-hidden">
      <div className="flex-1 min-h-0 overflow-y-auto overflow-x-hidden">
        <div className="px-2 py-1 text-[8px] font-mono text-scope-muted uppercase tracking-widest border-b border-scope-border">
          Built-in
        </div>
        {BUILTIN_EXPRS.map(f => (
          <FieldRow
            key={f.id}
            field={f}
            active={activeFields.includes(f.id)}
            removable={false}
            onToggle={() => onToggle(f.id)}
          />
        ))}

        {customFields.length > 0 && (
          <>
            <div className="px-2 py-1 text-[8px] font-mono text-scope-muted uppercase tracking-widest border-b border-scope-border mt-1">
              Custom
            </div>
            {customFields.map(f => (
              <FieldRow
                key={f.id}
                field={f}
                active={activeFields.includes(f.id)}
                removable={true}
                onToggle={() => onToggle(f.id)}
                onRemove={() => {
                  if (editingFieldId === f.id) setEditingFieldId(null);
                  onRemove(f.id);
                }}
                onEdit={() => setEditingFieldId(f.id)}
              />
            ))}
          </>
        )}
      </div>

      <AddFieldForm
        onAdd={onAdd}
        editField={editField}
        onCancelEdit={() => setEditingFieldId(null)}
      />
    </div>
  );
}
