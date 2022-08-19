import { readFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

import { lezer } from '@lezer/generator/rollup';
import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';
import injectPreload from 'vite-plugin-inject-preload';
import { VitePWA } from 'vite-plugin-pwa';

const thisDir = path.dirname(fileURLToPath(import.meta.url));

const mode = process.env.MODE || 'development';
const isDevelopment = mode === 'development';
process.env.NODE_ENV ??= mode;

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
process.env.VITE_PACKAGE_VERSION ??= packageVersion;

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
            /(?:jetbrains-mono-latin-variable-wghtOnly-(?:italic|normal)|roboto-latin-(?:400|500)-normal).+\.woff2$/,
          attributes: {
            type: 'font/woff2',
            as: 'font',
            crossorigin: 'anonymous',
          },
        },
      ],
    }),
    lezer(),
    VitePWA({
      strategies: 'generateSW',
      registerType: 'prompt',
      injectRegister: null,
      devOptions: {
        enabled: true,
      },
      // Unregister service worker installed in production mode
      // if Vite is started in development mode on the same domain.
      selfDestroying: isDevelopment,
      workbox: {
        globPatterns: [
          '**/*.{css,html,js}',
          'roboto-latin-{300,400,500,700}-normal.*.woff2',
          'jetbrains-mono-latin-variable-wghtOnly-{normal,italic}.*.woff2',
        ],
        dontCacheBustURLsMatching: /\.(?:css|js|woff2?)$/,
        navigateFallbackDenylist: [/^\/xtext-service/],
        sourcemap: isDevelopment,
      },
      includeAssets: ['apple-touch-icon.png', 'favicon.svg', 'mask-icon.svg'],
      manifest: {
        lang: 'en-US',
        name: 'Refinery',
        short_name: 'Refinery',
        description:
          'An efficient graph sovler for generating well-formed models',
        theme_color: '#21252b',
        background_color: '#21252b',
        icons: [
          {
            src: 'icon-192x192.png',
            sizes: '192x192',
            type: 'image/png',
            purpose: 'any maskable',
          },
          {
            src: 'icon-512x512.png',
            sizes: '512x512',
            type: 'image/png',
            purpose: 'any maskable',
          },
          {
            src: 'icon-any.svg',
            sizes: 'any',
            type: 'image/svg+xml',
            purpose: 'any maskable',
          },
          {
            src: 'mask-icon.svg',
            sizes: 'any',
            type: 'image/svg+xml',
            purpose: 'monochrome',
          },
        ],
      },
    }),
  ],
  base: '',
  define: {
    __DEV__: JSON.stringify(isDevelopment), // For MobX
  },
  build: {
    assetsDir: '.',
    // If we don't control inlining manually,
    // web fonts will randomly get inlined into the CSS, degrading performance.
    assetsInlineLimit: 0,
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
