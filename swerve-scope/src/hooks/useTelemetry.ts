import { useSyncExternalStore } from 'react';
import { telemetryStore } from '../store/telemetryStore';

function subscribeToCurrentFrame(listener: () => void) {
  const unsubFrame = telemetryStore.subscribeToFrames(listener);
  const unsubLayout = telemetryStore.subscribeToLayout(listener);
  return () => {
    unsubFrame();
    unsubLayout();
  };
}

export function useLiveFrame() {
  return useSyncExternalStore(
    subscribeToCurrentFrame,
    telemetryStore.getFrame,
  );
}

export function usePrevFrame() {
  return useSyncExternalStore(
    subscribeToCurrentFrame,
    telemetryStore.getPrevFrame,
  );
}

export function useTrail() {
  return useSyncExternalStore(
    telemetryStore.subscribeToFrames,
    telemetryStore.getTrail,
  );
}

export function useHistoryLength() {
  return useSyncExternalStore(
    telemetryStore.subscribeToLength,
    telemetryStore.getLength,
  );
}

export function usePlaybackState() {
  const scrubIndex = useSyncExternalStore(
    telemetryStore.subscribeToLayout,
    telemetryStore.getScrubIndex,
  );
  const isLive = useSyncExternalStore(
    telemetryStore.subscribeToLayout,
    telemetryStore.getIsLive,
  );
  return { scrubIndex, isLive };
}

export function useHistoryBuffer() {
  const length = useSyncExternalStore(
    telemetryStore.subscribeToLength,
    telemetryStore.getLength,
  );
  return { buffer: telemetryStore.getBuffer(), length };
}
