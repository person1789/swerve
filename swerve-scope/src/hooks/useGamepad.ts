import { useEffect, useRef, useState } from 'react';

export interface BrowserGamepadState {
  leftX: number;
  leftY: number;
  rightX: number;
  dpadUp: boolean;
  dpadRight: boolean;
  dpadDown: boolean;
  dpadLeft: boolean;
  resetHeading: boolean;
  connected: boolean;
}

export const DEFAULT_BINDINGS = {
  forward: 'w',
  backward: 's',
  left: 'a',
  right: 'd',
  turnLeft: 'j',
  turnRight: 'l',
  dpadUp: 'arrowup',
  dpadDown: 'arrowdown',
  dpadLeft: 'arrowleft',
  dpadRight: 'arrowright',
  reset: 'r'
};

export type KeyBindings = typeof DEFAULT_BINDINGS;

export function useGamepad(ws: WebSocket | null, connected: boolean) {
  const [bindings, setBindings] = useState<KeyBindings>(() => {
    const saved = localStorage.getItem('swerve_keybinds');
    return saved ? { ...DEFAULT_BINDINGS, ...JSON.parse(saved) } : DEFAULT_BINDINGS;
  });

  const keyboardState = useRef<Record<string, boolean>>({});
  const lastPostTime = useRef(0);

  const updateBindings = (newBindings: KeyBindings) => {
    setBindings(newBindings);
    localStorage.setItem('swerve_keybinds', JSON.stringify(newBindings));
  };

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      const key = e.key.toLowerCase();
      // Prevent default scrolling for arrow keys
      if (['arrowup', 'arrowdown', 'arrowleft', 'arrowright', ' '].includes(key)) {
        if (e.target === document.body) e.preventDefault();
      }
      keyboardState.current[key] = true;
    };

    const handleKeyUp = (e: KeyboardEvent) => {
      const key = e.key.toLowerCase();
      keyboardState.current[key] = false;
    };

    window.addEventListener('keydown', handleKeyDown);
    window.addEventListener('keyup', handleKeyUp);
    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      window.removeEventListener('keyup', handleKeyUp);
    };
  }, []);

  useEffect(() => {
    let animationFrameId: number;

    const clamp = (v: number) => Math.max(-1, Math.min(1, v));

    const pollGamepad = (timestamp: number) => {
      if (connected && ws && ws.readyState === WebSocket.OPEN) {
        if (timestamp - lastPostTime.current > 50) {
          lastPostTime.current = timestamp;
          
          let pad = null;
          if (navigator.getGamepads) {
            const pads = Array.from(navigator.getGamepads()).filter(Boolean);
            pad = pads.find(p => p && p.connected);
          }

          const k = keyboardState.current;
          
          let kLeftX = 0;
          let kLeftY = 0;
          let kRightX = 0;

          if (k[bindings.left]) kLeftX -= 1;
          if (k[bindings.right]) kLeftX += 1;
          if (k[bindings.forward]) kLeftY -= 1;
          if (k[bindings.backward]) kLeftY += 1;
          if (k[bindings.turnLeft]) kRightX -= 1;
          if (k[bindings.turnRight]) kRightX += 1;

          const payload: BrowserGamepadState = {
            leftX: pad ? clamp(pad.axes[0] || 0) : clamp(kLeftX),
            leftY: pad ? clamp(pad.axes[1] || 0) : clamp(kLeftY),
            rightX: pad ? clamp(pad.axes[2] ?? pad.axes[3] ?? 0) : clamp(kRightX),
            dpadUp: pad ? Boolean(pad.buttons[12]?.pressed) : Boolean(k[bindings.dpadUp]),
            dpadRight: pad ? Boolean(pad.buttons[15]?.pressed) : Boolean(k[bindings.dpadRight]),
            dpadDown: pad ? Boolean(pad.buttons[13]?.pressed) : Boolean(k[bindings.dpadDown]),
            dpadLeft: pad ? Boolean(pad.buttons[14]?.pressed) : Boolean(k[bindings.dpadLeft]),
            resetHeading: pad ? Boolean(pad.buttons[3]?.pressed) : Boolean(k[bindings.reset]),
            connected: Boolean(pad) || [kLeftX, kLeftY, kRightX].some(v => v !== 0) || 
                       [bindings.dpadUp, bindings.dpadDown, bindings.dpadLeft, bindings.dpadRight, bindings.reset].some(b => k[b])
          };

          ws.send(JSON.stringify({ type: 'gamepad', data: payload }));
        }
      }
      
      animationFrameId = requestAnimationFrame(pollGamepad);
    };

    animationFrameId = requestAnimationFrame(pollGamepad);
    return () => cancelAnimationFrame(animationFrameId);
  }, [ws, connected, bindings]);

  return { bindings, updateBindings };
}
