import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

// Dev server proxies backend paths to the Wiki service on :8080.
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
    manifest: true, // design 10 S4.5: check-bundle-size.mjs reads dist/.vite/manifest.json
    rollupOptions: {
      output: {
        // design 10 S4.5 chunking: react is shared everywhere; editor (Milkdown+CodeMirror)
        // loads only on edit routes. antd is intentionally NOT forced into one chunk:
        // hoisting would drag Tree/Modal/Dropdown (space pages only) into the home bundle.
        // Rollup splits antd per usage graph automatically.
        manualChunks: id => {
          if (/@milkdown|@codemirror/.test(id)) return 'editor';
          if (/[\\/]node_modules[\\/](react|react-dom|react-router|scheduler)[\\/]/.test(id)) return 'vendor-react';
          return undefined;
        },
      },
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test-setup.ts'],
  },
});
