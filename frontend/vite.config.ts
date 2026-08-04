import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      // Dev fallback: same-origin /api → Gateway (avoids CORS if VITE_API_BASE_URL is empty)
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true
      }
    }
  }
});
