import React, { useState, useRef, useEffect, useMemo } from 'react';
import { Terminal, Search, ArrowDown, Filter } from 'lucide-react';
import type { TelemetryEntry, LogMessage } from '../types/telemetry';

interface ConsolePanelProps {
    history: TelemetryEntry[];
    isLive: boolean;
}

type LogLevel = 'DEBUG' | 'INFO' | 'WARNING' | 'ERROR';

const LEVEL_COLORS: Record<LogLevel, { bg: string; text: string; badge: string }> = {
    DEBUG:   { bg: 'hover:bg-slate-800/30', text: 'text-slate-500',   badge: 'bg-slate-700 text-slate-400' },
    INFO:    { bg: 'hover:bg-blue-500/5',   text: 'text-blue-400',    badge: 'bg-blue-500/15 text-blue-400' },
    WARNING: { bg: 'hover:bg-amber-500/5',  text: 'text-amber-400',   badge: 'bg-amber-500/15 text-amber-400' },
    ERROR:   { bg: 'hover:bg-red-500/5',    text: 'text-red-400',     badge: 'bg-red-500/15 text-red-400' },
};

const ConsolePanel: React.FC<ConsolePanelProps> = ({ history, isLive }) => {
    const [searchQuery, setSearchQuery] = useState('');
    const [activeFilters, setActiveFilters] = useState<Set<LogLevel>>(new Set(['DEBUG', 'INFO', 'WARNING', 'ERROR']));
    const [autoScroll, setAutoScroll] = useState(true);
    const scrollRef = useRef<HTMLDivElement>(null);

    // Extract all log messages from history, plus generate system messages
    const allMessages = useMemo(() => {
        const msgs: (LogMessage & { frameIndex: number })[] = [];

        history.forEach((entry, frameIndex) => {
            // Add explicit log messages if present
            if (entry.logMessages) {
                entry.logMessages.forEach(msg => {
                    msgs.push({ ...msg, frameIndex });
                });
            }

            // Auto-generate system events from state transitions
            if (frameIndex > 0) {
                const prev = history[frameIndex - 1];

                // Snap activation
                if (entry.isSnapping && !prev.isSnapping) {
                    msgs.push({
                        level: 'INFO',
                        tag: 'SwerveController',
                        message: `Cardinal snap activated`,
                        timestamp: entry.timestamp,
                        frameIndex,
                    });
                }

                // Snap deactivation
                if (!entry.isSnapping && prev.isSnapping) {
                    msgs.push({
                        level: 'INFO',
                        tag: 'SwerveController',
                        message: `Snap target reached`,
                        timestamp: entry.timestamp,
                        frameIndex,
                    });
                }

                // Heading retention changes
                if (entry.isMaintaining && !prev.isMaintaining) {
                    msgs.push({
                        level: 'DEBUG',
                        tag: 'SwerveController',
                        message: `Heading retention engaged at ${(entry.heading * 180 / Math.PI).toFixed(1)}°`,
                        timestamp: entry.timestamp,
                        frameIndex,
                    });
                }

                // Drivetrain state changes
                if (entry.drivetrainState && entry.drivetrainState !== prev.drivetrainState) {
                    msgs.push({
                        level: 'INFO',
                        tag: 'Drivetrain',
                        message: `State → ${entry.drivetrainState}`,
                        timestamp: entry.timestamp,
                        frameIndex,
                    });
                }

                // High-speed warning
                const speed = Math.hypot(
                    (entry.actuals?.[0]?.[0] ?? 0),
                    (entry.actuals?.[1]?.[0] ?? 0),
                    (entry.actuals?.[2]?.[0] ?? 0),
                    (entry.actuals?.[3]?.[0] ?? 0),
                ) / 4;
                if (speed > 1.2 && Math.hypot(
                    (prev.actuals?.[0]?.[0] ?? 0),
                    (prev.actuals?.[1]?.[0] ?? 0),
                    (prev.actuals?.[2]?.[0] ?? 0),
                    (prev.actuals?.[3]?.[0] ?? 0),
                ) / 4 <= 1.2) {
                    msgs.push({
                        level: 'WARNING',
                        tag: 'Performance',
                        message: `High speed detected: ${speed.toFixed(2)} m/s (approaching limit)`,
                        timestamp: entry.timestamp,
                        frameIndex,
                    });
                }
            }
        });

        return msgs;
    }, [history]);

    // Filter + search
    const filteredMessages = useMemo(() => {
        return allMessages.filter(msg => {
            if (!activeFilters.has(msg.level)) return false;
            if (searchQuery) {
                const q = searchQuery.toLowerCase();
                return msg.message.toLowerCase().includes(q) || msg.tag.toLowerCase().includes(q);
            }
            return true;
        });
    }, [allMessages, activeFilters, searchQuery]);

    // Auto-scroll
    useEffect(() => {
        if (autoScroll && scrollRef.current) {
            scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
        }
    }, [filteredMessages.length, autoScroll]);

    const toggleFilter = (level: LogLevel) => {
        setActiveFilters(prev => {
            const next = new Set(prev);
            if (next.has(level)) next.delete(level);
            else next.add(level);
            return next;
        });
    };

    const formatTime = (ts: number) => {
        const mins = Math.floor(ts / 60);
        const secs = ts % 60;
        return `${mins.toString().padStart(2, '0')}:${secs.toFixed(2).padStart(5, '0')}`;
    };

    return (
        <div className="h-full flex flex-col bg-slate-950 text-slate-100 overflow-hidden font-mono">
            {/* Header */}
            <div className="p-2 bg-slate-900/60 border-b border-slate-800 flex items-center gap-2 shrink-0">
                <Terminal className="w-3.5 h-3.5 text-emerald-400" />
                <span className="text-[9px] font-bold uppercase tracking-widest text-slate-400">Console</span>

                {/* Level Filter Buttons */}
                <div className="flex items-center gap-1 ml-3">
                    {(['ERROR', 'WARNING', 'INFO', 'DEBUG'] as LogLevel[]).map(level => (
                        <button
                            key={level}
                            onClick={() => toggleFilter(level)}
                            className={`px-1.5 py-0.5 text-[7px] font-bold rounded transition-all ${
                                activeFilters.has(level)
                                    ? LEVEL_COLORS[level].badge
                                    : 'bg-slate-800 text-slate-600'
                            }`}
                        >
                            {level.slice(0, 3)}
                        </button>
                    ))}
                </div>

                {/* Auto-scroll toggle */}
                <button
                    onClick={() => setAutoScroll(!autoScroll)}
                    className={`ml-auto p-1 rounded transition-all ${autoScroll ? 'text-emerald-400' : 'text-slate-600'}`}
                    title={autoScroll ? 'Auto-scroll ON' : 'Auto-scroll OFF'}
                >
                    <ArrowDown className="w-3 h-3" />
                </button>

                <span className="text-[8px] text-slate-600">{filteredMessages.length}</span>
            </div>

            {/* Search */}
            <div className="px-2 py-1.5 border-b border-slate-800/50 flex items-center gap-2">
                <Search className="w-3 h-3 text-slate-600" />
                <input
                    type="text"
                    placeholder="Filter messages..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    className="flex-1 bg-transparent text-[10px] text-slate-300 placeholder-slate-700 outline-none"
                />
                {searchQuery && (
                    <button
                        onClick={() => setSearchQuery('')}
                        className="text-[8px] text-slate-600 hover:text-slate-400"
                    >
                        ✕
                    </button>
                )}
            </div>

            {/* Message list */}
            <div ref={scrollRef} className="flex-1 overflow-y-auto">
                {filteredMessages.length === 0 ? (
                    <div className="flex items-center justify-center h-full">
                        <span className="text-[10px] text-slate-700">
                            {history.length === 0 ? 'Waiting for telemetry...' : 'No messages match filters'}
                        </span>
                    </div>
                ) : (
                    filteredMessages.map((msg, i) => {
                        const colors = LEVEL_COLORS[msg.level];
                        return (
                            <div
                                key={i}
                                className={`flex items-start gap-2 px-2 py-0.5 text-[9px] leading-tight border-b border-slate-900/50 ${colors.bg} transition-colors`}
                            >
                                <span className="text-slate-600 shrink-0 w-14 text-right tabular-nums">
                                    [{formatTime(msg.timestamp)}]
                                </span>
                                <span className={`shrink-0 px-1 rounded text-[7px] font-bold ${colors.badge}`}>
                                    {msg.level.slice(0, 3)}
                                </span>
                                <span className="text-slate-500 shrink-0 w-24 truncate">
                                    {msg.tag}
                                </span>
                                <span className={`${colors.text} flex-1`}>
                                    {msg.message}
                                </span>
                            </div>
                        );
                    })
                )}
            </div>

            {/* Status bar */}
            <div className="px-2 py-1 bg-slate-900 border-t border-slate-800 flex justify-between text-[8px] text-slate-600 shrink-0">
                <span>{isLive ? '● LIVE' : '○ REPLAY'}</span>
                <span>{allMessages.length} total / {filteredMessages.length} shown</span>
            </div>
        </div>
    );
};

export default ConsolePanel;
