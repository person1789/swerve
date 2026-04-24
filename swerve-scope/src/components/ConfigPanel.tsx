import React, { useState } from 'react';

interface ConfigPanelProps {
  config: Record<string, unknown>;
  ws: WebSocket | null;
}

export function ConfigPanel({ config, ws }: ConfigPanelProps) {
  const [search, setSearch] = useState('');

  const send = (key: string, value: unknown) => {
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ type: 'config_update', key, value }));
    }
  };

  const entries = Object.entries(config).filter(([key]) =>
    key.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div style={{ width: '100%', height: '100%', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
      {/* Search */}
      <div style={{ padding: '0.5rem', flexShrink: 0 }}>
        <input
          type="text"
          placeholder="Search config..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          style={{
            width: '100%',
            padding: '0.35rem 0.5rem',
            backgroundColor: 'rgba(255,255,255,0.06)',
            border: '1px solid rgba(255,255,255,0.1)',
            borderRadius: '4px',
            color: 'white',
            fontSize: '0.7rem',
            fontFamily: 'var(--font-mono)',
            outline: 'none',
            boxSizing: 'border-box',
          }}
        />
      </div>

      {/* Fields */}
      <div style={{ flex: 1, overflow: 'auto', padding: '0 0.5rem 0.5rem' }}>
        {entries.map(([key, value]) => (
          <ConfigField key={key} fieldKey={key} value={value} onUpdate={send} />
        ))}
        {entries.length === 0 && (
          <div style={{ color: 'rgba(255,255,255,0.3)', fontSize: '0.7rem', padding: '0.5rem' }}>
            {Object.keys(config).length === 0 ? 'Waiting for config...' : 'No matches'}
          </div>
        )}
      </div>
    </div>
  );
}

function ConfigField({ fieldKey, value, onUpdate }: { fieldKey: string; value: unknown; onUpdate: (key: string, val: unknown) => void }) {
  if (typeof value === 'boolean') {
    return (
      <div style={rowStyle}>
        <span style={labelStyle}>{fieldKey}</span>
        <button
          onClick={() => onUpdate(fieldKey, !value)}
          style={{
            ...toggleStyle,
            backgroundColor: value ? 'rgba(0,230,118,0.2)' : 'rgba(255,82,82,0.2)',
            color: value ? '#00e676' : '#ff5252',
            borderColor: value ? 'rgba(0,230,118,0.3)' : 'rgba(255,82,82,0.3)',
          }}
        >
          {value ? 'ON' : 'OFF'}
        </button>
      </div>
    );
  }

  if (typeof value === 'number') {
    return (
      <div style={rowStyle}>
        <span style={labelStyle}>{fieldKey}</span>
        <input
          type="number"
          step={value % 1 === 0 ? 1 : 0.01}
          value={value}
          onChange={(e) => onUpdate(fieldKey, parseFloat(e.target.value))}
          style={inputStyle}
        />
      </div>
    );
  }

  // Fallback: display as text
  return (
    <div style={rowStyle}>
      <span style={labelStyle}>{fieldKey}</span>
      <span style={{ ...labelStyle, color: 'rgba(255,255,255,0.6)' }}>{String(value)}</span>
    </div>
  );
}

const rowStyle: React.CSSProperties = {
  display: 'flex',
  justifyContent: 'space-between',
  alignItems: 'center',
  padding: '0.3rem 0',
  borderBottom: '1px solid rgba(255,255,255,0.04)',
  gap: '0.5rem',
};

const labelStyle: React.CSSProperties = {
  fontSize: '0.65rem',
  fontFamily: 'var(--font-mono)',
  color: 'rgba(255,255,255,0.5)',
  whiteSpace: 'nowrap',
  overflow: 'hidden',
  textOverflow: 'ellipsis',
};

const inputStyle: React.CSSProperties = {
  width: '80px',
  padding: '0.2rem 0.35rem',
  backgroundColor: 'rgba(255,255,255,0.06)',
  border: '1px solid rgba(255,255,255,0.1)',
  borderRadius: '4px',
  color: 'white',
  fontSize: '0.65rem',
  fontFamily: 'var(--font-mono)',
  textAlign: 'right' as const,
  outline: 'none',
};

const toggleStyle: React.CSSProperties = {
  padding: '0.15rem 0.45rem',
  fontSize: '0.6rem',
  fontWeight: 700,
  border: '1px solid',
  borderRadius: '4px',
  cursor: 'pointer',
  background: 'none',
};
