import React, { useState, useMemo } from 'react';
import { BarChart3, ChevronDown } from 'lucide-react';
import type { TelemetryEntry } from '../types/telemetry';
import { TELEMETRY_FIELDS, resolveTelemetryField } from '../types/telemetry';

interface StatisticsPanelProps {
    history: TelemetryEntry[];
    rangeStart?: number;
    rangeEnd?: number;
}

interface StatResult {
    label: string;
    mean: number;
    median: number;
    stdDev: number;
    min: number;
    max: number;
    rms: number;
    count: number;
}

function computeStats(values: number[], label: string): StatResult {
    if (values.length === 0) {
        return { label, mean: 0, median: 0, stdDev: 0, min: 0, max: 0, rms: 0, count: 0 };
    }

    const n = values.length;
    const sum = values.reduce((a, b) => a + b, 0);
    const mean = sum / n;

    const sorted = [...values].sort((a, b) => a - b);
    const median = n % 2 === 0
        ? (sorted[n / 2 - 1] + sorted[n / 2]) / 2
        : sorted[Math.floor(n / 2)];

    const variance = values.reduce((acc, v) => acc + (v - mean) ** 2, 0) / n;
    const stdDev = Math.sqrt(variance);

    const rms = Math.sqrt(values.reduce((acc, v) => acc + v * v, 0) / n);

    return {
        label,
        mean,
        median,
        stdDev,
        min: sorted[0],
        max: sorted[n - 1],
        rms,
        count: n,
    };
}

function computeHistogram(values: number[], bins: number = 20): { edges: number[]; counts: number[] } {
    if (values.length === 0) return { edges: [], counts: [] };

    const min = Math.min(...values);
    const max = Math.max(...values);
    const range = max - min || 1;
    const binWidth = range / bins;

    const edges: number[] = [];
    const counts: number[] = new Array(bins).fill(0);

    for (let i = 0; i <= bins; i++) {
        edges.push(min + i * binWidth);
    }

    for (const v of values) {
        const idx = Math.min(Math.floor((v - min) / binWidth), bins - 1);
        counts[idx]++;
    }

    return { edges, counts };
}

// All available fields flattened
const ALL_FIELDS = [
    ...TELEMETRY_FIELDS.pose,
    ...TELEMETRY_FIELDS.modules,
    ...TELEMETRY_FIELDS.gamepad,
    ...TELEMETRY_FIELDS.observer,
];

