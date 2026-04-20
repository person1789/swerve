import React from 'react';
import { Settings2, Zap, ArrowUpRight, Gamepad2 } from 'lucide-react';

interface SidebarProps {
    showModuleVectors: boolean;
    setShowModuleVectors: (v: boolean) => void;
    showChassisVector: boolean;
    setShowChassisVector: (v: boolean) => void;
    showRotationVector: boolean;
    setShowRotationVector: (v: boolean) => void;
}

const Sidebar: React.FC<SidebarProps> = ({
    showModuleVectors, setShowModuleVectors,
    showChassisVector, setShowChassisVector,
    showRotationVector, setShowRotationVector
}) => {
    return (
        <div className="w-80 h-screen bg-black/40 backdrop-blur-xl border-r border-white/10 p-6 flex flex-col gap-8 shadow-2xl">
            <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-primary/20 rounded-lg flex items-center justify-center border border-primary/30">
                    <Zap className="w-6 h-6 text-primary" />
                </div>
                <div>
                    <h1 className="text-xl font-bold tracking-tight text-white">Swerve Sim</h1>
                    <p className="text-[10px] uppercase tracking-widest text-gray-500 font-semibold">Digital Twin V1.0</p>
                </div>
            </div>

            <div className="space-y-4">
                <div className="text-xs font-bold text-gray-400 uppercase tracking-wider flex items-center gap-2">
                    <Settings2 className="w-3 h-3" /> Visualization
                </div>
                <div className="space-y-3">
                    <ToggleItem 
                        label="Module Vectors" 
                        active={showModuleVectors} 
                        onClick={() => setShowModuleVectors(!showModuleVectors)} 
                        color="text-green-400"
                    />
                    <ToggleItem 
                        label="Chassis Velocity" 
                        active={showChassisVector} 
                        onClick={() => setShowChassisVector(!showChassisVector)} 
                        color="text-blue-400"
                    />
                    <ToggleItem 
                        label="Rotation Vector" 
                        active={showRotationVector} 
                        onClick={() => setShowRotationVector(!showRotationVector)} 
                        color="text-purple-400"
                    />
                </div>
            </div>

            <div className="space-y-4">
                <div className="text-xs font-bold text-gray-400 uppercase tracking-wider flex items-center gap-2">
                    <Gamepad2 className="w-3 h-3" /> Controller 
                </div>
                <div className="bg-white/5 border border-white/5 rounded-xl p-4 space-y-3">
                    <div className="flex items-center justify-between text-xs font-mono text-gray-400">
                        <span>Left Stick</span>
                        <span className="text-white">Translate</span>
                    </div>
                    <div className="flex items-center justify-between text-xs font-mono text-gray-400">
                        <span>Right Stick</span>
                        <span className="text-white">Rotate (Yaw)</span>
                    </div>
                    <div className="flex items-center justify-between text-xs font-mono text-gray-400">
                        <span>Options</span>
                        <span className="text-white">Reset Position</span>
                    </div>
                </div>
            </div>

            <div className="mt-auto space-y-4">
                <div className="p-4 bg-primary/10 rounded-xl border border-primary/20">
                    <p className="text-xs text-gray-400 leading-relaxed italic">
                        "Digital Twin active. Asymmetric braking behavior verified for manual driver inputs."
                    </p>
                </div>
                <div className="flex items-center justify-between text-[10px] text-gray-600 font-mono">
                    <span>BUILD: 2026.04.BC</span>
                    <span>FPS: 60</span>
                </div>
            </div>
        </div>
    );
};

const ToggleItem = ({ label, active, onClick, color }: { label: string, active: boolean, onClick: () => void, color: string }) => (
    <button 
        onClick={onClick}
        className={`w-full p-3 rounded-xl border transition-all duration-200 flex items-center justify-between group ${
            active ? 'bg-white/10 border-white/20' : 'bg-transparent border-white/5 opacity-50 gray-scale hover:opacity-100 hover:border-white/10'
        }`}
    >
        <div className="flex items-center gap-3">
            <div className={`p-2 rounded-lg ${active ? 'bg-white/10' : 'bg-white/5'}`}>
                <ArrowUpRight className={`w-4 h-4 ${active ? color : 'text-gray-500'}`} />
            </div>
            <span className={`text-sm font-medium ${active ? 'text-white' : 'text-gray-400'}`}>{label}</span>
        </div>
        <div className={`w-2 h-2 rounded-full ${active ? 'bg-green-500 shadow-[0_0_8px_rgba(34,197,94,0.6)]' : 'bg-gray-700'}`} />
    </button>
);

export default Sidebar;
