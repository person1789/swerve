import {
  type SavedSession,
  type SessionManifest,
  type TelemetryFrame,
  type TransportSource,
  flattenTelemetry,
  normalizeTelemetryFrame,
} from './schemas';

export type Session = SavedSession;

const DB_NAME = 'SwerveScope';
const STORE_NAME = 'sessions';
const DB_VERSION = 2;

function openDB(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, DB_VERSION);
    req.onupgradeneeded = () => {
      const db = req.result;
      if (!db.objectStoreNames.contains(STORE_NAME)) {
        db.createObjectStore(STORE_NAME, { keyPath: 'manifest.id' });
      }
    };
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error);
  });
}

function buildSessionId(name: string) {
  const base = name.trim().toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '') || 'session';
  return `${base}-${Date.now()}`;
}

function normalizeSavedSession(raw: unknown): SavedSession | null {
  if (!raw || typeof raw !== 'object') {
    return null;
  }
  const candidate = raw as Partial<SavedSession> & { name?: string; date?: number; frames?: Array<{ timestampMs: number; data: Record<string, unknown> }> };
  if (candidate.manifest && Array.isArray(candidate.frames)) {
    return candidate as SavedSession;
  }
  if (!candidate.name || !candidate.date || !Array.isArray(candidate.frames)) {
    return null;
  }
  const manifest: SessionManifest = {
    schemaVersion: 2,
    id: buildSessionId(candidate.name),
    name: candidate.name,
    source: 'sim',
    startedAtMs: candidate.date,
    endedAtMs: candidate.date + (candidate.frames.at(-1)?.timestampMs ?? 0),
    frameCount: candidate.frames.length,
    tags: [],
  };
  return {
    manifest,
    frames: candidate.frames.map(frame => normalizeTelemetryFrame(frame.data, 'sim', frame.timestampMs)),
  };
}

export class SessionRecorder {
  private frames: TelemetryFrame[] = [];
  private recording = false;
  private startTime = 0;
  private source: TransportSource = 'sim';
  private routeName = '';
  private opMode = '';

  isRecording() { return this.recording; }
  getFrameCount() { return this.frames.length; }

  startRecording(source: TransportSource = 'sim', opMode = '', routeName = '') {
    this.frames = [];
    this.startTime = Date.now();
    this.source = source;
    this.opMode = opMode;
    this.routeName = routeName;
    this.recording = true;
  }

  addFrame(data: Record<string, unknown>) {
    if (!this.recording) return;
    this.frames.push(normalizeTelemetryFrame(structuredClone(data), this.source, Date.now() - this.startTime));
  }

  async stopAndSave(name: string, notes = ''): Promise<void> {
    this.recording = false;
    const manifest: SessionManifest = {
      schemaVersion: 2,
      id: buildSessionId(name),
      name,
      source: this.source,
      startedAtMs: this.startTime,
      endedAtMs: Date.now(),
      frameCount: this.frames.length,
      opMode: this.opMode || this.frames.at(-1)?.activeOpMode,
      routeName: this.routeName || undefined,
      notes: notes || undefined,
      tags: [],
    };
    const session: SavedSession = { manifest, frames: [...this.frames] };

    const db = await openDB();
    const tx = db.transaction(STORE_NAME, 'readwrite');
    tx.objectStore(STORE_NAME).put(session);
    return new Promise((resolve, reject) => {
      tx.oncomplete = () => resolve();
      tx.onerror = () => reject(tx.error);
    });
  }

  static async listSessions(): Promise<Array<{ id: string; name: string; date: number; frameCount: number; source: TransportSource; opMode?: string }>> {
    const db = await openDB();
    const tx = db.transaction(STORE_NAME, 'readonly');
    const store = tx.objectStore(STORE_NAME);
    const req = store.getAll();
    return new Promise((resolve, reject) => {
      req.onsuccess = () => {
        const sessions = (req.result as unknown[])
          .map(normalizeSavedSession)
          .filter((item): item is SavedSession => item !== null)
          .map(session => ({
            id: session.manifest.id,
            name: session.manifest.name,
            date: session.manifest.startedAtMs,
            frameCount: session.frames.length,
            source: session.manifest.source,
            opMode: session.manifest.opMode,
          }));
        resolve(sessions.sort((a, b) => b.date - a.date));
      };
      req.onerror = () => reject(req.error);
    });
  }

  static async loadSession(idOrName: string): Promise<Session | null> {
    const db = await openDB();
    const tx = db.transaction(STORE_NAME, 'readonly');
    const store = tx.objectStore(STORE_NAME);
    const req = store.getAll();
    return new Promise((resolve, reject) => {
      req.onsuccess = () => {
        const found = (req.result as unknown[])
          .map(normalizeSavedSession)
          .find(session => session && (session.manifest.id === idOrName || session.manifest.name === idOrName));
        resolve(found ?? null);
      };
      req.onerror = () => reject(req.error);
    });
  }

  static async deleteSession(idOrName: string): Promise<void> {
    const db = await openDB();
    const tx = db.transaction(STORE_NAME, 'readwrite');
    const store = tx.objectStore(STORE_NAME);
    const allReq = store.getAll();
    return new Promise((resolve, reject) => {
      allReq.onsuccess = () => {
        const match = (allReq.result as unknown[])
          .map(normalizeSavedSession)
          .find(session => session && (session.manifest.id === idOrName || session.manifest.name === idOrName));
        if (!match) {
          resolve();
          return;
        }
        store.delete(match.manifest.id);
        tx.oncomplete = () => resolve();
        tx.onerror = () => reject(tx.error);
      };
      allReq.onerror = () => reject(allReq.error);
    });
  }

  static exportCSV(session: Session): string {
    if (session.frames.length === 0) return '';

    const allKeys = new Set<string>();
    for (const frame of session.frames) {
      const flat = flattenTelemetry(frame.data);
      for (const key of Object.keys(flat)) allKeys.add(key);
    }

    const keys = ['timestampMs', 'source', ...Array.from(allKeys).sort()];
    const header = keys.join(',');
    const rows = session.frames.map(frame => {
      const flat = flattenTelemetry(frame.data);
      return keys.map(key => escapeCsvValue(
        key === 'timestampMs' ? String(frame.timestampMs)
          : key === 'source' ? frame.source
          : flat[key] ?? '',
      )).join(',');
    });

    return [header, ...rows].join('\n');
  }

  static exportBundle(session: Session): string {
    return JSON.stringify(session, null, 2);
  }
}

function escapeCsvValue(value: string) {
  if (/[,"\n]/.test(value)) {
    return `"${value.replace(/"/g, '""')}"`;
  }
  return value;
}
