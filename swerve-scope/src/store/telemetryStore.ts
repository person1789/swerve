import type { TelemetryFrame } from '../types/telemetry';

const MAX_FRAMES = 36_000;

class RingBuffer {
  private buf: TelemetryFrame[] = [];
  private _length = 0;

  push(frame: TelemetryFrame) {
    this.buf.push(frame);
    if (this.buf.length > MAX_FRAMES) this.buf.shift();
    this._length = this.buf.length;
  }

  get(idx: number): TelemetryFrame | null {
    return this.buf[idx] ?? null;
  }

  getLast(): TelemetryFrame | null {
    return this.buf[this.buf.length - 1] ?? null;
  }

  getAll(): TelemetryFrame[] {
    return this.buf;
  }

  get length() {
    return this._length;
  }

  clear() {
    this.buf = [];
    this._length = 0;
  }
}

export interface TelemetryStoreState {
  buffer: RingBuffer;
  scrubIndex: number;
  isLive: boolean;
  frame: TelemetryFrame | null;
  prevFrame: TelemetryFrame | null;
  trail: { x: number; y: number }[];
  length: number;
}

type Listener = () => void;

const frameListeners = new Set<Listener>();
const layoutListeners = new Set<Listener>();
const lengthListeners = new Set<Listener>();

const state: TelemetryStoreState = {
  buffer: new RingBuffer(),
  scrubIndex: 0,
  isLive: true,
  frame: null,
  prevFrame: null,
  trail: [],
  length: 0,
};

let lastLengthNotify = 0;
const LENGTH_NOTIFY_MS = 50;

function notifyFrame() {
  frameListeners.forEach(fn => fn());
}

function notifyLayout() {
  layoutListeners.forEach(fn => fn());
}

function notifyLength() {
  const now = performance.now();
  if (now - lastLengthNotify > LENGTH_NOTIFY_MS) {
    lastLengthNotify = now;
    lengthListeners.forEach(fn => fn());
  }
}

export const telemetryStore = {
  push(frame: TelemetryFrame) {
    state.prevFrame = state.frame;
    state.buffer.push(frame);
    state.length = state.buffer.length;

    if (state.isLive) {
      state.scrubIndex = state.buffer.length - 1;
      state.frame = frame;

      const last = state.trail[state.trail.length - 1];
      if (!last || Math.hypot(frame.x - last.x, frame.y - last.y) > 0.01) {
        state.trail.push({ x: frame.x, y: frame.y });
        if (state.trail.length > 2500) state.trail.shift();
      }

      notifyFrame();
    }
    notifyLength();
  },

  seek(idx: number) {
    const clamped = Math.max(0, Math.min(idx, state.buffer.length - 1));
    state.scrubIndex = clamped;
    state.isLive = false;
    state.prevFrame = state.buffer.get(clamped - 1);
    state.frame = state.buffer.get(clamped);
    notifyFrame();
    notifyLayout();
    notifyLength();
  },

  stepBy(delta: number) {
    telemetryStore.seek(state.scrubIndex + delta);
  },

  snapToLive() {
    state.isLive = true;
    state.scrubIndex = state.buffer.length - 1;
    state.prevFrame = state.buffer.get(state.scrubIndex - 1);
    state.frame = state.buffer.getLast();
    notifyFrame();
    notifyLayout();
  },

  clear() {
    state.buffer.clear();
    state.scrubIndex = 0;
    state.isLive = true;
    state.frame = null;
    state.prevFrame = null;
    state.trail = [];
    state.length = 0;
    notifyFrame();
    notifyLayout();
    notifyLength();
  },

  replaceHistory(frames: TelemetryFrame[], options?: { scrubIndex?: number; isLive?: boolean }) {
    state.buffer.clear();
    state.trail = [];

    for (const frame of frames) {
      state.buffer.push(frame);
      const last = state.trail[state.trail.length - 1];
      if (!last || Math.hypot(frame.x - last.x, frame.y - last.y) > 0.01) {
        state.trail.push({ x: frame.x, y: frame.y });
      }
    }

    state.length = state.buffer.length;
    const hasFrames = state.length > 0;
    const requestedLive = options?.isLive ?? true;
    state.isLive = hasFrames ? requestedLive : true;
    const requestedIndex = options?.scrubIndex ?? (hasFrames ? state.length - 1 : 0);
    state.scrubIndex = hasFrames ? Math.max(0, Math.min(requestedIndex, state.length - 1)) : 0;

    if (hasFrames) {
      if (state.isLive) state.scrubIndex = state.length - 1;
      state.frame = state.buffer.get(state.scrubIndex);
      state.prevFrame = state.buffer.get(state.scrubIndex - 1);
    } else {
      state.frame = null;
      state.prevFrame = null;
    }

    notifyFrame();
    notifyLayout();
    notifyLength();
  },

  subscribeToFrames(listener: Listener) {
    frameListeners.add(listener);
    return () => frameListeners.delete(listener);
  },

  subscribeToLayout(listener: Listener) {
    layoutListeners.add(listener);
    return () => layoutListeners.delete(listener);
  },

  subscribeToLength(listener: Listener) {
    lengthListeners.add(listener);
    return () => lengthListeners.delete(listener);
  },

  getSnapshot() {
    return state;
  },
  getFrame() {
    return state.frame;
  },
  getPrevFrame() {
    return state.prevFrame;
  },
  getBuffer() {
    return state.buffer.getAll();
  },
  getScrubIndex() {
    return state.scrubIndex;
  },
  getIsLive() {
    return state.isLive;
  },
  getLength() {
    return state.length;
  },
  getTrail() {
    return state.trail;
  },
};
