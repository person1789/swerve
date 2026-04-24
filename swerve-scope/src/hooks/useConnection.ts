import { useState, useEffect, useCallback, useRef } from 'react';

export type ConnectionMode = 'sim' | 'robot' | 'disconnected';

const ROBOT_IP = '192.168.43.1';
const SIM_IP = 'localhost';
const PORT = 8080;

interface ConnectionState {
  mode: ConnectionMode;
  ws: WebSocket | null;
  connected: boolean;
  reconnect: () => void;
  setManualMode: (mode: 'sim' | 'robot') => void;
}

async function detectMode(): Promise<'robot' | 'sim'> {
  try {
    const res = await fetch(`http://${ROBOT_IP}:${PORT}/api/status`, {
      signal: AbortSignal.timeout(1500),
    });
    if (res.ok) return 'robot';
  } catch { /* not on robot wifi */ }

  return 'sim';
}

export function useConnection(
  onMessage: (msg: Record<string, unknown>) => void
): ConnectionState {
  const [mode, setMode] = useState<ConnectionMode>(() => {
    return (localStorage.getItem('swervescope_connection_mode') as ConnectionMode) || 'disconnected';
  });
  const [ws, setWs] = useState<WebSocket | null>(null);
  const [connected, setConnected] = useState(false);

  const onMessageRef = useRef(onMessage);
  onMessageRef.current = onMessage;

  const manualMode = useRef<ConnectionMode>((() => {
    return (localStorage.getItem('swervescope_connection_mode') as ConnectionMode) || 'sim';
  })());
  
  const wsRef = useRef<WebSocket | null>(null);
  const reconnectTimer = useRef<number>(0);
  const isMounted = useRef(true);
  const isConnecting = useRef(false);

  const connect = useCallback(() => {
    if (isConnecting.current) return;

    // Clean up any existing connection first
    if (wsRef.current) {
      wsRef.current.onopen = null;
      wsRef.current.onclose = null;
      wsRef.current.onmessage = null;
      wsRef.current.onerror = null;
      if (wsRef.current.readyState === WebSocket.OPEN ||
          wsRef.current.readyState === WebSocket.CONNECTING) {
        wsRef.current.close();
      }
      wsRef.current = null;
    }

    const doConnect = async () => {
      if (!isMounted.current) return;
      isConnecting.current = true;

      try {
        let targetMode = manualMode.current;
        
        // Only run discovery if we haven't explicitly set a mode or we are in 'disconnected' state
        if (targetMode === 'disconnected') {
          targetMode = await detectMode();
        }

        if (!isMounted.current) return;

        const host = targetMode === 'robot' ? ROBOT_IP : SIM_IP;
        const socket = new WebSocket(`ws://${host}:${PORT}/ws`);
        wsRef.current = socket;

        socket.onopen = () => {
          if (!isMounted.current) return;
          isConnecting.current = false;
          setMode(targetMode);
          setConnected(true);
          setWs(socket);
          localStorage.setItem('swervescope_connection_mode', targetMode);
        };

        socket.onclose = (event) => {
          if (!isMounted.current) return;
          isConnecting.current = false;
          setConnected(false);
          setWs(null);
          wsRef.current = null;

          // If it was a clean close by unmount, don't reconnect
          if (event.wasClean && !isMounted.current) return;

          // Auto-reconnect after 3s
          clearTimeout(reconnectTimer.current);
          reconnectTimer.current = window.setTimeout(() => {
            if (isMounted.current) connect();
          }, 3000);
        };

        socket.onerror = () => {
          isConnecting.current = false;
        };

        socket.onmessage = (event) => {
          try {
            const msg = JSON.parse(event.data);
            onMessageRef.current(msg);
          } catch (e) {
            console.error('WS parse error', e);
          }
        };
      } catch (err) {
        isConnecting.current = false;
        console.error('Connection failed', err);
      }
    };

    doConnect();
  }, []);

  useEffect(() => {
    isMounted.current = true;
    connect();

    return () => {
      isMounted.current = false;
      clearTimeout(reconnectTimer.current);
      if (wsRef.current) {
        wsRef.current.onopen = null;
        wsRef.current.onclose = null;
        wsRef.current.onmessage = null;
        wsRef.current.onerror = null;
        wsRef.current.close();
        wsRef.current = null;
      }
    };
  }, [connect]);

  const setManualMode = useCallback((m: 'sim' | 'robot') => {
    manualMode.current = m;
    localStorage.setItem('swervescope_connection_mode', m);
    setMode(m); // Optimistic update
    clearTimeout(reconnectTimer.current);
    isConnecting.current = false;
    connect();
  }, [connect]);

  return { mode, ws, connected, reconnect: connect, setManualMode };
}
