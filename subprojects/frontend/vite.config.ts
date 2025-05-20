/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import path from 'node:path';
import { fileURLToPath } from 'node:url';

import { lezer } from '@lezer/generator/rollup';
import react from '@vitejs/plugin-react-swc';
import { defineConfig, type UserConfig as ViteConfig } from 'vite';
import { VitePWA } from 'vite-plugin-pwa';

import { CONFIG_ENDPOINT } from './config/backendConfigVitePlugin';
import detectDevModeOptions, {
  XTEXT_ENDPOINT,
} from './config/detectDevModeOptions';
import fetchPackageMetadata from './config/fetchPackageMetadata';
import graphvizUMDVitePlugin from './config/graphvizUMDVitePlugin';
import manifest from './config/manifest';
import minifyHTMLVitePlugin from './config/minifyHTMLVitePlugin';
import preloadFontsVitePlugin from './config/preloadFontsVitePlugin';

const thisDir = path.dirname(fileURLToPath(import.meta.url));

const { mode, isDevelopment, devModePlugins, serverOptions } =
  detectDevModeOptions();

process.env['NODE_ENV'] ??= mode;

const fontsGlob = [
  'open-sans-latin-*.ttf',
  'open-sans-latin-400-{normal,italic}-*.woff2',
  'open-sans-latin-700-*.woff2',
  'open-sans-latin-wdth-{normal,italic}-*.woff2',
  'jetbrains-mono-latin-wght-{normal,italic}-*.woff2',
];

const viteConfig: ViteConfig = {
  logLevel: 'info',
  mode,
  root: thisDir,
  cacheDir: path.join(thisDir, 'build/vite/cache'),
  plugins: [
    react(),
    lezer(),
    preloadFontsVitePlugin(fontsGlob),
    minifyHTMLVitePlugin(),
    graphvizUMDVitePlugin(),
    VitePWA({
      strategies: 'generateSW',
      registerType: 'prompt',
      injectRegister: null,
      workbox: {
        globPatterns: ['**/*.{css,html,js}', ...fontsGlob],
        dontCacheBustURLsMatching: /\.(?:css|js|woff2?)$/,
        navigateFallbackDenylist: [new RegExp(`^\\/${XTEXT_ENDPOINT}$`)],
        runtimeCaching: [
          {
            urlPattern: CONFIG_ENDPOINT,
            handler: 'StaleWhileRevalidate',
          },
        ],
      },
      includeAssets: ['apple-touch-icon.png', 'favicon.svg'],
      manifest,
    }),
    devModePlugins,
  ],
  base: '',
  define: {
    __DEV__: JSON.stringify(isDevelopment), // For MobX
  },
  resolve: {
    alias: {
      '@tools.refinery/client/chat': path.join(
        thisDir,
        '../client-js/',
        isDevelopment ? 'src/chat/index.ts' : 'dist/chat.mjs',
      ),
      '@tools.refinery/client': path.join(
        thisDir,
        '../client-js/',
        isDevelopment ? 'src/index.ts' : 'dist/index.mjs',
      ),
    },
  },
  build: {
    assetsDir: '.',
    // If we don't control inlining manually, web fonts will be randomly inlined
    // into the CSS, which degrades performance.
    assetsInlineLimit: 0,
    outDir: path.join('build/vite', mode),
    emptyOutDir: true,
    sourcemap: isDevelopment,
    minify: !isDevelopment,
    rollupOptions: {
      output: {
        chunkFileNames: ({ isDynamicEntry, isEntry }) =>
          isDynamicEntry || isEntry ? '[name]-[hash].js' : '[hash].js',
        experimentalMinChunkSize: 20 * 1024,
      },
    },
  },
  server: serverOptions,
};

export default defineConfig(async () => {
  await fetchPackageMetadata(thisDir);
  return viteConfig;
});
