/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        background: "#0a0a0a",
        foreground: "#ededed",
        primary: "#3b82f6",
        secondary: "#6366f1",
        accent: "#f43f5e",
      }
    },
  },
  plugins: [],
}
