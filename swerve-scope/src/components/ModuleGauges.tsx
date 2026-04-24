

interface ModuleGaugesProps {
  modules: Record<string, any>[];
}

export function ModuleGauges({ modules }: ModuleGaugesProps) {
  if (!modules || modules.length !== 4) {
    return <div style={{ color: 'rgba(255,255,255,0.3)', fontSize: '0.875rem' }}>Waiting for module data...</div>;
  }

  return (
    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', height: '100%' }}>
      {modules.map((mod, i) => (
        <div key={i} style={{
          backgroundColor: 'rgba(0,0,0,0.2)',
          border: '1px solid rgba(255,255,255,0.05)',
          borderRadius: '0.5rem',
          padding: '0.75rem',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          position: 'relative'
        }}>
          {/* Module Label */}
          <div style={{
            position: 'absolute',
            top: '0.5rem',
            left: '0.75rem',
            fontSize: '0.75rem',
            fontWeight: 'bold',
            color: 'rgba(255,255,255,0.5)',
            textTransform: 'uppercase',
            letterSpacing: '0.05em'
          }}>
            {mod.name}
          </div>
          
          {/* Rotary Dial (SVG) */}
          <div style={{ marginTop: '1.5rem', marginBottom: '0.5rem', width: '80px', height: '80px' }}>
            <svg viewBox="0 0 100 100" style={{ width: '100%', height: '100%', overflow: 'visible' }}>
              {/* Outer Ring */}
              <circle cx="50" cy="50" r="45" fill="none" stroke="rgba(255,255,255,0.1)" strokeWidth="4" />
              
              {/* Center Hub */}
              <circle cx="50" cy="50" r="4" fill="white" />

              {/* Target Angle (Red, Ghost) */}
              <g style={{ transform: `rotate(${-mod.targetAngleDegrees}deg)`, transformOrigin: '50px 50px', transition: 'transform 0.1s' }}>
                <line x1="50" y1="50" x2="50" y2="10" stroke="rgba(255,82,82,0.5)" strokeWidth="4" strokeLinecap="round" />
                <polygon points="50,2 45,12 55,12" fill="rgba(255,82,82,0.5)" />
              </g>

              {/* Current Angle (Green) */}
              <g style={{ transform: `rotate(${-mod.currentAngleDegrees}deg)`, transformOrigin: '50px 50px', transition: 'transform 0.1s' }}>
                <line x1="50" y1="50" x2="50" y2="10" stroke="#00e676" strokeWidth="4" strokeLinecap="round" />
                <polygon points="50,2 45,12 55,12" fill="#00e676" />
              </g>
            </svg>
          </div>

          {/* Speed & Power Bars */}
          <div style={{ width: '100%', display: 'flex', flexDirection: 'column', gap: '0.375rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem' }}>
              <span style={{ color: 'rgba(255,255,255,0.4)' }}>Speed</span>
              <span style={{ fontFamily: 'monospace', color: 'rgba(255,255,255,0.8)' }}>
                {Math.abs(mod.actualVelocityMps).toFixed(2)} m/s
              </span>
            </div>
            <div style={{ width: '100%', height: '6px', backgroundColor: 'rgba(255,255,255,0.1)', borderRadius: '9999px', overflow: 'hidden' }}>
              <div style={{
                height: '100%',
                backgroundColor: '#00e676',
                transition: 'width 0.1s',
                width: `${Math.min(100, (Math.abs(mod.actualVelocityMps) / 3.0) * 100)}%`
              }} />
            </div>
            
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem' }}>
              <span style={{ color: 'rgba(255,255,255,0.4)' }}>Power</span>
              <span style={{ fontFamily: 'monospace', color: 'rgba(255,255,255,0.8)' }}>
                {mod.drivePower.toFixed(2)}
              </span>
            </div>
            <div style={{ width: '100%', height: '6px', backgroundColor: 'rgba(255,255,255,0.1)', borderRadius: '9999px', overflow: 'hidden' }}>
              <div style={{
                height: '100%',
                transition: 'width 0.1s',
                backgroundColor: mod.drivePower < 0 ? '#ff5252' : '#448aff',
                width: `${Math.abs(mod.drivePower) * 100}%`,
                marginLeft: mod.drivePower < 0 ? 'auto' : '0'
              }} />
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}
