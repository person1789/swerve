import React, { useState } from 'react';
import { Settings2, Zap, Cpu, RotateCcw, Activity, ChevronDown, ChevronRight, Sliders } from 'lucide-react';
import { SwerveConfig } from '../lib/SwerveLogic';

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
    const [useRealtime, setUseRealtime] = useState(SwerveConfig.USE_REALTIME);
    const [, forceUpdate] = useState(0);

    const toggleRealtime = () => {
        SwerveConfig.USE_REALTIME = !SwerveConfig.USE_REALTIME;
        setUseRealtime(SwerveConfig.USE_REALTIME);
    };

    const updateConfig = (key: keyof typeof SwerveConfig, val: number) => {
        (SwerveConfig as any)[key] = val;
        forceUpdate(s => s + 1);
    };

    return (
        <div className="w-80 h-screen bg-slate-900 border-r border-slate-800 p-6 flex flex-col gap-6 shadow-2xl relative z-20 overflow-y-auto custom-scrollbar">
            <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-blue-600/20 rounded-xl flex items-center justify-center border border-blue-500/30">
                    <Zap className="w-6 h-6 text-blue-400" />
                </div>
                <div>
                    <h1 className="text-xl font-bold tracking-tight text-white">Swerve Sim</h1>
                    <p className="text-[10px] uppercase tracking-widest text-slate-500 font-bold">Hardware Twin v2.1</p>
                </div>
            </div>

            {/* Sim Control */}
            <div className="p-4 bg-slate-950/50 rounded-xl border border-slate-800 space-y-3">
                <div className="flex items-center justify-between">
                    <span className="text-[10px] text-slate-500 uppercase font-bold tracking-tight">Time Step</span>
                    <button 
                        onClick={toggleRealtime}
                        className={`text-[9px] px-2 py-0.5 rounded border font-mono transition-all ${
                            useRealtime ? 'bg-blue-600/20 border-blue-500/50 text-blue-400' : 'bg-orange-600/20 border-orange-500/50 text-orange-400'
                        }`}
                    >
                        {useRealtime ? "REAL-TIME" : "FIXED STEP"}
                    </button>
                </div>
                <button 
                    className="w-full py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 text-[10px] font-bold uppercase rounded-lg flex items-center justify-center gap-2 transition-all border border-slate-700"
                    onClick={() => window.location.reload()}
                >
                    <RotateCcw className="w-3 h-3" /> Reset Sensors
                </button>
            </div>

            {/* Tuning Dashboard */}
            <div className="space-y-2">
                <CollapsibleSection title="Module Tuning" icon={<Cpu className="w-3.5 h-3.5" />}>
                    <TuningSlider label="Steer P" value={SwerveConfig.STEER_P} min={0} max={1} step={0.01} onChange={(v: number) => updateConfig('STEER_P', v)} />
                    <TuningSlider label="Drive P" value={SwerveConfig.DRIVE_P} min={0} max={1} step={0.01} onChange={(v: number) => updateConfig('DRIVE_P', v)} />
                </CollapsibleSection>

                <CollapsibleSection title="Chassis Stability" icon={<Sliders className="w-3.5 h-3.5" />}>
                    <TuningSlider label="Heading P" value={SwerveConfig.HEADING_P} min={0} max={3} step={0.1} onChange={(v: number) => updateConfig('HEADING_P', v)} />
                    <TuningSlider label="Snap P" value={SwerveConfig.SNAP_P} min={0} max={5} step={0.1} onChange={(v: number) => updateConfig('SNAP_P', v)} />
                </CollapsibleSection>

                <CollapsibleSection title="Power Management" icon={<Zap className="w-3.5 h-3.5" />}>
                   <TuningSlider label="Battery Voltage" value={SwerveConfig.BATTERY_VOLTAGE} min={9} max={14.5} step={0.1} onChange={(v: number) => updateConfig('BATTERY_VOLTAGE', v)} />
                </CollapsibleSection>
            </div>

            {/* Visualization */}
            <div className="space-y-3">
                <div className="text-[10px] font-bold text-slate-500 uppercase tracking-widest flex items-center gap-2">
                    <Settings2 className="w-3 h-3" /> Visualization
                </div>
                <div className="grid grid-cols-1 gap-2">
                    <MiniToggle active={showModuleVectors} onClick={() => setShowModuleVectors(!showModuleVectors)} label="Module Vectors" color="bg-green-500" />
                    <MiniToggle active={showChassisVector} onClick={() => setShowChassisVector(!showChassisVector)} label="Chassis Vector" color="bg-blue-500" />
                    <MiniToggle active={showRotationVector} onClick={() => setShowRotationVector(!showRotationVector)} label="Rotation Vector" color="bg-purple-500" />
                </div>
            </div>

            <div className="mt-auto pt-4 border-t border-slate-800">
                <div className="flex items-center justify-between text-[10px] font-mono text-slate-600 uppercase tracking-widest">
                    <div className="flex items-center gap-1.5">
                        <Activity className="w-3 h-3 text-blue-500" />
                        <span>Core Active</span>
                    </div>
                </div>
            </div>
        </div>
    );
};

const CollapsibleSection = ({ title, icon, children }: any) => {
    const [isOpen, setIsOpen] = useState(false);
    return (
        <div className="border border-slate-800 rounded-xl overflow-hidden bg-slate-900/50">
            <button 
                onClick={() => setIsOpen(!isOpen)}
                className="w-full px-4 py-3 flex items-center justify-between hover:bg-slate-800/50 transition-colors"
            >
                <div className="flex items-center gap-3 text-slate-400">
                    {icon}
                    <span className="text-xs font-bold uppercase tracking-tight text-slate-300">{title}</span>
                </div>
                {isOpen ? <ChevronDown className="w-4 h-4 text-slate-500" /> : <ChevronRight className="w-4 h-4 text-slate-500" />}
            </button>
            {isOpen && <div className="p-4 pt-0 space-y-4 border-t border-slate-800/50 bg-slate-950/20">{children}</div>}
        </div>
    );
};

const TuningSlider = ({ label, value, min, max, step, onChange }: any) => (
    <div className="space-y-2">
        <div className="flex justify-between items-center">
            <span className="text-[10px] font-mono text-slate-500 uppercase">{label}</span>
            <span className="text-[10px] font-bold text-blue-400 bg-blue-400/10 px-1.5 py-0.5 rounded">{value.toFixed(2)}</span>
        </div>
        <input 
            type="range" min={min} max={max} step={step} value={value}
            onChange={(e) => onChange(parseFloat(e.target.value))}
            className="w-full h-1 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-blue-500"
        />
    </div>
);

const MiniToggle = ({ active, onClick, label, color }: any) => (
    <button onClick={onClick} className={`flex items-center justify-between p-3 rounded-xl border transition-all ${active ? 'bg-slate-800 border-slate-700' : 'bg-transparent border-slate-800/50 opacity-40'}`}>
        <span className="text-xs font-medium text-slate-300">{label}</span>
        <div className={`w-2 h-2 rounded-full ${active ? color : 'bg-slate-700'} ${active ? 'shadow-[0_0_8px_rgba(59,130,246,0.5)]' : ''}`} />
    </button>
);

export default Sidebar;
