import { useState, useCallback, useRef } from 'react';
import MosaicShell from './components/MosaicShell';
import type { TelemetryEntry } from './types/telemetry';

function App() {
  const showModuleVectors = true;

  // Temporal Engine State
  const historyRef = useRef<TelemetryEntry[]>([]);
  const [scrubIndex, setScrubIndex] = useState(-1);
  const [isLive, setIsLive] = useState(true);
  const [isRecording, setIsRecording] = useState(false);

  // Frame to Display
  const currentFrame = isLive || scrubIndex === -1 
    ? (historyRef.current[historyRef.current.length - 1] || null)
    : historyRef.current[scrubIndex];

  const handleTelemetryUpdate = useCallback((entry: TelemetryEntry) => {
    historyRef.current.push(entry);
    
    // Limit history to 30,000 points (~10mins at 50Hz) for custom binary seek
    if (historyRef.current.length > 30000) {
      historyRef.current.shift();
    }

    if (isLive) {
      setScrubIndex(historyRef.current.length - 1);
    }
  }, [isLive]);

  const handleExportCSV = () => {
    const data = historyRef.current;
    if (data.length === 0) return;

    const headers = ["timestamp", "x", "y", "heading", "mode"];
    const csvRows = [
      headers.join(','),
      ...data.map(row => headers.map(header => row[header]).join(','))
    ];

    const csvContent = csvRows.join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.setAttribute('href', url);
    link.setAttribute('download', `swerve_scope_${new Date().toISOString()}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div className="flex flex-col w-full h-screen bg-slate-950 text-slate-100 overflow-hidden font-sans">
      {/* Top Header */}
      <header className="h-10 border-b border-slate-800 flex items-center justify-between px-6 bg-slate-900/40 backdrop-blur-md shrink-0">
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-2">
            <h1 className="text-xs font-black tracking-tighter text-blue-500 uppercase">SwerveScope</h1>
            <div className="h-4 w-[1px] bg-slate-800 mx-2" />
            <div className={`w-2 h-2 rounded-full ${isLive ? 'bg-emerald-500 animate-pulse' : 'bg-amber-500 shadow-[0_0_8px_currentColor]'} `} />
            <span className={`text-[10px] font-mono font-bold tracking-wider ${isLive ? 'text-emerald-400' : 'text-amber-400'}`}>
              {isLive ? 'LIVE_FEED' : 'HISTORY_SEEK'}
            </span>
          </div>
        </div>
        <div className="flex items-center gap-6">
          <button 
            onClick={() => setIsLive(true)}
            className={`px-3 py-1 text-[9px] font-mono font-bold border rounded transition-all ${isLive ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/40' : 'bg-slate-800 text-slate-400 border-slate-700 hover:border-slate-500'}`}
          >
            SNAP_LIVE
          </button>
        </div>
      </header>

      {/* Tiled Dashboard */}
      <MosaicShell 
        showModuleVectors={showModuleVectors}
        onTelemetryUpdate={handleTelemetryUpdate}
        currentFrame={currentFrame}
        history={historyRef.current}
        isLive={isLive}
        isRecording={isRecording}
        onStartRecording={() => setIsRecording(true)}
        onStopRecording={() => setIsRecording(false)}
        onClear={() => { historyRef.current = []; setScrubIndex(-1); }}
        onExport={handleExportCSV}
      />

      {/* Global Scrubber Footer */}
      <footer className="h-12 border-t border-slate-800 bg-slate-900 px-6 flex items-center gap-4 shrink-0">
         <span className="text-[10px] font-mono text-slate-500 w-12 text-right">0:00</span>
         <div className="flex-1 relative group h-4 flex items-center">
           <input 
             type="range"
             min="0"
             max={Math.max(0, historyRef.current.length - 1)}
             value={scrubIndex === -1 ? historyRef.current.length - 1 : scrubIndex}
             onChange={(e) => {
               setIsLive(false);
               setScrubIndex(parseInt(e.target.value));
             }}
             className="w-full h-1 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-blue-500 group-hover:h-1.5 transition-all"
           />
         </div>
         <span className="text-[10px] font-mono text-slate-500 w-12">
           {((historyRef.current.length * 0.02)).toFixed(1)}s
         </span>
      </footer>
    </div>
  );
}

export default App;
