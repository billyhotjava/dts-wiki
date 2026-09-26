import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

// Dev server proxies backend paths to the JHipster monolith on :8080
// (design 06 S2.1). Same-origin in prod: frontend is bundled into the jar.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
      '/oauth2': 'http://localhost:8080',
      '/login': 'http://localhost:8080',
      '/logout': 'http://localhost:8080',
      '/management': 'http://localhost:8080',
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test-setup.ts'],
  },
});
