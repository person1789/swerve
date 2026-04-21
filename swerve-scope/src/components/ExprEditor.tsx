// ExprEditor.tsx — add/remove/edit derived telemetry expression fields
import { useState, useCallback } from 'react';
import type { DerivedField } from '../types/telemetry';
import { evalExpr, BUILTIN_EXPRS } from '../lib/ExprEval';
import type { TelemetryFrame } from '../types/telemetry';

interface ExprEditorProps {
  customFields:    DerivedField[];
  activeFields:    string[];
  onAdd:           (field: DerivedField) => void;
  onRemove:        (id: string) => void;
  onToggle:        (id: string) => void;
  sampleFrame:     TelemetryFrame | null;
}

const PALETTE = [
  '#f43f5e','#fb923c','#facc15','#4ade80',
  '#34d399','#22d3ee','#60a5fa','#a78bfa',
  '#e879f9','#f87171',
];

function FieldRow({
  field,
  active,
  removable,
  onToggle,
  onRemove,
  sampleFrame,
}: {
  field: DerivedField;
  active: boolean;
  removable: boolean;
  onToggle: () => void;
  onRemove?: () => void;
  sampleFrame: TelemetryFrame | null;
}) {
  const preview = sampleFrame ? evalExpr(field.expr, sampleFrame) : null;
  const isNaN_  = preview !== null && isNaN(preview);

  return (
    <div className={`flex items-center gap-2 px-2 py-1.5 border-b border-scope-border hover:bg-scope-surface transition-colors ${active ? '' : 'opacity-50'}`}>
      {/* Color swatch / toggle */}
      <button
        onClick={onToggle}
        className="w-3 h-3 rounded-sm flex-shrink-0 transition-opacity"
        style={{ background: field.color, opacity: active ? 1 : 0.3 }}
        title={active ? 'Hide' : 'Show'}
      />

      {/* Name */}
      <span className="text-[9px] font-mono text-scope-text w-24 truncate flex-shrink-0">
        {field.name}
      </span>

      {/* Expression */}
      <span className="text-[9px] font-mono text-scope-muted flex-1 truncate">
        {field.expr}
        {field.unit && <span className="text-scope-muted opacity-50"> [{field.unit}]</span>}
      </span>

      {/* Live preview */}
      <span className={`text-[9px] font-mono readout w-16 text-right flex-shrink-0 ${isNaN_ ? 'text-red-400' : 'text-scope-text'}`}>
        {preview !== null ? (isNaN_ ? 'ERR' : preview.toFixed(3)) : '---'}
      </span>

      {/* Remove */}
      {removable && onRemove && (
        <button
          onClick={onRemove}
          className="text-[9px] text-scope-muted hover:text-red-400 transition-colors flex-shrink-0"
          title="Remove"
        >
          ✕
        </button>
      )}
    </div>
  );
}

// ── New field form ────────────────────────────────────────────────────────────
function AddFieldForm({ onAdd, sampleFrame }: {
  onAdd: (f: DerivedField) => void;
  sampleFrame: TelemetryFrame | null;
}) {
  const [name,  setName]  = useState('');
  const [expr,  setExpr]  = useState('');
  const [unit,  setUnit]  = useState('');
  const [color, setColor] = useState(PALETTE[0]);
  const [error, setError] = useState('');

  const validate = useCallback(() => {
    if (!name.trim()) return 'Name is required';
    if (!expr.trim()) return 'Expression is required';
    if (sampleFrame) {
      const v = evalExpr(expr, sampleFrame);
      if (isNaN(v)) return 'Expression returned NaN — check syntax';
    }
    return '';
  }, [name, expr, sampleFrame]);

  const submit = useCallback(() => {
    const err = validate();
    if (err) { setError(err); return; }
    onAdd({
      id:    `custom_${Date.now()}`,
      name:  name.trim(),
      expr:  expr.trim(),
      unit:  unit.trim(),
      color,
    });
    setName(''); setExpr(''); setUnit(''); setError('');
  }, [validate, onAdd, name, expr, unit, color]);

  return (
    <div className="px-2 py-2 border-t border-scope-border bg-scope-surface">
      <div className="text-[8px] font-mono text-scope-muted uppercase tracking-widest mb-2">
        New Expression Field
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

        <div className="flex items-center gap-2">
          {/* Color picker */}
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
          <button onClick={submit} className="btn btn-accent text-[9px]">Add Field</button>
        </div>

        {error && <span className="text-[9px] font-mono text-red-400">{error}</span>}
      </div>

      {/* Variable reference */}
      <details className="mt-2">
        <summary className="text-[8px] font-mono text-scope-muted cursor-pointer select-none">
          Available variables ▸
        </summary>
        <div className="text-[8px] font-mono text-scope-muted mt-1 grid grid-cols-2 gap-x-4 gap-y-0.5">
          {[
            't','x','y','heading','headingDeg','speed',
            'battery','loopMs','driveX','driveY','turn',
            'm0_speed','m1_speed','m2_speed','m3_speed',
            'm0_angle','m1_angle','m2_angle','m3_angle',
            'abs','sqrt','sin','cos','atan2','hypot','PI',
          ].map(v => (
            <span key={v} className="text-scope-accent opacity-70">{v}</span>
          ))}
        </div>
      </details>
    </div>
  );
}

// ── Main ───────────────────────────────────────────────────────────────────
export default function ExprEditor({
  customFields, activeFields,
  onAdd, onRemove, onToggle,
  sampleFrame,
}: ExprEditorProps) {
  return (
    <div className="flex flex-col h-full bg-scope-bg overflow-hidden">
      <div className="flex-1 overflow-y-auto">
        {/* Built-in fields */}
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
            sampleFrame={sampleFrame}
          />
        ))}

        {/* Custom fields */}
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
                onRemove={() => onRemove(f.id)}
                sampleFrame={sampleFrame}
              />
            ))}
          </>
        )}
      </div>

      <AddFieldForm onAdd={onAdd} sampleFrame={sampleFrame} />
    </div>
  );
}
