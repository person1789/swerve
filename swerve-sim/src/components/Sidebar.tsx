import React from 'react';

interface SidebarProps {
    showModuleVectors: boolean;
    setShowModuleVectors: (val: boolean) => void;
    showChassisVector: boolean;
    setShowChassisVector: (val: boolean) => void;
    showRotationVector: boolean;
    setShowRotationVector: (val: boolean) => void;
}

const Sidebar: React.FC<SidebarProps> = ({ 
    showModuleVectors, setShowModuleVectors,
    showChassisVector, setShowChassisVector,
    showRotationVector, setShowRotationVector
}) => {

    const sendSnap = (direction: string) => {
        // Find the global socket and send a snap command
        // This is a quick bridge for SITL manual testing
        const socket = (window as any).sitlSocket;
        if (socket && socket.readyState === WebSocket.OPEN) {
            const cmd: any = { drive: 0, strafe: 0, turn: 0 };
            cmd[`dpad_${direction}`] = true;
            socket.send(JSON.stringify(cmd));
        }
    };

    return (
        <aside className="w-80 h-full border-r border-slate-800 bg-slate-900/50 backdrop-blur-xl p-8 flex flex-col gap-8 z-20">
            <div>
                <h1 className="text-xl font-bold tracking-tighter bg-gradient-to-br from-white to-slate-400 bg-clip-text text-transparent">
                    SWERVE <span className="text-blue-500">SITL</span>
                </h1>
                <p className="text-[10px] font-mono text-slate-500 mt-1 uppercase tracking-widest">Digital Twin Environment v2.0</p>
            </div>

            <nav className="flex flex-col gap-6">
                <section>
                    <h3 className="text-[10px] font-bold text-slate-500 uppercase tracking-widest mb-4">Diagnostic Views</h3>
                    <div className="flex flex-col gap-3">
                        <ToggleButton label="Module Vectors" active={showModuleVectors} onClick={() => setShowModuleVectors(!showModuleVectors)} color="emerald" />
                        <ToggleButton label="Chassis Vector" active={showChassisVector} onClick={() => setShowChassisVector(!showChassisVector)} color="blue" />
                        <ToggleButton label="Angular Vector" active={showRotationVector} onClick={() => setShowRotationVector(!showRotationVector)} color="purple" />
                    </div>
                </section>

                <section>
                    <h3 className="text-[10px] font-bold text-slate-500 uppercase tracking-widest mb-4">Cardinal Snapping (SITL)</h3>
                    <div className="grid grid-cols-2 gap-2">
                        <ActionButton label="NORTH" onClick={() => sendSnap('up')} />
                        <ActionButton label="WEST" onClick={() => sendSnap('left')} />
                        <ActionButton label="EAST" onClick={() => sendSnap('right')} />
                        <ActionButton label="SOUTH" onClick={() => sendSnap('down')} />
                    </div>
                </section>

                <section>
                    <h3 className="text-[10px] font-bold text-slate-500 uppercase tracking-widest mb-4">History Management</h3>
                    <button 
                        onClick={() => (window as any).clearSwervePath?.()}
                        className="w-full px-3 py-2 text-[9px] font-mono font-bold bg-red-500/10 hover:bg-red-500/20 text-red-400 border border-red-500/30 rounded transition-all uppercase"
                    >
                        Clear Odometry Path
                    </button>
                </section>
            </nav>

            <div className="mt-auto pt-6 border-t border-slate-800">
                <div className="flex items-center gap-2 mb-2">
                    <div className="w-1.5 h-1.5 rounded-full bg-amber-500 shadow-[0_0_8px_rgba(245,158,11,0.5)]" />
                    <span className="text-[10px] font-mono text-slate-400 uppercase tracking-wider">Hardware Loop: 20ms</span>
                </div>
                <p className="text-[9px] leading-relaxed text-slate-500 italic">
                    Simulation state is mirrored from production Java code on localhost:8080.
                </p>
            </div>
        </aside>
    );
};

const ToggleButton = ({ label, active, onClick, color }: { label: string, active: boolean, onClick: () => void, color: string }) => {
    const colorClasses: Record<string, string> = {
        emerald: active ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/50' : 'text-slate-400 border-slate-800 hover:border-slate-700',
        blue: active ? 'bg-blue-500/10 text-blue-400 border-blue-500/50' : 'text-slate-400 border-slate-800 hover:border-slate-700',
        purple: active ? 'bg-purple-500/10 text-purple-400 border-purple-500/50' : 'text-slate-400 border-slate-800 hover:border-slate-700'
    };

    return (
        <button 
            onClick={onClick}
            className={`w-full px-4 py-2 text-[10px] font-mono font-bold uppercase text-left border rounded-lg transition-all ${colorClasses[color]}`}
        >
            <div className="flex justify-between items-center">
                {label}
                <div className={`w-1 h-1 rounded-full ${active ? 'bg-current shadow-[0_0_5px_currentColor]' : 'bg-slate-800'}`} />
            </div>
        </button>
    );
};

const ActionButton = ({ label, onClick }: { label: string, onClick: () => void }) => (
    <button 
        onClick={onClick}
        className="px-3 py-2 text-[9px] font-mono font-bold bg-slate-800/50 hover:bg-slate-700 text-slate-300 border border-slate-700 rounded transition-all active:scale-95"
    >
        {label}
    </button>
);

export default Sidebar;
