import React, { useState, useEffect, useRef } from 'react';
import { Download, Terminal, Table as TableIcon, Trash2, Play, Square } from 'lucide-react';
import type { TelemetryEntry } from '../types/telemetry';

interface TelemetryPanelProps {
    data: TelemetryEntry[];
    isRecording: boolean;
    onStartRecording: () => void;
    onStopRecording: () => void;
    onClear: () => void;
    onExport: () => void;
}

const TelemetryPanel: React.FC<TelemetryPanelProps> = ({ 
    data, 
    isRecording, 
    onStartRecording, 
    onStopRecording, 
    onClear, 
    onExport 
}) => {
    const [viewMode, setViewMode] = useState<'table' | 'terminal'>('table');
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
                <div className="flex items-center gap-4">
                    <h2 className="text-xs font-bold text-slate-200 uppercase tracking-widest flex items-center gap-2 font-mono">
                        <Terminal className="w-4 h-4 text-blue-400" />
                        Inspection_Engine
                    </h2>
                    <div className="flex bg-slate-900 rounded-lg p-1 border border-slate-700">
                        <button onClick={() => setViewMode('table')} className={`p-1.5 rounded ${viewMode === 'table' ? 'bg-blue-600 text-white' : 'text-slate-400'}`}>
                            <TableIcon className="w-4 h-4" />
                        </button>
                        <button onClick={() => setViewMode('terminal')} className={`p-1.5 rounded ${viewMode === 'terminal' ? 'bg-blue-600 text-white' : 'text-slate-400'}`}>
                            <Terminal className="w-4 h-4" />
                        </button>
                    </div>
                </div>
                <div className="flex items-center gap-2">
                    {isRecording ? (
                        <button onClick={onStopRecording} className="flex items-center gap-2 px-3 py-1.5 bg-red-600/20 text-red-400 border border-red-600/50 rounded-lg hover:bg-red-600/30 transition-all font-bold text-[10px]">
                            <Square className="w-3 h-3 fill-current" /> STOP
                        </button>
                    ) : (
                        <button onClick={onStartRecording} className="flex items-center gap-2 px-3 py-1.5 bg-green-600/20 text-green-400 border border-green-600/50 rounded-lg hover:bg-green-600/30 transition-all font-bold text-[10px]">
                            <Play className="w-3 h-3 fill-current" /> RECORD
                        </button>
                    )}
                    <button onClick={onExport} disabled={data.length === 0} className="p-1.5 bg-blue-600/20 text-blue-400 border border-blue-600/50 rounded-lg hover:bg-blue-600/30 transition-all disabled:opacity-50">
                        <Download className="w-4 h-4" />
                    </button>
                    <button onClick={onClear} className="p-1.5 bg-slate-700 text-slate-400 border border-slate-600 rounded-lg hover:bg-slate-600 hover:text-white transition-all">
                        <Trash2 className="w-4 h-4" />
                    </button>
                </div>
            </div>

            <div className="flex-1 overflow-hidden relative">
                {viewMode === 'table' ? (
                    <div className="h-full overflow-y-auto p-4 custom-scrollbar">
                        <div className="grid grid-cols-1 gap-4">
                            <TelemetryCard title="Pinpoint Odometry" color="blue">
                                <Row label="Field X" value={lastEntry?.x} unit="m" />
                                <Row label="Field Y" value={lastEntry?.y} unit="m" />
                                <Row label="Heading" value={(lastEntry?.heading || 0) * 180 / Math.PI} unit="°" />
                            </TelemetryCard>

                            <TelemetryCard title="Swerve Logic Status" color="emerald">
                                <div className="flex justify-between items-center text-[10px] font-mono mb-1">
                                    <span className="text-slate-500 uppercase">Snap Active</span>
                                    <span className={lastEntry?.isSnapping ? "text-emerald-400 font-bold" : "text-slate-600"}>
                                        {lastEntry?.isSnapping ? "TRUE" : "FALSE"}
                                    </span>
                                </div>
                                <div className="flex justify-between items-center text-[10px] font-mono">
                                    <span className="text-slate-500 uppercase">Holding Angle</span>
                                    <span className={lastEntry?.isMaintaining ? "text-blue-400 font-bold" : "text-slate-600"}>
                                        {lastEntry?.isMaintaining ? "ACTIVE" : "IDLE"}
                                    </span>
                                </div>
                            </TelemetryCard>
                        </div>

                        <div className="mt-6 rounded-xl border border-slate-800 bg-slate-900/50 overflow-hidden">
                            <div className="px-4 py-2 bg-slate-800 text-[9px] font-mono font-bold text-slate-400 uppercase tracking-widest">Module States</div>
                            <table className="w-full text-[10px] text-left border-collapse">
                                <thead className="text-slate-500 font-mono border-b border-slate-800">
                                    <tr>
                                        <th className="px-4 py-2">MOD</th>
                                        <th className="px-4 py-2">SET_DEG</th>
                                        <th className="px-4 py-2">SET_MPS</th>
                                        <th className="px-4 py-2">ACT_DEG</th>
                                        <th className="px-4 py-2">ACT_MPS</th>
                                    </tr>
                                </thead>
                                <tbody className="font-mono text-slate-300 divide-y divide-slate-800/30">
                                    {['FL', 'FR', 'RR', 'RL'].map((m, i) => {
                                        const target = lastEntry?.targets?.[i] || [0,0];
                                        const actual = lastEntry?.actuals?.[i] || [0,0];
                                        return (
                                            <tr key={m} className="hover:bg-slate-800/30">
                                                <td className="px-4 py-2 font-bold text-blue-400">{m}</td>
                                                <td className="px-4 py-2">{(target[1] * 180 / Math.PI).toFixed(1)}°</td>
                                                <td className="px-4 py-2 text-emerald-400/80">{target[0].toFixed(2)}</td>
                                                <td className="px-4 py-2">{(actual[1] * 180 / Math.PI).toFixed(1)}°</td>
                                                <td className="px-4 py-2 text-amber-400/80">{actual[0].toFixed(2)}</td>
                                            </tr>
                                        );
                                    })}
                                </tbody>
                            </table>
                        </div>
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

                <div className="absolute bottom-0 left-0 right-0 py-1 px-4 bg-slate-900 border-t border-slate-800 flex justify-between text-[9px] font-mono text-slate-500 uppercase tracking-widest">
                    <span>Buffer: {data.length} records</span>
                    <span>{isRecording ? "RECORD_ACTIVE" : "LIVE_STREAM"}</span>
                </div>
            </div>
        </div>
    );
};

const TelemetryCard = ({ title, children }: any) => (
    <div className={`p-3 rounded-xl bg-slate-800/30 border border-slate-800 shadow-sm`}>
        <h3 className={`text-[9px] font-bold text-slate-500 uppercase tracking-widest mb-2 border-b border-slate-800 pb-1`}>
            {title}
        </h3>
        <div className="space-y-1">
            {children}
        </div>
    </div>
);

const Row = ({ label, value, unit }: { label: string, value?: number, unit: string }) => (
    <div className="flex justify-between items-center text-[11px] font-mono">
        <span className="text-slate-500 uppercase text-[9px]">{label}:</span>
        <span className="text-slate-200">
            {value !== undefined ? value.toFixed(3) : "---"}
            <span className="text-[9px] text-slate-600 ml-1">{unit}</span>
        </span>
    </div>
);

export default TelemetryPanel;
