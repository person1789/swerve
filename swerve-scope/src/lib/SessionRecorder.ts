/**
 * Records telemetry frames to IndexedDB for later replay.
 */

export interface TelemetryFrame {
  timestampMs: number;
  data: Record<string, unknown>;
}

export interface Session {
  name: string;
  date: number;
  frames: TelemetryFrame[];
}

const DB_NAME = 'SwerveScope';
const STORE_NAME = 'sessions';
const DB_VERSION = 1;

function openDB(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, DB_VERSION);
    req.onupgradeneeded = () => {
      const db = req.result;
      if (!db.objectStoreNames.contains(STORE_NAME)) {
        db.createObjectStore(STORE_NAME, { keyPath: 'name' });
      }
    };
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error);
  });
}

export class SessionRecorder {
  private frames: TelemetryFrame[] = [];
  private recording = false;
  private startTime = 0;

  isRecording() { return this.recording; }
  getFrameCount() { return this.frames.length; }

  startRecording() {
    this.frames = [];
    this.startTime = Date.now();
    this.recording = true;
  }

  addFrame(data: Record<string, unknown>) {
    if (!this.recording) return;
    this.frames.push({
      timestampMs: Date.now() - this.startTime,
      data: structuredClone(data),
    });
  }

  async stopAndSave(name: string): Promise<void> {
    this.recording = false;
    const db = await openDB();
    const tx = db.transaction(STORE_NAME, 'readwrite');
    const session: Session = { name, date: Date.now(), frames: [...this.frames] };
    tx.objectStore(STORE_NAME).put(session);
    return new Promise((resolve, reject) => {
      tx.oncomplete = () => resolve();
      tx.onerror = () => reject(tx.error);
    });
  }

  static async listSessions(): Promise<{ name: string; date: number; frameCount: number }[]> {
    const db = await openDB();
    const tx = db.transaction(STORE_NAME, 'readonly');
    const store = tx.objectStore(STORE_NAME);
    const req = store.getAll();
    return new Promise((resolve, reject) => {
      req.onsuccess = () => {
        const sessions = (req.result as Session[]).map(s => ({
          name: s.name,
          date: s.date,
          frameCount: s.frames.length,
        }));
        resolve(sessions.sort((a, b) => b.date - a.date));
      };
      req.onerror = () => reject(req.error);
    });
  }

  static async loadSession(name: string): Promise<Session | null> {
    const db = await openDB();
    const tx = db.transaction(STORE_NAME, 'readonly');
    const req = tx.objectStore(STORE_NAME).get(name);
    return new Promise((resolve, reject) => {
      req.onsuccess = () => resolve(req.result as Session || null);
      req.onerror = () => reject(req.error);
    });
  }

  static async deleteSession(name: string): Promise<void> {
    const db = await openDB();
    const tx = db.transaction(STORE_NAME, 'readwrite');
    tx.objectStore(STORE_NAME).delete(name);
    return new Promise((resolve, reject) => {
      tx.oncomplete = () => resolve();
      tx.onerror = () => reject(tx.error);
    });
  }

  static exportCSV(session: Session): string {
    if (session.frames.length === 0) return '';

    // Gather all keys across all frames
    const allKeys = new Set<string>();
    for (const frame of session.frames) {
      const flat = flattenObject(frame.data);
      for (const key of Object.keys(flat)) allKeys.add(key);
    }

    const keys = ['timestampMs', ...Array.from(allKeys).sort()];
    const header = keys.join(',');
    const rows = session.frames.map(frame => {
      const flat = flattenObject(frame.data);
      return keys.map(k => k === 'timestampMs' ? frame.timestampMs : (flat[k] ?? '')).join(',');
    });

    return [header, ...rows].join('\n');
  }
}

function flattenObject(obj: Record<string, unknown>, prefix = ''): Record<string, string> {
  const result: Record<string, string> = {};
  for (const [key, value] of Object.entries(obj)) {
    const fullKey = prefix ? `${prefix}.${key}` : key;
    if (value && typeof value === 'object' && !Array.isArray(value)) {
      Object.assign(result, flattenObject(value as Record<string, unknown>, fullKey));
    } else {
      result[fullKey] = String(value);
    }
  }
  return result;
}
