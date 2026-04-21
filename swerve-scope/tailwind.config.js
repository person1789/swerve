/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      fontFamily: {
        mono: ['JetBrains Mono', 'Fira Code', 'Cascadia Code', 'monospace'],
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
      colors: {
        scope: {
          bg:      '#060a0f',
          surface: '#0d1520',
          border:  '#1a2535',
          muted:   '#2a3a52',
          text:    '#8ba4c0',
          bright:  '#c8dff0',
          accent:  '#0ea5e9',
          green:   '#10b981',
          red:     '#ef4444',
          amber:   '#f59e0b',
          purple:  '#a855f7',
        }
      },
      animation: {
        'pulse-slow': 'pulse 3s ease-in-out infinite',
        'scan': 'scan 2s linear infinite',
      },
      keyframes: {
        scan: {
          '0%': { transform: 'translateY(-100%)' },
          '100%': { transform: 'translateY(100vh)' },
        }
      }
    },
  },
  plugins: [],
}
