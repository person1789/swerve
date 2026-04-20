import { useState } from 'react';
import SwerveSim from './components/SwerveSim';
import Sidebar from './components/Sidebar';

function App() {
  const [showModuleVectors, setShowModuleVectors] = useState(true);
  const [showChassisVector, setShowChassisVector] = useState(true);
  const [showRotationVector, setShowRotationVector] = useState(true);

  return (
    <div className="flex w-full h-screen bg-background text-foreground overflow-hidden">
      <Sidebar 
        showModuleVectors={showModuleVectors} 
        setShowModuleVectors={setShowModuleVectors}
        showChassisVector={showChassisVector}
        setShowChassisVector={setShowChassisVector}
        showRotationVector={showRotationVector}
        setShowRotationVector={setShowRotationVector}
      />
      
      <main className="flex-1 relative">
        <header className="absolute top-0 left-0 right-0 p-6 flex justify-between items-center pointer-events-none z-10">
          <div>
            <h2 className="text-sm font-mono text-gray-500 uppercase">Operational Environment</h2>
            <div className="flex items-center gap-2">
              <div className="w-2 h-2 rounded-full bg-green-500 animate-pulse" />
              <p className="text-xs font-mono text-green-500/80 tracking-wide">SYSTEM NOMINAL // HIGH-FIDELITY ACTIVE</p>
            </div>
          </div>
          <div className="text-right">
            <h2 className="text-sm font-mono text-gray-500 uppercase">Robot Model</h2>
            <p className="text-xl font-black tracking-tighter text-white">4-POD SWERVE V1</p>
          </div>
        </header>

        <SwerveSim 
          showModuleVectors={showModuleVectors}
          showChassisVector={showChassisVector}
          showRotationVector={showRotationVector}
        />

        <footer className="absolute bottom-6 left-6 right-6 flex justify-between items-end pointer-events-none z-10">
          <div className="bg-black/60 backdrop-blur-md p-3 rounded-lg border border-white/5">
            <p className="text-[10px] font-mono text-gray-500">
               COORD_SYS: FIELD_CENTRIC <br/>
               DRV_MD: ASYMMETRIC_BRAKING <br/>
               SNAPPING: LOGIC_ACTIVE
            </p>
          </div>
          <p className="text-[10px] font-mono text-gray-700">
            CONNECT GAMEPAD TO INITIALIZE INPUT ENGINE
          </p>
        </footer>
      </main>
    </div>
  );
}

export default App;
