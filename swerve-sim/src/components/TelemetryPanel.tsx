import React, { useState, useEffect, useRef } from 'react';
import { Download, Terminal, Table as TableIcon, Trash2, Play, Square, History as HistoryIcon, Clock } from 'lucide-react';
import type { TelemetryEntry } from '../types/telemetry';

interface TelemetryPanelProps {
    data: TelemetryEntry[];
    history: TelemetryEntry[];
    onScrub: (index: number) => void;
    isRecording: boolean;
    onStartRecording: () => void;
    onStopRecording: () => void;
    onClear: () => void;
    onExport: () => void;
}

const TelemetryPanel: React.FC<TelemetryPanelProps> = ({ 
    data, 
    history,
    onScrub,
    isRecording, 
    onStartRecording, 
    onStopRecording, 
    onClear, 
    onExport 
}) => {
    const [viewMode, setViewMode] = useState<'table' | 'history' | 'terminal'>('table');
    const terminalRef = useRef<HTMLDivElement>(null);
    const lastEntry = data.length > 0 ? data[data.length - 1] : null;

    useEffect(() => {
        if (viewMode === 'terminal' && terminalRef.current) {
            terminalRef.current.scrollTop = terminalRef.current.scrollHeight;
        }
    }, [data.length, viewMode]);

    return (
        <div className="h-full flex flex-col bg-slate-900 border-l border-slate-800 shadow-2xl overflow-hidden font-sans">
            <div className="p-4 bg-slate-800/50 border-b border-slate-700 flex items-center justify-between shrink-0">
                <div className="flex items-center gap-3">
                    <h2 className="text-[10px] font-bold text-slate-200 uppercase tracking-widest flex items-center gap-2 font-mono">
                        <Terminal className="w-3.5 h-3.5 text-blue-400" />
                        Inspection
                    </h2>
                    <div className="flex bg-slate-950/80 rounded-md p-0.5 border border-slate-700/50">
                        <button 
                            onClick={() => setViewMode('table')} 
                            className={`p-1 rounded transition-all ${viewMode === 'table' ? 'bg-blue-600 text-white' : 'text-slate-500 hover:text-slate-300'}`}
                            title="Live Inspector"
                        >
                            <TableIcon className="w-3.5 h-3.5" />
                        </button>
                        <button 
                            onClick={() => setViewMode('history')} 
                            className={`p-1 rounded transition-all ${viewMode === 'history' ? 'bg-blue-600 text-white' : 'text-slate-500 hover:text-slate-300'}`}
                            title="History Browser"
                        >
                            <HistoryIcon className="w-3.5 h-3.5" />
                        </button>
                        <button 
                            onClick={() => setViewMode('terminal')} 
                            className={`p-1 rounded transition-all ${viewMode === 'terminal' ? 'bg-blue-600 text-white' : 'text-slate-500 hover:text-slate-300'}`}
                            title="Raw Frames"
                        >
                            <Terminal className="w-3.5 h-3.5" />
                        </button>
                    </div>
                </div>
                <div className="flex items-center gap-1.5">
                    {isRecording ? (
                        <button onClick={onStopRecording} className="flex items-center gap-1.5 px-2 py-1 bg-red-600/20 text-red-400 border border-red-600/30 rounded hover:bg-red-600/30 transition-all font-bold text-[9px]">
                            <Square className="w-3 h-3 fill-current" /> STOP
                        </button>
                    ) : (
                        <button onClick={onStartRecording} className="flex items-center gap-1.5 px-2 py-1 bg-green-600/20 text-green-400 border border-green-600/30 rounded hover:bg-green-600/30 transition-all font-bold text-[9px]">
                            <Play className="w-3 h-3 fill-current" /> REC
                        </button>
                    )}
                    <button onClick={onExport} disabled={history.length === 0} className="p-1.5 bg-slate-800 text-slate-400 border border-slate-700 rounded hover:bg-slate-700 transition-all disabled:opacity-30">
                        <Download className="w-3.5 h-3.5" />
                    </button>
                    <button onClick={onClear} className="p-1.5 bg-slate-800 text-slate-500 border border-slate-700 rounded hover:bg-red-900/50 hover:text-red-400 hover:border-red-900/50 transition-all">
                        <Trash2 className="w-3.5 h-3.5" />
                    </button>
                </div>
            </div>

            <div className="flex-1 overflow-hidden relative">
                {viewMode === 'table' ? (
                    <div className="h-full overflow-y-auto p-4 custom-scrollbar space-y-4">
                        <div className="grid grid-cols-1 gap-4">
                            <TelemetryCard title="Pinpoint Odometry" color="blue">
                                <Row label="Field X" value={lastEntry?.x} unit="m" />
                                <Row label="Field Y" value={lastEntry?.y} unit="m" />
                                <Row label="Heading" value={(lastEntry?.heading || 0) * 180 / Math.PI} unit="°" />
                                <Row label="Loop Time" value={lastEntry?.loopTimeMs} unit="ms" />
                            </TelemetryCard>

                            <TelemetryCard title="Swerve Status" color="emerald">
                                <div className="flex justify-between items-center text-[10px] font-mono mb-1">
                                    <span className="text-slate-500 uppercase">Snap Active</span>
                                    <span className={lastEntry?.isSnapping ? "text-emerald-400 font-bold" : "text-slate-600"}>
                                        {lastEntry?.isSnapping ? "TRUE" : "FALSE"}
                                    </span>
                                </div>
                                <div className="flex justify-between items-center text-[10px] font-mono">
                                    <span className="text-slate-500 uppercase">Heading Hold</span>
                                    <span className={lastEntry?.isMaintaining ? "text-blue-400 font-bold" : "text-slate-600"}>
                                        {lastEntry?.isMaintaining ? "ACTIVE" : "IDLE"}
                                    </span>
                                </div>
                            </TelemetryCard>
                        </div>

                        <div className="rounded-xl border border-slate-800 bg-slate-950/30 overflow-hidden shadow-inner">
                            <div className="px-3 py-1.5 bg-slate-800/50 text-[8px] font-mono font-black text-slate-400 uppercase tracking-widest border-b border-slate-800">Module Detail</div>
                            <table className="w-full text-[10px] text-left border-collapse">
                                <thead className="text-[8px] text-slate-600 font-mono border-b border-slate-800 uppercase tracking-tighter">
                                    <tr>
                                        <th className="px-3 py-1.5">M</th>
                                        <th className="px-3 py-1.5">S_DEG</th>
                                        <th className="px-3 py-1.5 text-rose-500/50">T_MPS</th>
                                        <th className="px-3 py-1.5">A_DEG</th>
                                        <th className="px-3 py-1.5 text-emerald-500/50">A_MPS</th>
                                        <th className="px-3 py-1.5 text-blue-500/50 text-right">CURR</th>
                                    </tr>
                                </thead>
                                <tbody className="font-mono text-[10px] divide-y divide-slate-800/30">
                                    {['FL', 'FR', 'RR', 'RL'].map((m, i) => {
                                        const target = lastEntry?.targets?.[i] || [0,0];
                                        const actual = lastEntry?.actuals?.[i] || [0,0];
                                        const amps = lastEntry?.currentDraw?.[i] || 0;
                                        return (
                                            <tr key={m} className="hover:bg-slate-800/10">
                                                <td className="px-3 py-1.5 font-bold text-slate-400">{m}</td>
                                                <td className="px-3 py-1.5 text-slate-500">{(target[1] * 180 / Math.PI).toFixed(0)}°</td>
                                                <td className="px-3 py-1.5 text-rose-500/70">{target[0].toFixed(2)}</td>
                                                <td className="px-3 py-1.5 text-slate-300">{(actual[1] * 180 / Math.PI).toFixed(0)}°</td>
                                                <td className="px-3 py-1.5 text-emerald-500">{actual[0].toFixed(2)}</td>
                                                <td className={`px-3 py-1.5 text-right font-bold ${amps > 15 ? 'text-amber-500' : 'text-blue-400/80'}`}>
                                                    {amps.toFixed(1)}A
                                                </td>
                                            </tr>
                                        );
                                    })}
                                </tbody>
                            </table>
                        </div>
                    </div>
                ) : viewMode === 'history' ? (
                    <div className="h-full overflow-y-auto custom-scrollbar">
                        <table className="w-full text-[9px] font-mono text-left border-collapse sticky-header">
                            <thead className="bg-slate-950 text-slate-600 border-b border-slate-800 sticky top-0 z-10">
                                <tr>
                                    <th className="px-3 py-2 font-black uppercase tracking-tighter w-12">Entry</th>
                                    <th className="px-3 py-2 font-black uppercase tracking-tighter">Time</th>
                                    <th className="px-3 py-2 font-black uppercase tracking-tighter">Pose (X,Y,H)</th>
                                    <th className="px-3 py-2 font-black uppercase tracking-tighter text-right">Loop</th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-slate-800/20">
                                {history.slice().reverse().map((entry, revIdx) => {
                                    const actualIdx = history.length - 1 - revIdx;
                                    const isAlert = entry.loopTimeMs && entry.loopTimeMs > 30;
                                    return (
                                        <tr 
                                            key={actualIdx} 
                                            onClick={() => onScrub(actualIdx)}
                                            className="hover:bg-blue-500/10 cursor-pointer transition-colors group"
                                        >
                                            <td className="px-3 py-1.5 text-slate-700 font-bold">{actualIdx}</td>
                                            <td className="px-3 py-1.5 text-slate-400 tabular-nums">{entry.timestamp.toFixed(2)}s</td>
                                            <td className="px-3 py-1.5 text-slate-500 group-hover:text-slate-300">
                                                {entry.x.toFixed(2)}, {entry.y.toFixed(2)}, {(entry.heading * 180/Math.PI).toFixed(1)}°
                                            </td>
                                            <td className={`px-3 py-1.5 text-right tabular-nums ${isAlert ? 'text-amber-500 font-bold' : 'text-slate-600'}`}>
                                                {entry.loopTimeMs ? `${entry.loopTimeMs.toFixed(1)}ms` : '---'}
                                            </td>
                                        </tr>
                                    );
                                })}
                                {history.length === 0 && (
                                    <tr>
                                        <td colSpan={4} className="px-4 py-8 text-center text-slate-700 italic">No historical frames captured yet...</td>
                                    </tr>
                                )}
                            </tbody>
                        </table>
                    </div>
                ) : (
                    <div ref={terminalRef} className="h-full bg-slate-950 p-4 font-mono text-[9px] leading-tight text-slate-400 overflow-y-auto custom-scrollbar">
                        {data.slice(-100).map((entry, i) => (
                            <div key={i} className="mb-0.5 opacity-60 hover:opacity-100 whitespace-nowrap">
                                <span className="text-slate-600">[{entry.timestamp.toFixed(2)}]</span> 
                                <span className="text-blue-500/80 mx-2 uppercase">Pose:</span>
                                {entry.x.toFixed(2)}, {entry.y.toFixed(2)}, {(entry.heading * 180 / Math.PI).toFixed(1)}°
                                <span className="text-emerald-500/80 mx-2 uppercase">Module0:</span>
                                {entry.actuals?.[0]?.[0].toFixed(2)} m/s
                            </div>
                        ))}
                        {data.length === 0 && <div className="text-slate-700 animate-pulse">Waiting for SITL brain...</div>}
                    </div>
                )}

                <div className="absolute bottom-0 left-0 right-0 py-1 px-4 bg-slate-950 border-t border-slate-800 flex justify-between text-[8px] font-mono text-slate-600 uppercase tracking-widest pointer-events-none">
                    <span className="flex items-center gap-1">
                        <Clock className="w-2.5 h-2.5" />
                        {history.length} Records
                    </span>
                    <span>{isRecording ? "RECORDING" : "LIVE"}</span>
                </div>
            </div>
        </div>
    );
};

const TelemetryCard = ({ title, children }: any) => (
    <div className={`p-3 rounded-xl bg-slate-800/20 border border-slate-800/60 shadow-sm`}>
        <h3 className={`text-[8px] font-black text-slate-600 uppercase tracking-[0.2em] mb-2 pb-1 border-b border-slate-800`}>
            {title}
        </h3>
        <div className="space-y-1.5">
            {children}
        </div>
    </div>
);

const Row = ({ label, value, unit }: { label: string, value?: number, unit: string }) => (
    <div className="flex justify-between items-center text-[11px] font-mono">
        <span className="text-slate-500 uppercase text-[8px] tracking-tight">{label}:</span>
        <span className="text-slate-200">
            {value !== undefined ? value.toFixed(value < 10 ? 3 : 1) : "---"}
            <span className="text-[8px] text-slate-600 ml-1">{unit}</span>
        </span>
    </div>
);

export default TelemetryPanel;

