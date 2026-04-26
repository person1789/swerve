import React from 'react';

interface OpModeSelectorProps {
  availableOpModes: string[];
  activeOpMode: string;
  opModeState: string;
  ws: WebSocket | null;
}

export function OpModeSelector({ availableOpModes, activeOpMode, opModeState, ws }: OpModeSelectorProps) {
  const send = (msg: Record<string, unknown>) => {
    if (ws && ws.readyState === WebSocket.OPEN) ws.send(JSON.stringify(msg));
  };

  const isStopped = opModeState === 'STOPPED';
  const isInit = opModeState === 'INITIALIZED';
  const isRunning = opModeState === 'RUNNING';
  const isInitFailed = opModeState === 'INIT_FAILED';

  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
      <select
        value={activeOpMode || ''}
        onChange={(e) => send({ type: 'opmode_select', name: e.target.value })}
        disabled={isRunning}
        style={{
          backgroundColor: 'rgba(255,255,255,0.08)',
          border: '1px solid rgba(255,255,255,0.1)',
          color: 'white',
          padding: '0.3rem 0.6rem',
          borderRadius: '6px',
          fontSize: '0.75rem',
          fontWeight: 600,
          cursor: isRunning ? 'not-allowed' : 'pointer',
          outline: 'none',
          opacity: isRunning ? 0.5 : 1,
        }}
      >
        <option value="" style={{ color: '#222' }}>Select OpMode</option>
        {availableOpModes.map((name) => (
          <option key={name} value={name} style={{ color: '#222' }}>{name}</option>
        ))}
      </select>

      <button
        onClick={() => send({ type: 'opmode_control', action: 'INIT' })}
        disabled={!activeOpMode || isInit || isRunning}
        style={{
          ...btnStyle,
          backgroundColor: (isStopped || isInitFailed) && activeOpMode ? 'rgba(255,215,64,0.2)' : 'rgba(255,255,255,0.05)',
          color: (isStopped || isInitFailed) && activeOpMode ? '#ffd740' : 'rgba(255,255,255,0.3)',
          borderColor: (isStopped || isInitFailed) && activeOpMode ? 'rgba(255,215,64,0.3)' : 'rgba(255,255,255,0.1)',
          cursor: (isStopped || isInitFailed) && activeOpMode ? 'pointer' : 'not-allowed',
        }}
      >
        INIT
      </button>

      <button
        onClick={() => send({ type: 'opmode_control', action: 'START' })}
        disabled={!isInit}
        style={{
          ...btnStyle,
          backgroundColor: isInit ? 'rgba(0,230,118,0.2)' : 'rgba(255,255,255,0.05)',
          color: isInit ? '#00e676' : 'rgba(255,255,255,0.3)',
          borderColor: isInit ? 'rgba(0,230,118,0.3)' : 'rgba(255,255,255,0.1)',
          cursor: isInit ? 'pointer' : 'not-allowed',
        }}
      >
        START
      </button>

      <button
        onClick={() => send({ type: 'opmode_control', action: 'STOP' })}
        disabled={isStopped}
        style={{
          ...btnStyle,
          backgroundColor: !isStopped ? 'rgba(255,82,82,0.2)' : 'rgba(255,255,255,0.05)',
          color: !isStopped ? '#ff5252' : 'rgba(255,255,255,0.3)',
          borderColor: !isStopped ? 'rgba(255,82,82,0.3)' : 'rgba(255,255,255,0.1)',
          cursor: !isStopped ? 'pointer' : 'not-allowed',
        }}
      >
        STOP
      </button>

      <span style={{
        fontSize: '0.65rem',
        fontWeight: 700,
        padding: '0.15rem 0.5rem',
        borderRadius: 9999,
        backgroundColor: isRunning
          ? 'rgba(0,230,118,0.15)'
          : isInit
            ? 'rgba(255,215,64,0.15)'
            : isInitFailed
              ? 'rgba(255,82,82,0.16)'
              : 'rgba(255,255,255,0.05)',
        color: isRunning
          ? '#00e676'
          : isInit
            ? '#ffd740'
            : isInitFailed
              ? '#ff8a80'
              : 'rgba(255,255,255,0.4)',
      }}>
        ● {opModeState}
      </span>
    </div>
  );
}

const btnStyle: React.CSSProperties = {
  padding: '0.25rem 0.6rem',
  fontSize: '0.65rem',
  fontWeight: 700,
  border: '1px solid',
  borderRadius: '6px',
  background: 'none',
  letterSpacing: '0.04em',
};
