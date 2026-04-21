import React, { useEffect, useState } from 'react';
import { db, type SavedSession } from '../lib/db';
import { FolderOpen, Play, Trash2, Calendar, Clock, Database } from 'lucide-react';

interface SavedSessionsProps {
    onLoad: (session: SavedSession) => void;
}

const SavedSessions: React.FC<SavedSessionsProps> = ({ onLoad }) => {
    const [sessions, setSessions] = useState<SavedSession[]>([]);

    const loadSessions = async () => {
        const all = await db.sessions.toArray();
        setSessions(all.sort((a, b) => b.date - a.date));
    };

    useEffect(() => {
        loadSessions();
        const interval = setInterval(loadSessions, 2000); // Poll for new saves
        return () => clearInterval(interval);
    }, []);

    const handleDelete = async (id: number) => {
        if (confirm("Delete this session forever?")) {
            await db.sessions.delete(id);
            loadSessions();
        }
    };

    return (
        <div className="h-full flex flex-col bg-slate-900 overflow-hidden font-sans">
            <div className="p-4 bg-slate-800/50 border-b border-slate-700 flex items-center justify-between shrink-0">
                <h2 className="text-[10px] font-black text-slate-200 uppercase tracking-widest flex items-center gap-2">
                    <Database className="w-3.5 h-3.5 text-blue-400" />
                    Session_Archive
                </h2>
                <div className="text-[8px] font-mono text-slate-500 uppercase tracking-tighter">
                    {sessions.length} Saved Captures
                </div>
            </div>

            <div className="flex-1 overflow-y-auto p-4 custom-scrollbar space-y-2">
                {sessions.map((session) => (
                    <div 
                        key={session.id}
                        className="group p-3 rounded-lg border border-slate-800 bg-slate-950/40 hover:bg-slate-800/30 hover:border-blue-500/30 transition-all"
                    >
                        <div className="flex justify-between items-start mb-2">
                            <div>
                                <h3 className="text-xs font-bold text-slate-300 group-hover:text-blue-400 transition-colors">
                                    {session.name}
                                </h3>
                                <div className="flex items-center gap-3 mt-1 text-[8px] font-mono text-slate-500 uppercase tracking-wide">
                                    <span className="flex items-center gap-1">
                                        <Calendar className="w-2.5 h-3" />
                                        {new Date(session.date).toLocaleDateString()}
                                    </span>
                                    <span className="flex items-center gap-1">
                                        <Clock className="w-2.5 h-3" />
                                        {session.duration.toFixed(1)}s
                                    </span>
                                    <span className="px-1.5 py-0.5 rounded-full bg-slate-800 border border-slate-700">
                                        {session.frames.length} FRAMES
                                    </span>
                                </div>
                            </div>
                            <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                                <button 
                                    onClick={() => session.id && handleDelete(session.id)}
                                    className="p-1.5 rounded hover:bg-red-500/20 text-slate-600 hover:text-red-400 transition-all"
                                >
                                    <Trash2 className="w-3.5 h-3.5" />
                                </button>
                                <button 
                                    onClick={() => onLoad(session)}
                                    className="flex items-center gap-1.5 px-2.5 py-1.5 bg-blue-600 text-white rounded text-[9px] font-bold hover:bg-blue-500 active:scale-95 transition-all"
                                >
                                    <Play className="w-3 h-3 fill-current" /> LOAD
                                </button>
                            </div>
                        </div>
                    </div>
                ))}

                {sessions.length === 0 && (
                    <div className="flex flex-col items-center justify-center h-64 text-slate-700 border-2 border-dashed border-slate-800 rounded-xl">
                        <FolderOpen className="w-8 h-8 mb-3 opacity-20" />
                        <p className="text-[10px] font-mono uppercase tracking-widest italic">No archived telemetry</p>
                    </div>
                )}
            </div>
            
            <div className="p-3 bg-slate-950/80 border-t border-slate-800 pointer-events-none">
                <p className="text-[8px] font-mono text-slate-600 uppercase tracking-tighter leading-relaxed">
                    Recording is automatic. Use the SAVE button in the header to persist the current 10-minute flight recorder buffer to this archive.
                </p>
            </div>
        </div>
    );
};

export default SavedSessions;
