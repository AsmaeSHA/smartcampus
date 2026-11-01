import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// En développement, les appels /api sont redirigés vers le backend Spring Boot.
const apiProxyTarget = process.env.VITE_API_PROXY_TARGET || 'http://localhost:8080';

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': apiProxyTarget,
    },
  },
});
