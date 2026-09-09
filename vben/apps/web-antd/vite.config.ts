import { defineConfig } from '@vben/vite-config';
import { loadEnv } from 'vite';

export default defineConfig(async ({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const apiTarget =
    env.SAFETEAM_API_TARGET ??
    process.env.SAFETEAM_API_TARGET ??
    'http://127.0.0.1:28080';
  return {
    application: {},
    vite: {
      server: {
        proxy: {
          '/api': {
            changeOrigin: true,
            // Spring Boot backend keeps the /api prefix.
            target: apiTarget,
            ws: true,
          },
          '/uploads': {
            changeOrigin: true,
            target: apiTarget,
          },
        },
      },
    },
  };
});
