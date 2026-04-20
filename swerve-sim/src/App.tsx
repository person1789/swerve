import { useState, useCallback } from 'react';
import SwerveSim from './components/SwerveSim';
import Sidebar from './components/Sidebar';
import TelemetryPanel from './components/TelemetryPanel';

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
    [key: string]: number;
}

function App() {
  const [showModuleVectors, setShowModuleVectors] = useState(true);
  const [showChassisVector, setShowChassisVector] = useState(true);
  const [showRotationVector, setShowRotationVector] = useState(true);

  // Telemetry State
  const [telemetryData, setTelemetryData] = useState<TelemetryEntry[]>([]);
  const [isRecording, setIsRecording] = useState(false);

  const handleTelemetryUpdate = useCallback((entry: TelemetryEntry) => {
    setTelemetryData(prev => {
      const next = [...prev, entry];
      // Keep only last 1000 items in memory if not recording to save performance
      if (!isRecording && next.length > 500) return next.slice(-500);
      return next;
    });
  }, [isRecording]);

  const handleExportCSV = () => {
    if (telemetryData.length === 0) return;

    const headers = Object.keys(telemetryData[0]);
    const csvRows = [
      headers.join(','), // Header row
      ...telemetryData.map(row => headers.map(header => row[header]).join(','))
    ];

    const csvContent = csvRows.join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.setAttribute('href', url);
    link.setAttribute('download', `swerve_telemetry_${new Date().toISOString()}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div className="flex w-full h-screen bg-slate-950 text-slate-100 overflow-hidden">
      {/* Sidebar Controls */}
      <Sidebar 
        showModuleVectors={showModuleVectors} 
        setShowModuleVectors={setShowModuleVectors}
        showChassisVector={showChassisVector}
        setShowChassisVector={setShowChassisVector}
        showRotationVector={showRotationVector}
        setShowRotationVector={setShowRotationVector}
      />
      
      {/* Main Dashboard - Split Screen */}
      <div className="flex-1 flex overflow-hidden">
        {/* Left: Robot Viewport */}
        <section className="flex-[1.2] relative border-r border-slate-800">
          <header className="absolute top-0 left-0 right-0 p-6 flex justify-between items-center pointer-events-none z-10">
            <div>
              <h2 className="text-[10px] font-mono text-slate-500 uppercase tracking-[0.2em] mb-1">Observation Environment</h2>
              <div className="flex items-center gap-2">
                <div className="w-1.5 h-1.5 rounded-full bg-blue-500 animate-pulse shadow-[0_0_8px_rgba(59,130,246,0.8)]" />
                <p className="text-[10px] font-mono text-blue-400 font-bold tracking-wider">FIXED_LOOP // 20MS POLLING</p>
              </div>
            </div>
          </header>

          <SwerveSim 
            showModuleVectors={showModuleVectors}
            showChassisVector={showChassisVector}
            showRotationVector={showRotationVector}
            onTelemetryUpdate={handleTelemetryUpdate}
          />
        </section>

        {/* Right: Telemetry Dashboard */}
        <section className="flex-1 min-w-[400px]">
          <TelemetryPanel 
            data={telemetryData}
            isRecording={isRecording}
            onStartRecording={() => setIsRecording(true)}
            onStopRecording={() => setIsRecording(false)}
            onClear={() => setTelemetryData([])}
            onExport={handleExportCSV}
          />
        </section>
      </div>
    </div>
  );
}

export default App;
