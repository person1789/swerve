import React, { useEffect, useMemo, useState } from 'react';
import { loadJson, saveJson } from '../lib/localStore';

interface ConfigPanelProps {
  config: Record<string, unknown>;
  ws: WebSocket | null;
}

const FAVORITES_KEY = 'swervescope.config.favorites';

export function ConfigPanel({ config, ws }: ConfigPanelProps) {
  const [search, setSearch] = useState('');
  const [favorites, setFavorites] = useState<string[]>(() => loadJson(FAVORITES_KEY, []));
  const [showFavoritesOnly, setShowFavoritesOnly] = useState(false);
  const [draftValues, setDraftValues] = useState<Record<string, unknown>>({});
  const [defaults, setDefaults] = useState<Record<string, unknown> | null>(null);
  const [notes, setNotes] = useState(() => loadJson('swervescope.config.notes', ''));

  useEffect(() => { saveJson(FAVORITES_KEY, favorites); }, [favorites]);
  useEffect(() => { saveJson('swervescope.config.notes', notes); }, [notes]);

  useEffect(() => {
    if (!defaults && Object.keys(config).length > 0) {
      setDefaults(config);
    }
  }, [config, defaults]);

  const send = (key: string, value: unknown) => {
    setDraftValues(prev => ({ ...prev, [key]: value }));
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ type: 'config_update', key, value }));
    }
  };

  const groupedEntries = useMemo(() => {
    const favoriteSet = new Set(favorites);
    const lowered = search.trim().toLowerCase();
    const groups: Record<string, Array<[string, unknown]>> = {};

    Object.entries(config).forEach(([key, value]) => {
      const matchesSearch = !lowered || key.toLowerCase().includes(lowered) || String(value).toLowerCase().includes(lowered);
      const matchesFavorite = !showFavoritesOnly || favoriteSet.has(key);
      if (!matchesSearch || !matchesFavorite) return;

      const group = key.includes('_') ? key.split('_')[0] : key.split(/[A-Z]/)[0] || 'misc';
      if (!groups[group]) groups[group] = [];
      groups[group].push([key, value]);
    });

    return Object.entries(groups)
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([group, entries]) => ({
        group,
        entries: entries.sort(([a], [b]) => a.localeCompare(b)),
      }));
  }, [config, favorites, search, showFavoritesOnly]);

  return (
    <div style={{ width: '100%', height: '100%', display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
      <div style={{ padding: '0.5rem', flexShrink: 0, display: 'grid', gap: '0.4rem', borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
        <div style={{ display: 'flex', gap: '0.35rem' }}>
          <input
            type="text"
            placeholder="Search config..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            style={{
              flex: 1,
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
          <button onClick={() => setShowFavoritesOnly(prev => !prev)} style={{ ...smallBtn, color: showFavoritesOnly ? '#ffd740' : 'var(--text-dim)' }}>
            Favorites
          </button>
        </div>
        <textarea
          value={notes}
          onChange={(event) => setNotes(event.target.value)}
          placeholder="Tuning notes for this session"
          style={{
            minHeight: 54,
            resize: 'vertical',
            backgroundColor: 'rgba(255,255,255,0.04)',
            border: '1px solid rgba(255,255,255,0.08)',
            borderRadius: 6,
            color: 'var(--text-primary)',
            fontSize: '0.66rem',
            padding: '0.4rem 0.45rem',
            outline: 'none',
          }}
        />
      </div>

      <div style={{ flex: 1, overflow: 'auto', padding: '0 0.5rem 0.5rem' }}>
        {groupedEntries.map(({ group, entries }) => (
          <div key={group} style={{ marginTop: '0.45rem' }}>
            <div style={{ fontSize: '0.58rem', color: 'var(--text-dim)', textTransform: 'uppercase', fontWeight: 800, letterSpacing: '0.06em', marginBottom: '0.2rem' }}>
              {group}
            </div>
            {entries.map(([key, value]) => (
              <ConfigField
                key={key}
                fieldKey={key}
                value={draftValues[key] ?? value}
                defaultValue={defaults?.[key]}
                favorite={favorites.includes(key)}
                onFavoriteToggle={() => setFavorites(prev => prev.includes(key) ? prev.filter(item => item !== key) : [...prev, key].sort())}
                onReset={() => defaults && send(key, defaults[key])}
                onUpdate={send}
              />
            ))}
          </div>
        ))}
        {groupedEntries.length === 0 && (
          <div style={{ color: 'rgba(255,255,255,0.3)', fontSize: '0.7rem', padding: '0.5rem' }}>
            {Object.keys(config).length === 0 ? 'Waiting for config...' : 'No matches'}
          </div>
        )}
      </div>
    </div>
  );
}

function ConfigField({
  fieldKey,
  value,
  defaultValue,
  favorite,
  onFavoriteToggle,
  onReset,
  onUpdate,
}: {
  fieldKey: string;
  value: unknown;
  defaultValue?: unknown;
  favorite: boolean;
  onFavoriteToggle: () => void;
  onReset: () => void;
  onUpdate: (key: string, val: unknown) => void;
}) {
  const dirty = defaultValue !== undefined && String(value) !== String(defaultValue);

  let control: React.ReactNode;
  if (typeof value === 'boolean') {
    control = (
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
    );
  } else if (typeof value === 'number') {
    control = (
      <input
        type="number"
        step={value % 1 === 0 ? 1 : 0.01}
        value={value}
        onChange={(e) => onUpdate(fieldKey, parseFloat(e.target.value))}
        style={inputStyle}
      />
    );
  } else {
    control = (
      <span style={{ ...labelStyle, color: 'rgba(255,255,255,0.6)' }}>{String(value)}</span>
    );
  }

  return (
    <div style={{ ...rowStyle, background: dirty ? 'rgba(124,77,255,0.05)' : 'transparent' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', minWidth: 0, flex: 1 }}>
        <button onClick={onFavoriteToggle} style={{ ...smallBtn, color: favorite ? '#ffd740' : 'rgba(255,255,255,0.2)', padding: '0.05rem 0.25rem' }}>
          {favorite ? '★' : '☆'}
        </button>
        <span style={labelStyle}>{fieldKey}</span>
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
        {dirty && <span style={{ fontSize: '0.58rem', color: '#d9ccff' }}>dirty</span>}
        {dirty && <button onClick={onReset} style={smallBtn}>Reset</button>}
        {control}
      </div>
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
  width: '92px',
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

const smallBtn: React.CSSProperties = {
  padding: '0.15rem 0.45rem',
  fontSize: '0.6rem',
  borderRadius: 4,
  border: '1px solid rgba(255,255,255,0.08)',
  background: 'rgba(255,255,255,0.04)',
  color: 'var(--text-primary)',
  cursor: 'pointer',
};
