import { readFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

import { lezer } from '@lezer/generator/rollup';
import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';
import injectPreload from 'vite-plugin-inject-preload';

const thisDir = path.dirname(fileURLToPath(import.meta.url));

const mode = process.env.MODE || 'development';
const isDevelopment = mode === 'development';

function portNumberOrElse(envName: string, fallback: number): number {
  const value = process.env[envName];
  return value ? parseInt(value, 10) : fallback;
}

const listenHost = process.env.LISTEN_HOST || 'localhost';
const listenPort = portNumberOrElse('LISTEN_PORT', 1313);
const apiHost = process.env.API_HOST || listenHost;
const apiPort = portNumberOrElse('API_PORT', 1312);
const apiSecure = apiPort === 443;
const publicHost = process.env.PUBLIC_HOST || listenHost;
const publicPort = portNumberOrElse('PUBLIC_PORT', listenPort);

const { name: packageName, version: packageVersion } = JSON.parse(
  readFileSync(path.join(thisDir, 'package.json'), 'utf8'),
) as { name: string; version: string };
process.env.VITE_PACKAGE_NAME ??= packageName;
process.env.VITE_PACKAGE_VERSIOn ??= packageVersion;

export default defineConfig({
  logLevel: 'info',
  mode,
  root: thisDir,
  cacheDir: path.join(thisDir, 'build/vite/cache'),
  plugins: [
    react({
      babel: {
        // Gets rid of deoptimization warnings for large chunks.
        // We don't need to minify here, because the output of Babel
        // will get passed to esbuild anyways.
        compact: false,
        minified: false,
      },
    }),
    injectPreload({
      files: [
        {
          match:
            /(?:jetbrains-mono-latin-variable-wghtOnly-(?:italic|normal)|roboto-latin-(?:400|500)-normal).+\.woff2/,
          attributes: {
            type: 'font/woff2',
            as: 'font',
            crossorigin: 'anonymous',
          },
        },
      ],
    }),
    lezer(),
  ],
  base: '',
  define: {
    __DEV__: JSON.stringify(isDevelopment), // For MobX
  },
  build: {
    assetsDir: '.',
    outDir: path.join('build/vite', mode),
    emptyOutDir: true,
    sourcemap: isDevelopment,
    minify: !isDevelopment,
  },
  server: {
    host: listenHost,
    port: listenPort,
    strictPort: true,
    proxy: {
      '/xtext-service': {
        target: `${apiSecure ? 'https' : 'http'}://${apiHost}:${apiPort}`,
        ws: true,
        secure: apiSecure,
      },
    },
    hmr: {
      host: publicHost,
      clientPort: publicPort,
      path: '/vite',
    },
  },
});
