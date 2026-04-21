// useGamepad.ts — polls the Gamepad API and sends inputs to SITL
import { useEffect, useRef } from 'react';
import type { GamepadInput } from './useSITL';

export function useGamepad(
  sendInput: (input: GamepadInput) => void,
  enabled: boolean,
) {
  const rafRef = useRef<number>(0);
  const prevButtons = useRef<boolean[]>([]);

  useEffect(() => {
    if (!enabled) return;

    const poll = () => {
      const gamepads = navigator.getGamepads();
      const gp = gamepads[0] ?? gamepads[1];

      if (gp) {
        const deadband = (v: number) => Math.abs(v) < 0.07 ? 0 : v;
        sendInput({
          drive:      -deadband(gp.axes[1] ?? 0),
          strafe:     -deadband(gp.axes[0] ?? 0),
          turn:       -deadband(gp.axes[2] ?? gp.axes[3] ?? 0),
          dpad_up:    gp.buttons[12]?.pressed,
          dpad_down:  gp.buttons[13]?.pressed,
          dpad_left:  gp.buttons[14]?.pressed,
          dpad_right: gp.buttons[15]?.pressed,
          options:    gp.buttons[9]?.pressed,
        });
      }
      rafRef.current = requestAnimationFrame(poll);
    };

    rafRef.current = requestAnimationFrame(poll);
    return () => cancelAnimationFrame(rafRef.current);
  }, [sendInput, enabled]);
}