const StatisticsPanel: React.FC<StatisticsPanelProps> = ({ history, rangeStart, rangeEnd }) => {
    const [selectedField, setSelectedField] = useState(ALL_FIELDS[0]?.key || 'x');
    const [isDropdownOpen, setIsDropdownOpen] = useState(false);
    const [compareField, setCompareField] = useState<string | null>(null);

    // Resolve range
    const rangedHistory = useMemo(() => {
        if (rangeStart !== undefined && rangeEnd !== undefined) {
            return history.slice(rangeStart, rangeEnd + 1);
        }
        return history;
    }, [history, rangeStart, rangeEnd]);

    // Extract values for selected field
    const primaryValues = useMemo(() => {
        return rangedHistory.map(e => resolveTelemetryField(e, selectedField));
    }, [rangedHistory, selectedField]);

    const compareValues = useMemo(() => {
        if (!compareField) return null;
        return rangedHistory.map(e => resolveTelemetryField(e, compareField));
    }, [rangedHistory, compareField]);

    const primaryStats = useMemo(() => {
        const field = ALL_FIELDS.find(f => f.key === selectedField);
        return computeStats(primaryValues, field?.label || selectedField);
    }, [primaryValues, selectedField]);

    const compareStats = useMemo(() => {
        if (!compareValues || !compareField) return null;
        const field = ALL_FIELDS.find(f => f.key === compareField);
        return computeStats(compareValues, field?.label || compareField);
    }, [compareValues, compareField]);

    const histogram = useMemo(() => computeHistogram(primaryValues, 24), [primaryValues]);

    const selectedFieldDef = ALL_FIELDS.find(f => f.key === selectedField);

    return (
        <div className="h-full flex flex-col bg-slate-950 text-slate-100 overflow-hidden font-mono">
            {/* Header */}
            <div className="p-3 bg-slate-900/60 border-b border-slate-800 flex items-center gap-3 shrink-0">
                <BarChart3 className="w-4 h-4 text-violet-400" />
                <span className="text-[9px] font-bold uppercase tracking-widest text-slate-400">Statistics Engine</span>
                <span className="text-[8px] text-slate-600 ml-auto">
                    {rangedHistory.length} samples
                </span>
            </div>

            {/* Field selector */}
            <div className="px-3 py-2 border-b border-slate-800/50 relative">
                <button
                    onClick={() => setIsDropdownOpen(!isDropdownOpen)}
                    className="w-full flex items-center justify-between px-3 py-1.5 bg-slate-900 border border-slate-700 rounded-lg text-[10px] text-slate-300 hover:border-slate-500 transition-all"
                >
                    <span>{selectedFieldDef?.label || selectedField}</span>
                    <ChevronDown className={`w-3 h-3 text-slate-500 transition-transform ${isDropdownOpen ? 'rotate-180' : ''}`} />
                </button>

                {isDropdownOpen && (
                    <div className="absolute top-full left-3 right-3 z-50 mt-1 bg-slate-900 border border-slate-700 rounded-lg shadow-xl max-h-48 overflow-y-auto">
                        {Object.entries(TELEMETRY_FIELDS).map(([group, fields]) => (
                            <div key={group}>
                                <div className="px-3 py-1 text-[8px] text-slate-600 uppercase tracking-widest bg-slate-800/50">{group}</div>
                                {fields.map((f: any) => (
                                    <button
                                        key={f.key}
                                        onClick={() => { setSelectedField(f.key); setIsDropdownOpen(false); }}
                                        className={`w-full text-left px-3 py-1.5 text-[10px] hover:bg-slate-800 transition-colors ${
                                            f.key === selectedField ? 'text-violet-400 bg-violet-500/10' : 'text-slate-300'
                                        }`}
                                    >
                                        {f.label} <span className="text-slate-600 ml-1">{f.unit}</span>
                                    </button>
                                ))}
                            </div>
                        ))}
                    </div>
                )}
            </div>

            {/* Stats table */}
            <div className="flex-1 overflow-y-auto p-3 space-y-3">
                <StatsTable stats={primaryStats} color="violet" />
                {compareStats && <StatsTable stats={compareStats} color="emerald" />}

                {/* Histogram */}
                <div className="bg-slate-900/40 border border-slate-800 rounded-lg p-3">
                    <h4 className="text-[8px] text-slate-500 uppercase tracking-widest mb-2">Distribution</h4>
                    <HistogramChart histogram={histogram} />
                </div>

                {/* Compare selector */}
                <div className="bg-slate-900/40 border border-slate-800 rounded-lg p-3">
                    <h4 className="text-[8px] text-slate-500 uppercase tracking-widest mb-2">Compare With</h4>
                    <select
                        value={compareField || ''}
                        onChange={(e) => setCompareField(e.target.value || null)}
                        className="w-full bg-slate-900 border border-slate-700 rounded px-2 py-1 text-[10px] text-slate-300"
                    >
                        <option value="">None</option>
                        {ALL_FIELDS.map((f: any) => (
                            <option key={f.key} value={f.key}>{f.label}</option>
                        ))}
                    </select>
                </div>
            </div>
        </div>
    );
};

const StatsTable: React.FC<{ stats: StatResult; color: string }> = ({ stats, color }) => {
    const rows = [
        { label: 'Mean', value: stats.mean },
        { label: 'Median', value: stats.median },
        { label: 'Std Dev', value: stats.stdDev },
        { label: 'Min', value: stats.min },
        { label: 'Max', value: stats.max },
        { label: 'RMS', value: stats.rms },
    ];

    const colorClasses: Record<string, string> = {
        violet: 'border-violet-500/30 text-violet-400',
        emerald: 'border-emerald-500/30 text-emerald-400',
    };

    return (
        <div className={`bg-slate-900/40 border rounded-lg p-3 ${colorClasses[color] || ''}`}>
            <h4 className="text-[9px] font-bold uppercase tracking-wider mb-2">
                {stats.label} <span className="text-slate-600 font-normal">({stats.count} pts)</span>
            </h4>
            <div className="grid grid-cols-2 gap-1">
                {rows.map(r => (
                    <div key={r.label} className="flex justify-between items-center py-0.5">
                        <span className="text-[9px] text-slate-500">{r.label}</span>
                        <span className="text-[10px] text-slate-200">{r.value.toFixed(4)}</span>
                    </div>
                ))}
            </div>
        </div>
    );
};

const HistogramChart: React.FC<{ histogram: { edges: number[]; counts: number[] } }> = ({ histogram }) => {
    const { counts } = histogram;
    const maxCount = Math.max(...counts, 1);
    const barCount = counts.length;

    if (barCount === 0) {
        return <div className="text-[9px] text-slate-600 text-center py-4">No data</div>;
    }

    return (
        <div className="flex items-end gap-[1px] h-16">
            {counts.map((count, i) => {
                const heightPct = (count / maxCount) * 100;
                return (
                    <div
                        key={i}
                        className="flex-1 bg-violet-500/30 hover:bg-violet-500/50 transition-colors rounded-t-sm min-w-[2px]"
                        style={{ height: `${Math.max(heightPct, 1)}%` }}
                        title={`${count} samples`}
                    />
                );
            })}
        </div>
    );
};

export default StatisticsPanel;
