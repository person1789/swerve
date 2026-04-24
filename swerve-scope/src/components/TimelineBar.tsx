import React, { useState, useEffect, useRef } from 'react';
import { Circle, Square, Play, Pause, Download, Trash2, List } from 'lucide-react';
import { SessionRecorder, type Session } from '../lib/SessionRecorder';

interface TimelineBarProps {
  recorder: SessionRecorder;
  telemetry: Record<string, unknown>;
  onReplayFrame: (data: Record<string, unknown> | null) => void;
}

export function TimelineBar({ recorder, telemetry, onReplayFrame }: TimelineBarProps) {
  const [isRecording, setIsRecording] = useState(false);
  const [showSessions, setShowSessions] = useState(false);
  const [sessions, setSessions] = useState<{ name: string; date: number; frameCount: number }[]>([]);
  const [replaySession, setReplaySession] = useState<Session | null>(null);
  const [replayIndex, setReplayIndex] = useState(0);
  const [isPlaying, setIsPlaying] = useState(false);
  const [speed, setSpeed] = useState(1);
  const replayTimer = useRef<number>(0);

  // Record frames when recording
  useEffect(() => {
    if (isRecording && Object.keys(telemetry).length > 0) {
      recorder.addFrame(telemetry);
    }
  }, [telemetry, isRecording, recorder]);

  // Replay timer
  useEffect(() => {
    if (isPlaying && replaySession) {
      const interval = 20 / speed; // 50Hz base
      replayTimer.current = window.setInterval(() => {
        setReplayIndex(prev => {
          const next = prev + 1;
          if (next >= replaySession.frames.length) {
            setIsPlaying(false);
            return prev;
          }
          onReplayFrame(replaySession.frames[next].data);
          return next;
        });
      }, interval);
      return () => clearInterval(replayTimer.current);
    }
  }, [isPlaying, replaySession, speed, onReplayFrame]);

  const handleStartRecording = () => {
    recorder.startRecording();
    setIsRecording(true);
  };

  const handleStopRecording = async () => {
    const name = `Session ${new Date().toLocaleTimeString()}`;
    await recorder.stopAndSave(name);
    setIsRecording(false);
    refreshSessions();
  };

  const refreshSessions = async () => {
    const list = await SessionRecorder.listSessions();
    setSessions(list);
  };

  const handleLoadSession = async (name: string) => {
    const session = await SessionRecorder.loadSession(name);
    if (session) {
      setReplaySession(session);
      setReplayIndex(0);
      setIsPlaying(false);
      setShowSessions(false);
      if (session.frames.length > 0) {
        onReplayFrame(session.frames[0].data);
      }
    }
  };

  const handleDeleteSession = async (name: string) => {
    await SessionRecorder.deleteSession(name);
    refreshSessions();
  };

  const handleExportCSV = () => {
    if (!replaySession) return;
    const csv = SessionRecorder.exportCSV(replaySession);
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${replaySession.name}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const handleStopReplay = () => {
    setReplaySession(null);
    setIsPlaying(false);
    onReplayFrame(null);
  };

  const progress = replaySession ? (replayIndex / Math.max(1, replaySession.frames.length - 1)) * 100 : 0;

  return (
    <div className="glass-panel" style={{
      margin: '0 0.5rem 0.5rem',
      padding: '0.4rem 1rem',
      display: 'flex',
      alignItems: 'center',
      gap: '0.75rem',
      flexShrink: 0,
      position: 'relative',
    }}>
      {/* Record Button */}
      {!replaySession && (
        <button onClick={isRecording ? handleStopRecording : handleStartRecording} style={{ ...iconBtn, color: isRecording ? '#ff5252' : 'rgba(255,255,255,0.5)' }}>
          {isRecording ? <Square size={14} fill="#ff5252" /> : <Circle size={14} />}
        </button>
      )}

      {/* Sessions List Toggle */}
      <button onClick={() => { setShowSessions(!showSessions); refreshSessions(); }} style={{ ...iconBtn, color: 'rgba(255,255,255,0.5)' }}>
        <List size={14} />
      </button>

      {/* Replay Controls */}
      {replaySession && (
        <>
          <button onClick={() => setIsPlaying(!isPlaying)} style={{ ...iconBtn, color: '#7c4dff' }}>
            {isPlaying ? <Pause size={14} /> : <Play size={14} />}
          </button>

          {/* Speed Selector */}
          <select value={speed} onChange={(e) => setSpeed(parseFloat(e.target.value))} style={selectStyle}>
            <option value={0.25}>0.25×</option>
            <option value={0.5}>0.5×</option>
            <option value={1}>1×</option>
            <option value={2}>2×</option>
            <option value={4}>4×</option>
          </select>

          {/* Scrubber */}
          <input
            type="range"
            min={0}
            max={replaySession.frames.length - 1}
            value={replayIndex}
            onChange={(e) => {
              const idx = parseInt(e.target.value);
              setReplayIndex(idx);
              onReplayFrame(replaySession.frames[idx].data);
            }}
            style={{ flex: 1, accentColor: '#7c4dff' }}
          />

          <span style={{ fontSize: '0.65rem', fontFamily: 'var(--font-mono)', color: 'rgba(255,255,255,0.5)', whiteSpace: 'nowrap' }}>
            {replayIndex + 1}/{replaySession.frames.length}
          </span>

          <button onClick={handleExportCSV} style={{ ...iconBtn, color: 'rgba(255,255,255,0.5)' }}>
            <Download size={14} />
          </button>

          <button onClick={handleStopReplay} style={{ ...iconBtn, color: '#ff5252' }}>✕</button>
        </>
      )}

      {isRecording && (
        <span style={{ fontSize: '0.65rem', color: '#ff5252', fontWeight: 700, animation: 'blink 1s infinite' }}>
          ● REC ({recorder.getFrameCount()} frames)
        </span>
      )}

      {/* Sessions Dropdown */}
      {showSessions && (
        <div style={{
          position: 'absolute',
          bottom: '100%',
          left: '3rem',
          marginBottom: '0.5rem',
          backgroundColor: '#1c1e2e',
          border: '1px solid rgba(255,255,255,0.1)',
          borderRadius: '8px',
          padding: '0.5rem',
          minWidth: '220px',
          maxHeight: '200px',
          overflow: 'auto',
          zIndex: 100,
        }}>
          <div style={{ fontSize: '0.7rem', fontWeight: 700, color: 'rgba(255,255,255,0.5)', marginBottom: '0.4rem' }}>
            Saved Sessions
          </div>
          {sessions.length === 0 && (
            <div style={{ fontSize: '0.65rem', color: 'rgba(255,255,255,0.3)' }}>No sessions yet</div>
          )}
          {sessions.map(s => (
            <div key={s.name} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '0.25rem 0', borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
              <button onClick={() => handleLoadSession(s.name)} style={{ background: 'none', border: 'none', color: 'white', fontSize: '0.65rem', cursor: 'pointer', textAlign: 'left' }}>
                {s.name} ({s.frameCount}f)
              </button>
              <button onClick={() => handleDeleteSession(s.name)} style={{ ...iconBtn, color: '#ff5252' }}>
                <Trash2 size={12} />
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

const iconBtn: React.CSSProperties = {
  background: 'none',
  border: 'none',
  cursor: 'pointer',
  padding: '4px',
  display: 'flex',
  alignItems: 'center',
};

const selectStyle: React.CSSProperties = {
  backgroundColor: 'rgba(255,255,255,0.08)',
  border: '1px solid rgba(255,255,255,0.1)',
  color: 'white',
  fontSize: '0.65rem',
  padding: '0.15rem 0.3rem',
  borderRadius: '4px',
  outline: 'none',
};
