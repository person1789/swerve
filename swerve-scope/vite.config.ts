import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { spawn, execSync } from 'child_process'
import path from 'path'
import type { Plugin } from 'vite'

function autoBackend(): Plugin {
  let proc: ReturnType<typeof spawn> | null = null;
  const root = path.resolve(__dirname, '..');

  return {
    name: 'auto-backend',
    configureServer() {
      // Kill any stale Gradle daemons first
      try { execSync('.\\\\gradlew --stop', { cwd: root, stdio: 'ignore' }); } catch {}

      console.log('\\n🚀 Starting SwerveSimulator backend...');
      const isWin = process.platform === 'win32';
      const cmd = isWin ? '.\\\\gradlew.bat' : './gradlew';
      proc = spawn(cmd, [':TeamCode:runSwerveSimulator'], {
        cwd: root,
        stdio: ['ignore', 'pipe', 'pipe'],
        shell: true,
        detached: false,
      });

      proc.stdout?.on('data', (d: Buffer) => {
        const line = d.toString().trim();
        if (line) console.log(`[backend] ${line}`);
      });
      proc.stderr?.on('data', (d: Buffer) => {
        const line = d.toString().trim();
        if (line && !line.includes('% EXECUTING')) console.error(`[backend] ${line}`);
      });
      proc.on('exit', (code) => {
        if (code !== null && code !== 0) console.warn(`[backend] exited with code ${code}`);
      });
    },
    closeBundle() {
      if (proc) { proc.kill(); proc = null; }
    },
    buildEnd() {
      // Also handle Ctrl+C
    }
  };
}

// Kill backend on process exit
process.on('SIGINT', () => { process.exit(); });
process.on('SIGTERM', () => { process.exit(); });

export default defineConfig({
  plugins: [react(), autoBackend()],
})
