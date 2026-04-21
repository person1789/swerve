import { useState, useCallback, useRef, useEffect } from 'react';
import MosaicShell from './components/MosaicShell';
import { db } from './lib/db';
import type { TelemetryEntry } from './types/telemetry';
import { Play, Pause, FastForward, Save, Trash2, RotateCcw } from 'lucide-react';

function App() {
  const showModuleVectors = true;

  // Temporal Engine State
  const historyRef = useRef<TelemetryEntry[]>([]);
  const [scrubIndex, setScrubIndex] = useState(-1);
  const [isLive, setIsLive] = useState(true);
  const [isRecording, setIsRecording] = useState(false);
  
  // Playback Controls (Phase 4)
  const [isPlaying, setIsPlaying] = useState(false);
  const [playbackSpeed, setPlaybackSpeed] = useState(1);
  const playbackRef = useRef<number | null>(null);

  // Frame to Display
  const currentFrame = isLive || scrubIndex === -1 
    ? (historyRef.current[historyRef.current.length - 1] || null)
    : historyRef.current[scrubIndex];

  const handleScrub = useCallback((index: number) => {
    setIsLive(false);
    setIsPlaying(false);
    setScrubIndex(Math.min(Math.max(0, index), historyRef.current.length - 1));
  }, []);

  const handleTelemetryUpdate = useCallback((entry: TelemetryEntry) => {
    historyRef.current.push(entry);
    if (historyRef.current.length > 35000) historyRef.current.shift();

    if (isLive) {
      setScrubIndex(historyRef.current.length - 1);
    }
  }, [isLive]);

  // Playback Engine Effect
  useEffect(() => {
    if (isPlaying && !isLive) {
      const interval = 20 / playbackSpeed; // 50Hz base
      playbackRef.current = window.setInterval(() => {
        setScrubIndex(prev => {
          if (prev >= historyRef.current.length - 1) {
            setIsPlaying(false);
            return prev;
          }
          return prev + 1;
        });
      }, interval);
    } else {
      if (playbackRef.current) clearInterval(playbackRef.current);
    }
    return () => { if (playbackRef.current) clearInterval(playbackRef.current); };
  }, [isPlaying, isLive, playbackSpeed]);

  const saveCurrentSession = async () => {
    if (historyRef.current.length === 0) return;
    const name = `Session ${new Date().toLocaleTimeString()}`;
    await db.sessions.add({
      name,
      description: "Manual Capture",
      date: Date.now(),
      duration: historyRef.current.length * 0.02,
      frames: [...historyRef.current]
    });
    alert("Session saved to disk!");
  };

  const handleExportCSV = () => {
    const data = historyRef.current;
    if (data.length === 0) return;
    const headers = ["timestamp", "x", "y", "heading", "batteryVoltage", "loopTimeMs"];
    const csvContent = [
      headers.join(','),
      ...data.map(row => headers.map(h => (row as any)[h] ?? 0).join(','))
    ].join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = `swerve_scope_${Date.now()}.csv`;
    a.click();
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
            {currentFrame?.batteryVoltage !== undefined && (
              <div className="flex items-center gap-2 ml-4 px-2 py-0.5 rounded bg-slate-950/50 border border-slate-800">
                <div className={`w-1.5 h-1.5 rounded-full ${currentFrame.batteryVoltage < 10.5 ? 'bg-red-500 animate-ping' : currentFrame.batteryVoltage < 11.5 ? 'bg-amber-500' : 'bg-emerald-500'}`} />
                <span className="text-[10px] font-mono font-bold text-slate-300">
                  {currentFrame.batteryVoltage.toFixed(2)}V
                </span>
              </div>
            )}
          </div>
        </div>
        <div className="flex items-center gap-3">
          <button 
            onClick={saveCurrentSession}
            className="flex items-center gap-2 px-3 py-1 bg-blue-600/10 text-blue-400 border border-blue-600/30 rounded text-[9px] font-mono font-bold hover:bg-blue-600/20 transition-all"
          >
            <Save className="w-3 h-3" /> SAVE_SESSION
          </button>
          <button 
            onClick={() => setIsLive(true)}
            className={`px-3 py-1 text-[9px] font-mono font-bold border rounded transition-all ${isLive ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/40' : 'bg-slate-800 text-slate-400 border-slate-700 hover:border-slate-500 shadow-lg shadow-emerald-500/10'}`}
          >
            SNAP_LIVE
          </button>
        </div>
      </header>

      {/* Tiled Dashboard */}
      <MosaicShell 
        showModuleVectors={showModuleVectors}
        onTelemetryUpdate={handleTelemetryUpdate}
        onScrub={handleScrub}
        currentFrame={currentFrame}
        history={historyRef.current}
        isLive={isLive}
        isRecording={isRecording}
        onStartRecording={() => setIsRecording(true)}
        onStopRecording={() => setIsRecording(false)}
        onClear={() => { historyRef.current = []; setScrubIndex(-1); }}
        onExport={handleExportCSV}
        onLoadSession={(session) => {
          historyRef.current = [...session.frames];
          setIsLive(false);
          setScrubIndex(0);
          alert(`Loaded session: ${session.name}`);
        }}
      />

      {/* Global Scrubber Footer */}
      <footer className="h-14 border-t border-slate-800 bg-slate-900/80 backdrop-blur-xl px-6 flex items-center gap-6 shrink-0">
         <div className="flex items-center gap-2">
            <button 
                onClick={() => { setIsLive(false); setIsPlaying(!isPlaying); }}
                className="p-2.5 rounded-full bg-blue-600/10 border border-blue-600/30 text-blue-400 hover:bg-blue-600/20 hover:scale-105 active:scale-95 transition-all"
            >
                {isPlaying ? <Pause className="w-4 h-4 fill-current" /> : <Play className="w-4 h-4 fill-current ml-0.5" />}
            </button>
            <div className="flex bg-slate-950 rounded-lg p-0.5 border border-slate-800">
                {[0.5, 1, 2, 4].map(s => (
                    <button 
                        key={s}
                        onClick={() => setPlaybackSpeed(s)}
                        className={`px-2 py-1 text-[9px] font-black rounded transition-all ${playbackSpeed === s ? 'bg-slate-800 text-blue-400' : 'text-slate-600 hover:text-slate-400'}`}
                    >
                        {s}x
                    </button>
                ))}
            </div>
         </div>

         <div className="flex-1 flex items-center gap-4 group">
            <span className="text-[10px] font-mono text-slate-500 tabular-nums w-10 text-right">
                {((scrubIndex === -1 ? historyRef.current.length - 1 : scrubIndex) * 0.02).toFixed(1)}s
            </span>
            <div className="flex-1 relative h-6 flex items-center">
                <input 
                    type="range" min="0" max={Math.max(0, historyRef.current.length - 1)}
                    value={scrubIndex === -1 ? historyRef.current.length - 1 : scrubIndex}
                    onChange={(e) => handleScrub(parseInt(e.target.value))}
                    className="w-full h-1.5 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-blue-500 hover:h-2 transition-all"
                />
            </div>
            <span className="text-[10px] font-mono text-slate-500 tabular-nums w-10">
                {(historyRef.current.length * 0.02).toFixed(1)}s
            </span>
         </div>
      </footer>
    </div>
  );
}

export default App;

