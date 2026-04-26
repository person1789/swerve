import React, { useState, useEffect, useRef } from 'react';
import { Circle, Square, Play, Pause, Download, Trash2, List, Upload, BookmarkPlus } from 'lucide-react';
import { SessionRecorder, type Session } from '../lib/SessionRecorder';

interface TimelineBarProps {
  recorder: SessionRecorder;
  telemetry: Record<string, unknown>;
  onReplayFrame: (data: Record<string, unknown> | null) => void;
}

interface SessionSummary {
  id: string;
  name: string;
  date: number;
  frameCount: number;
  source: 'sim' | 'robot' | 'replay';
  opMode?: string;
}

export function TimelineBar({ recorder, telemetry, onReplayFrame }: TimelineBarProps) {
  const [isRecording, setIsRecording] = useState(false);
  const [showSessions, setShowSessions] = useState(false);
  const [sessions, setSessions] = useState<SessionSummary[]>([]);
  const [replaySession, setReplaySession] = useState<Session | null>(null);
  const [replayIndex, setReplayIndex] = useState(0);
  const [isPlaying, setIsPlaying] = useState(false);
  const [speed, setSpeed] = useState(1);
  const [bookmarks, setBookmarks] = useState<number[]>([]);
  const importInputRef = useRef<HTMLInputElement>(null);
  const replayTimer = useRef<number>(0);

  useEffect(() => {
    if (isRecording && Object.keys(telemetry).length > 0) {
      recorder.addFrame(telemetry);
    }
  }, [telemetry, isRecording, recorder]);

  useEffect(() => {
    if (isPlaying && replaySession) {
      const interval = 20 / speed;
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
    recorder.startRecording('sim', String(telemetry.activeOpMode || ''), String((telemetry as Record<string, unknown>).routeName || ''));
    setIsRecording(true);
    setBookmarks([]);
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

  const handleLoadSession = async (id: string) => {
    const session = await SessionRecorder.loadSession(id);
    if (session) {
      setReplaySession(session);
      setReplayIndex(0);
      setIsPlaying(false);
      setShowSessions(false);
      setBookmarks([]);
      if (session.frames.length > 0) {
        onReplayFrame(session.frames[0].data);
      }
    }
  };

  const handleDeleteSession = async (id: string) => {
    await SessionRecorder.deleteSession(id);
    refreshSessions();
  };

  const handleExportCSV = () => {
    if (!replaySession) return;
    const csv = SessionRecorder.exportCSV(replaySession);
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${replaySession.manifest.name}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const handleExportBundle = () => {
    if (!replaySession) return;
    const blob = new Blob([SessionRecorder.exportBundle(replaySession)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${replaySession.manifest.name}.session.json`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const handleImportBundle = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;
    const text = await file.text();
    const parsed = JSON.parse(text) as Session;
    const sessionName = `${parsed.manifest.name}-imported`;
    const importedRecorder = new SessionRecorder();
    importedRecorder.startRecording('replay');
    parsed.frames.forEach(frame => importedRecorder.addFrame(frame.data));
    await importedRecorder.stopAndSave(sessionName, parsed.manifest.notes);
    await refreshSessions();
    event.target.value = '';
  };

  const handleStopReplay = () => {
    setReplaySession(null);
    setIsPlaying(false);
    setBookmarks([]);
    onReplayFrame(null);
  };

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
      {!replaySession && (
        <button onClick={isRecording ? handleStopRecording : handleStartRecording} style={{ ...iconBtn, color: isRecording ? '#ff5252' : 'rgba(255,255,255,0.5)' }}>
          {isRecording ? <Square size={14} fill="#ff5252" /> : <Circle size={14} />}
        </button>
      )}

      <button onClick={() => { setShowSessions(!showSessions); refreshSessions(); }} style={{ ...iconBtn, color: 'rgba(255,255,255,0.5)' }}>
        <List size={14} />
      </button>
      <button onClick={() => importInputRef.current?.click()} style={{ ...iconBtn, color: 'rgba(255,255,255,0.5)' }}>
        <Upload size={14} />
      </button>

      {replaySession && (
        <>
          <button onClick={() => setIsPlaying(!isPlaying)} style={{ ...iconBtn, color: '#7c4dff' }}>
            {isPlaying ? <Pause size={14} /> : <Play size={14} />}
          </button>

          <select value={speed} onChange={(e) => setSpeed(parseFloat(e.target.value))} style={selectStyle}>
            <option value={0.25}>0.25x</option>
            <option value={0.5}>0.5x</option>
            <option value={1}>1x</option>
            <option value={2}>2x</option>
            <option value={4}>4x</option>
          </select>

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

          <button onClick={() => setBookmarks(prev => [...prev, replayIndex].sort((a, b) => a - b))} style={{ ...iconBtn, color: '#ffd740' }}>
            <BookmarkPlus size={14} />
          </button>
          <button onClick={handleExportCSV} style={{ ...iconBtn, color: 'rgba(255,255,255,0.5)' }}>
            <Download size={14} />
          </button>
          <button onClick={handleExportBundle} style={{ ...iconBtn, color: '#8bf5ff' }}>
            <Upload size={14} />
          </button>
          <button onClick={handleStopReplay} style={{ ...iconBtn, color: '#ff5252' }}>×</button>
        </>
      )}

      {isRecording && (
        <span style={{ fontSize: '0.65rem', color: '#ff5252', fontWeight: 700, animation: 'blink 1s infinite' }}>
          ● REC ({recorder.getFrameCount()} frames)
        </span>
      )}

      {bookmarks.length > 0 && replaySession && (
        <div style={{ display: 'flex', gap: '0.25rem', flexWrap: 'wrap' }}>
          {bookmarks.map(mark => (
            <button
              key={mark}
              onClick={() => {
                setReplayIndex(mark);
                onReplayFrame(replaySession.frames[mark].data);
              }}
              style={{ ...miniBtn, color: mark === replayIndex ? '#ffd740' : 'var(--text-dim)' }}
            >
              {mark}
            </button>
          ))}
        </div>
      )}

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
          minWidth: '280px',
          maxHeight: '240px',
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
            <div key={s.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '0.4rem', padding: '0.25rem 0', borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
              <button onClick={() => handleLoadSession(s.id)} style={{ background: 'none', border: 'none', color: 'white', fontSize: '0.65rem', cursor: 'pointer', textAlign: 'left', flex: 1 }}>
                <div>{s.name}</div>
                <div style={{ color: 'var(--text-dim)', fontSize: '0.58rem' }}>{s.source} · {s.frameCount}f {s.opMode ? `· ${s.opMode}` : ''}</div>
              </button>
              <button onClick={() => handleDeleteSession(s.id)} style={{ ...iconBtn, color: '#ff5252' }}>
                <Trash2 size={12} />
              </button>
            </div>
          ))}
        </div>
      )}

      <input ref={importInputRef} type="file" accept=".json,.session.json" onChange={handleImportBundle} style={{ display: 'none' }} />
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

const miniBtn: React.CSSProperties = {
  background: 'rgba(255,255,255,0.05)',
  border: '1px solid rgba(255,255,255,0.08)',
  borderRadius: 4,
  fontSize: '0.6rem',
  padding: '0.1rem 0.35rem',
  cursor: 'pointer',
};
