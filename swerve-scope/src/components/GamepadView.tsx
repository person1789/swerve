import React, { useState } from 'react';
import { Settings, X } from 'lucide-react';
import type { KeyBindings } from '../hooks/useGamepad';

interface GamepadViewProps {
  bindings: KeyBindings;
  onUpdateBindings: (newBindings: KeyBindings) => void;
}

export function GamepadView({ bindings, onUpdateBindings }: GamepadViewProps) {
  const [isConfigOpen, setIsConfigOpen] = useState(false);
  const [editingKey, setEditingKey] = useState<keyof KeyBindings | null>(null);

  // Quick listener for key binding
  React.useEffect(() => {
    if (!editingKey) return;
    
    const handleKeyDown = (e: KeyboardEvent) => {
      e.preventDefault();
      onUpdateBindings({ ...bindings, [editingKey]: e.key.toLowerCase() });
      setEditingKey(null);
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [editingKey, bindings, onUpdateBindings]);

  return (
    <div style={{ width: '100%', height: '100%', position: 'relative', display: 'flex', flexDirection: 'column', backgroundColor: 'rgba(0,0,0,0.2)', borderRadius: '0.5rem', border: '1px solid rgba(255,255,255,0.05)' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.75rem', borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
        <span style={{ fontSize: '0.75rem', fontWeight: 'bold', color: 'rgba(255,255,255,0.5)', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
          Input Status
        </span>
        <button 
          onClick={() => setIsConfigOpen(true)}
          style={{ background: 'none', border: 'none', color: 'rgba(255,255,255,0.5)', cursor: 'pointer', padding: '4px' }}
        >
          <Settings size={14} />
        </button>
      </div>

      {/* Main View */}
      <div style={{ flex: 1, position: 'relative', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '1rem' }}>
        {isConfigOpen ? (
          <div style={{ width: '100%', height: '100%', overflowY: 'auto', paddingRight: '8px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
              <span style={{ color: 'white', fontWeight: 'bold', fontSize: '0.875rem' }}>Keyboard Mapping</span>
              <button onClick={() => setIsConfigOpen(false)} style={{ background: 'none', border: 'none', color: 'white', cursor: 'pointer' }}><X size={16} /></button>
            </div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.5rem' }}>
              {Object.entries(bindings).map(([action, key]) => (
                <div key={action} style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                  <span style={{ fontSize: '0.65rem', color: 'rgba(255,255,255,0.5)', textTransform: 'uppercase' }}>{action}</span>
                  <button 
                    onClick={() => setEditingKey(action as keyof KeyBindings)}
                    style={{ 
                      padding: '0.5rem', 
                      background: editingKey === action ? '#7c4dff' : 'rgba(255,255,255,0.1)', 
                      border: editingKey === action ? '1px solid #b388ff' : '1px solid rgba(255,255,255,0.1)',
                      color: 'white',
                      borderRadius: '4px',
                      fontFamily: 'monospace',
                      cursor: 'pointer',
                      textAlign: 'center'
                    }}
                  >
                    {editingKey === action ? 'Press key...' : key}
                  </button>
                </div>
              ))}
            </div>
          </div>
        ) : (
          <div style={{ textAlign: 'center', color: 'rgba(255,255,255,0.5)', fontSize: '0.875rem' }}>
            <p>Connect Gamepad</p>
            <p style={{ fontSize: '0.75rem', marginTop: '0.5rem' }}>or use keyboard (WASD/JL)</p>
          </div>
        )}
      </div>
    </div>
  );
}
