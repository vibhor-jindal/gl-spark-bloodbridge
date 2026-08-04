/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        ink: "#14213D",
        bg: "#F7F9FB",
        card: "#FFFFFF",
        border: "#E2E8ED",
        muted: "#6B7785",
        primary: {
          DEFAULT: "#0B6E4F",
          dark: "#08543C",
          light: "#E4F3ED"
        },
        urgent: {
          DEFAULT: "#D6224C",
          dark: "#A81939",
          light: "#FBE4E9"
        }
      },
      fontFamily: {
        display: ["Fraunces", "serif"],
        body: ["Inter", "sans-serif"],
        mono: ["IBM Plex Mono", "monospace"]
      }
    }
  },
  plugins: []
};
