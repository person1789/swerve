import React, { useEffect, useRef } from 'react';
import uPlot from 'uplot';
import 'uplot/dist/uPlot.min.css';
import type { TelemetryEntry } from '../types/telemetry';

interface SwerveScopesProps {
    history: TelemetryEntry[];
    isLive: boolean;
}

const SwerveScopes: React.FC<SwerveScopesProps> = ({ history, isLive }) => {
    const chartRef = useRef<HTMLDivElement>(null);
    const plotRef = useRef<uPlot | null>(null);

    useEffect(() => {
        if (!chartRef.current) return;

        // AdvantageScope Design Palettes
        const opts: uPlot.Options = {
            width: chartRef.current.clientWidth,
            height: chartRef.current.clientHeight,
            title: "",
            cursor: {
                show: true,
            },
            series: [
                {}, // Time Axis
                {
                    label: "Target X",
                    stroke: "rgba(244, 63, 94, 1)", // Red-point
                    width: 2,
                },
                {
                    label: "Actual X",
                    stroke: "rgba(16, 185, 129, 1)", // Emerald
                    width: 2,
                }
            ],
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

        const data: uPlot.AlignedData = [
            [], // Time
            [], // Target X
            [], // Actual X
        ];

        plotRef.current = new uPlot(opts, data, chartRef.current);

        return () => {
            plotRef.current?.destroy();
        };
    }, []);

    // Update Plot Data
    useEffect(() => {
        if (!plotRef.current) return;

        // Take the last 500 points for the scope view
        const windowSize = 500;
        const recent = history.slice(-windowSize);
        
        const times = recent.map((_, i) => i);
        
        // Multi-pane logic: Here we just plot the front-left module as a reference
        const targets = recent.map(h => (h.targets ? h.targets[0][0] : 0)); 
        const actuals = recent.map(h => (h.actuals ? h.actuals[0][0] : 0));

        plotRef.current.setData([times, targets, actuals]);
    }, [history, isLive]);

    return (
        <div className="w-full h-full flex gap-4">
            <div className="flex-1 flex flex-col gap-2">
                <header className="flex justify-between items-center px-1">
                    <span className="text-[8px] font-mono font-bold text-slate-500 uppercase tracking-widest">Velocity Scope (FL) // M/S</span>
                </header>
                <div ref={chartRef} className="flex-1 w-full bg-slate-950/50 rounded-lg overflow-hidden border border-slate-800/50" />
            </div>

            <div className="flex-1 flex flex-col gap-2">
                <header className="flex justify-between items-center px-1">
                    <span className="text-[8px] font-mono font-bold text-slate-500 uppercase tracking-widest">Logic Flow // Telemetry Stream</span>
                </header>
                <div className="flex-1 bg-slate-950/30 rounded-lg border border-slate-800/50 p-3 flex flex-col gap-1 overflow-hidden font-mono text-[9px] text-slate-500">
                    <div className="flex justify-between py-1 border-b border-slate-800/30">
                        <span>Buffer Health</span>
                        <span className="text-emerald-500">OPTIMAL</span>
                    </div>
                    <div className="flex justify-between py-1 border-b border-slate-800/30">
                        <span>Frequency</span>
                        <span className="text-slate-300">50Hz</span>
                    </div>
                    <div className="flex justify-between py-1 border-b border-slate-800/30">
                        <span>Swerve Mode</span>
                        <span className="text-blue-400 font-bold uppercase tracking-tighter">Field-Centric</span>
                    </div>
                    <div className="mt-auto opacity-30 text-[8px] leading-tight">
                        Telemetry synchronized with frame buffer indices 0 through {history.length - 1}.
                    </div>
                </div>
            </div>
        </div>
    );
};

export default SwerveScopes;
