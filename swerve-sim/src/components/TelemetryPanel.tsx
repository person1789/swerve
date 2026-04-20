import React, { useState, useEffect, useRef } from 'react';
import { Download, Terminal, Table as TableIcon, Trash2, Play, Square } from 'lucide-react';

interface TelemetryEntry {
    timestamp: number;
    posX: number;
    posY: number;
    heading: number;
    velX: number;
    velY: number;
    velH: number;
    accelX: number;
    accelY: number;
    accelH: number;
    jerkX: number;
    jerkY: number;
    jerkH: number;
    [key: string]: number; // Support for dynamic module data
}

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

    // Auto-scroll terminal
    useEffect(() => {
        if (viewMode === 'terminal' && terminalRef.current) {
            terminalRef.current.scrollTop = terminalRef.current.scrollHeight;
        }
    }, [data.length, viewMode]);

    return (
        <div className="h-full flex flex-col bg-slate-900 border-l border-slate-800 shadow-2xl overflow-hidden">
            {/* Header / Controls */}
            <div className="p-4 bg-slate-800/50 border-b border-slate-700 flex items-center justify-between">
                <div className="flex items-center gap-4">
                    <h2 className="text-sm font-bold text-slate-200 uppercase tracking-widest flex items-center gap-2">
                        <Terminal className="w-4 h-4 text-blue-400" />
                        System Telemetry
                    </h2>
                    <div className="flex bg-slate-900 rounded-lg p-1 border border-slate-700">
                        <button 
                            onClick={() => setViewMode('table')}
                            className={`p-1.5 rounded ${viewMode === 'table' ? 'bg-blue-600 text-white' : 'text-slate-400 hover:text-slate-200'}`}
                        >
                            <TableIcon className="w-4 h-4" />
                        </button>
                        <button 
                            onClick={() => setViewMode('terminal')}
                            className={`p-1.5 rounded ${viewMode === 'terminal' ? 'bg-blue-600 text-white' : 'text-slate-400 hover:text-slate-200'}`}
                        >
                            <Terminal className="w-4 h-4" />
                        </button>
                    </div>
                </div>

                <div className="flex items-center gap-2">
                    {isRecording ? (
                        <button 
                            onClick={onStopRecording}
                            className="flex items-center gap-2 px-3 py-1.5 bg-red-600/20 text-red-400 border border-red-600/50 rounded-lg hover:bg-red-600/30 transition-all font-bold text-xs"
                        >
                            <Square className="w-3 h-3 fill-current" /> STOP
                        </button>
                    ) : (
                        <button 
                            onClick={onStartRecording}
                            className="flex items-center gap-2 px-3 py-1.5 bg-green-600/20 text-green-400 border border-green-600/50 rounded-lg hover:bg-green-600/30 transition-all font-bold text-xs"
                        >
                            <Play className="w-3 h-3 fill-current" /> RECORD
                        </button>
                    )}
                    <button 
                        onClick={onExport}
                        disabled={data.length === 0}
                        className="p-1.5 bg-blue-600/20 text-blue-400 border border-blue-600/50 rounded-lg hover:bg-blue-600/30 transition-all disabled:opacity-50 disabled:grayscale"
                        title="Export CSV"
                    >
                        <Download className="w-4 h-4" />
                    </button>
                    <button 
                        onClick={onClear}
                        className="p-1.5 bg-slate-700 text-slate-400 border border-slate-600 rounded-lg hover:bg-slate-600 hover:text-white transition-all"
                        title="Clear History"
                    >
                        <Trash2 className="w-4 h-4" />
                    </button>
                </div>
            </div>

            {/* Content Area */}
            <div className="flex-1 overflow-hidden relative">
                {viewMode === 'table' ? (
                    <div className="h-full overflow-y-auto p-4 custom-scrollbar">
                        <div className="grid grid-cols-2 gap-4">
                            {/* Pose Section */}
                            <TelemetryCard title="Pinpoint Odometry" color="blue">
                                <Row label="Field X" value={lastEntry?.posX} unit="m" />
                                <Row label="Field Y" value={lastEntry?.posY} unit="m" />
                                <Row label="Heading" value={(lastEntry?.heading || 0) * 180 / Math.PI} unit="°" />
                            </TelemetryCard>

                            {/* Motion State (Target) */}
                            <TelemetryCard title="Target Velocity" color="green">
                                <Row label="Vx" value={lastEntry?.velX} unit="m/s" />
                                <Row label="Vy" value={lastEntry?.velY} unit="m/s" />
                                <Row label="Vω" value={lastEntry?.velH} unit="rad/s" />
                            </TelemetryCard>

                            {/* Observer Feedback (Actual) */}
                            <TelemetryCard title="Observer Feedback" color="blue">
                                <Row label="Obs Vx" value={lastEntry?.obsX} unit="m/s" />
                                <Row label="Obs Vy" value={lastEntry?.obsY} unit="m/s" />
                                <Row label="Obs Vω" value={lastEntry?.obsH} unit="rad/s" />
                            </TelemetryCard>

                            {/* Acceleration */}
                            <TelemetryCard title="Acceleration Profile" color="purple">
                                <Row label="Ax" value={lastEntry?.accelX} unit="m/s²" />
                                <Row label="Ay" value={lastEntry?.accelY} unit="m/s²" />
                                <Row label="Aω" value={lastEntry?.accelH} unit="rad/s²" />
                            </TelemetryCard>

                            {/* Jerk */}
                            <TelemetryCard title="Jerk Profile" color="orange">
                                <Row label="Jx" value={lastEntry?.jerkX} unit="m/s³" />
                                <Row label="Jy" value={lastEntry?.jerkY} unit="m/s³" />
                                <Row label="Jω" value={lastEntry?.jerkH} unit="rad/s³" />
                            </TelemetryCard>
                        </div>

                        {/* Module Detail Table */}
                        <div className="mt-6 rounded-xl border border-slate-800 bg-slate-900/50 overflow-hidden">
                            <table className="w-full text-xs text-left">
                                <thead className="bg-slate-800 text-slate-400 font-mono">
                                    <tr>
                                        <th className="px-4 py-2">Module</th>
                                        <th className="px-4 py-2">Angle (°)</th>
                                        <th className="px-4 py-2">Speed (m/s)</th>
                                        <th className="px-4 py-2">Dist (m)</th>
                                    </tr>
                                </thead>
                                <tbody className="font-mono text-slate-300 divide-y divide-slate-800">
                                    {['FL', 'FR', 'RR', 'RL'].map((m) => (
                                        <tr key={m} className="hover:bg-slate-800/30">
                                            <td className="px-4 py-2 font-bold text-blue-400">{m}</td>
                                            <td className="px-4 py-2">{(lastEntry?.[`${m}_angle`] || 0).toFixed(1)}°</td>
                                            <td className="px-4 py-2">{(lastEntry?.[`${m}_vel`] || 0).toFixed(2)}</td>
                                            <td className="px-4 py-2">{(lastEntry?.[`${m}_dist`] || 0).toFixed(3)}</td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    </div>
                ) : (
                    <div 
                        ref={terminalRef}
                        className="h-full bg-black/50 p-4 font-mono text-[10px] leading-tight text-green-500 overflow-y-auto custom-scrollbar"
                    >
                        {data.slice(-200).map((entry, i) => (
                            <div key={i} className="mb-0.5 opacity-80 hover:opacity-100 whitespace-nowrap">
                                <span className="text-slate-500">[{entry.timestamp.toFixed(3)}]</span> 
                                <span className="text-blue-400 mx-2">POSE:</span>
                                ({entry.posX.toFixed(2)}, {entry.posY.toFixed(2)}, {(entry.heading * 180 / Math.PI).toFixed(1)}°)
                                <span className="text-green-400 mx-2">VEL:</span>
                                ({entry.velX.toFixed(2)}, {entry.velY.toFixed(2)})
                                <span className="text-purple-400 mx-2">ACCEL:</span>
                                {entry.accelX.toFixed(2)}
                            </div>
                        ))}
                        {data.length === 0 && <div className="text-slate-600 animate-pulse">Waiting for loop start...</div>}
                    </div>
                )}

                {/* Status Bar */}
                <div className="absolute bottom-0 left-0 right-0 py-1 px-4 bg-slate-900 border-t border-slate-800 flex justify-between text-[10px] font-mono text-slate-500 uppercase tracking-widest">
                    <span>Buffer: {data.length} records</span>
                    <span>Status: {isRecording ? "RECORDING" : "LIVE FEED"}</span>
                </div>
            </div>
        </div>
    );
};

const TelemetryCard = ({ title, children }: any) => (
    <div className={`p-3 rounded-xl bg-slate-800/30 border border-slate-800 shadow-sm`}>
        <h3 className={`text-[10px] font-bold text-slate-500 uppercase tracking-widest mb-2 border-b border-slate-700/50 pb-1`}>
            {title}
        </h3>
        <div className="space-y-1">
            {children}
        </div>
    </div>
);

const Row = ({ label, value, unit }: { label: string, value?: number, unit: string }) => (
    <div className="flex justify-between items-center text-xs font-mono">
        <span className="text-slate-400">{label}:</span>
        <span className="text-slate-100">
            {value !== undefined ? value.toFixed(3) : "---"}
            <span className="text-[10px] text-slate-500 ml-1">{unit}</span>
        </span>
    </div>
);

export default TelemetryPanel;
