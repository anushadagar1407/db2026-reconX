import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';
import { normalizeBasePath } from './src/config/base-path.ts';

export default defineConfig({
  plugins: [react()],
  base: normalizeBasePath(process.env.PRESENTATION_BASE_PATH),
  server: {
    port: 4173,
    strictPort: true,
  },
  preview: {
    port: 4173,
    strictPort: true,
  },
  test: {
    environment: 'jsdom',
    restoreMocks: true,
    setupFiles: './src/test/setup.ts',
  },
});
