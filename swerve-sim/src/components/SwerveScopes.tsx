import React, { useEffect, useRef, useState, useMemo } from 'react';
import uPlot from 'uplot';
import 'uplot/dist/uPlot.min.css';
import { Settings, Info, Plus, ChevronRight, X } from 'lucide-react';
import type { TelemetryEntry } from '../types/telemetry';
import { TELEMETRY_FIELDS, resolveTelemetryField } from '../types/telemetry';

interface SwerveScopesProps {
    history: TelemetryEntry[];
    isLive: boolean;
}

const SERIES_COLORS = [
    '#38bdf8', // sky-400
    '#f43f5e', // rose-500
    '#fbbf24', // amber-400
    '#10b981', // emerald-500
    '#a78bfa', // violet-400
    '#f97316', // orange-500
    '#2dd4bf', // teal-400
    '#e879f9', // fuchsia-400
];

const SwerveScopes: React.FC<SwerveScopesProps> = ({ history, isLive }) => {
    const chartRef = useRef<HTMLDivElement>(null);
    const plotRef = useRef<uPlot | null>(null);
    
    const [activeFields, setActiveFields] = useState<string[]>(['targets.0.0', 'actuals.0.0']);
    const [isSidebarOpen, setIsSidebarOpen] = useState(true);

    // Filter available fields to show in the selector
    const allFieldsList = useMemo(() => [
        ...TELEMETRY_FIELDS.pose,
        ...TELEMETRY_FIELDS.modules,
        ...TELEMETRY_FIELDS.gamepad,
        ...TELEMETRY_FIELDS.observer,
    ], []);

    const toggleField = (key: string) => {
        setActiveFields(prev => {
            if (prev.includes(key)) {
                return prev.filter(k => k !== key);
            }
            if (prev.length >= 8) return prev; // Limit to 8 colors/series
            return [...prev, key];
        });
    };

    useEffect(() => {
        if (!chartRef.current) return;

        const series = [
            {}, // Time Axis
            ...activeFields.map((key, i) => {
                const field = allFieldsList.find(f => f.key === key);
                return {
                    label: field?.label || key,
                    stroke: SERIES_COLORS[i % SERIES_COLORS.length],
                    width: 2,
                    points: { show: false },
                    spanGaps: true,
                };
            })
        ];

        const opts: uPlot.Options = {
            width: chartRef.current.clientWidth,
            height: chartRef.current.clientHeight,
            cursor: {
                show: true,
                points: {
                    show: true,
                },
            },
            series: series as any,
            axes: [
                {
                    stroke: "#475569",
                    grid: { stroke: "#1e293b", width: 1 },
                },
                {
                    stroke: "#475569",
                    grid: { stroke: "#1e293b", width: 1 },
                }
            ],
            scales: {
                x: { time: false },
            },
        };

        // Initial empty data
        const data: uPlot.AlignedData = [
            [], // Time
            ...activeFields.map(() => []),
        ];

        if (plotRef.current) {
            plotRef.current.destroy();
        }
        
        plotRef.current = new uPlot(opts, data, chartRef.current);

        return () => {
            plotRef.current?.destroy();
            plotRef.current = null;
        };
    }, [activeFields, allFieldsList]);

    // Handle resize
    useEffect(() => {
        const resizeObserver = new ResizeObserver(() => {
            if (plotRef.current && chartRef.current) {
                plotRef.current.setSize({
                    width: chartRef.current.clientWidth,
                    height: chartRef.current.clientHeight,
                });
            }
        });

        if (chartRef.current) {
            resizeObserver.observe(chartRef.current);
        }

        return () => resizeObserver.disconnect();
    }, []);

    // Update Plot Data
    useEffect(() => {
        if (!plotRef.current) return;

        // Take a window of data for the scope
        const windowSize = 500;
        const recent = history.slice(-windowSize);
        
        if (recent.length === 0) return;

        const timestampField = recent.map(h => h.timestamp);
        
        const seriesData = activeFields.map(key => 
            recent.map(h => resolveTelemetryField(h, key))
        );

        plotRef.current.setData([timestampField, ...seriesData]);
    }, [history, activeFields]);

    return (
        <div className="w-full h-full flex bg-slate-950 text-slate-300 font-mono overflow-hidden">
            {/* Field Selector Sidebar */}
            <div className={`flex flex-col border-r border-slate-800 transition-all duration-300 ${isSidebarOpen ? 'w-48' : 'w-10'}`}>
                <div className="flex items-center justify-between p-2 border-b border-slate-800 shrink-0">
                    {isSidebarOpen && <span className="text-[10px] font-bold uppercase tracking-widest text-slate-500">Fields</span>}
                    <button 
                        onClick={() => setIsSidebarOpen(!isSidebarOpen)}
                        className="p-1 hover:bg-slate-800 rounded text-slate-500"
                    >
                        <ChevronRight className={`w-3.5 h-3.5 transition-transform ${isSidebarOpen ? 'rotate-180' : ''}`} />
                    </button>
                </div>

                <div className="flex-1 overflow-y-auto custom-scrollbar">
                    {isSidebarOpen ? (
                        Object.entries(TELEMETRY_FIELDS).map(([group, fields]) => (
                            <div key={group} className="mb-2">
                                <div className="px-3 py-1 text-[8px] text-slate-600 uppercase tracking-widest bg-slate-900/50">{group}</div>
                                {fields.map((f: any) => (
                                    <button
                                        key={f.key}
                                        onClick={() => toggleField(f.key)}
                                        className={`w-full text-left px-3 py-1.5 text-[9px] hover:bg-slate-800 transition-colors flex items-center justify-between group ${
                                            activeFields.includes(f.key) ? 'text-sky-400 bg-sky-500/5' : 'text-slate-500'
                                        }`}
                                    >
                                        <span className="truncate mr-2">{f.label}</span>
                                        {activeFields.includes(f.key) ? (
                                            <X className="w-2.5 h-2.5 shrink-0" />
                                        ) : (
                                            <Plus className="w-2.5 h-2.5 opacity-0 group-hover:opacity-100 shrink-0" />
                                        )}
                                    </button>
                                ))}
                            </div>
                        ))
                    ) : (
                        <div className="flex flex-col items-center py-4 gap-4">
                            <Plus className="w-4 h-4 text-slate-700" />
                        </div>
                    )}
                </div>
            </div>

            {/* Main Graph Area */}
            <div className="flex-1 flex flex-col overflow-hidden">
                <header className="flex justify-between items-center px-4 py-2 bg-slate-900/40 border-b border-slate-800 shrink-0">
                    <div className="flex items-center gap-4">
                        <span className="text-[9px] font-bold uppercase tracking-[0.2em] text-slate-400">Diagnostic Scopes</span>
                        <div className="flex gap-2">
                            {activeFields.map((key, i) => {
                                const field = allFieldsList.find(f => f.key === key);
                                return (
                                    <div key={key} className="flex items-center gap-1.5 px-2 py-0.5 rounded bg-slate-800/50 border border-slate-700/50">
                                        <div className="w-1.5 h-1.5 rounded-full" style={{ backgroundColor: SERIES_COLORS[i % SERIES_COLORS.length] }} />
                                        <span className="text-[8px]">{field?.label}</span>
                                        <button onClick={() => toggleField(key)} className="hover:text-rose-400">
                                            <X className="w-2 h-2" />
                                        </button>
                                    </div>
                                );
                            })}
                        </div>
                    </div>
                </header>

                <div className="flex-1 p-4 relative min-h-0">
                    <div ref={chartRef} className="w-full h-full" />
                </div>

                {/* Footer / Stats mini view */}
                <div className="px-4 py-1.5 bg-slate-950 border-t border-slate-800 flex justify-between items-center shrink-0">
                    <div className="flex gap-4 text-[8px] text-slate-600">
                        <div className="flex items-center gap-1">
                            <div className="w-1 h-1 rounded-full bg-emerald-500" />
                            <span>BUFFER OK</span>
                        </div>
                        <div className="flex items-center gap-1">
                            <div className="w-1 h-1 rounded-full bg-blue-500" />
                            <span>50 HZ</span>
                        </div>
                    </div>
                    <span className="text-[8px] text-slate-700">{history.length} samples recorded</span>
                </div>
            </div>
        </div>
    );
};

export default SwerveScopes;
