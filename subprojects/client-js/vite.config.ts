/*
 * SPDX-FileCopyrightText: 2021-2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import path from 'node:path';
import { fileURLToPath } from 'node:url';

import { defineConfig } from 'vite';
import dts from 'vite-plugin-dts';

const thisDir = path.dirname(fileURLToPath(import.meta.url));

const mode = process.env['MODE'] ?? 'production';

const isProduction = mode === 'production';

const outDir = isProduction ? 'dist' : path.join('build/vite', mode);

process.env['NODE_ENV'] ??= mode;

export default defineConfig({
  logLevel: 'info',
  mode,
  root: thisDir,
  cacheDir: path.join(thisDir, 'build/vite/cache'),
  plugins: isProduction
    ? [
        dts({
          // We can't bundle our types due to https://github.com/qmhc/vite-plugin-dts/issues/321
          rollupTypes: false,
          include: ['src/**/*.ts'],
          beforeWriteFile(filePath, content) {
            // Strip the `src/` prefix from the file paths.
            return { filePath: filePath.replace(/(^|\/)src\//, '/'), content };
          },
        }),
      ]
    : [],
  resolve: {
    alias: {
      // Make sure Rollup treats our entry points as part of the project,
      // and doesn't inline them as exteral dependencies.
      '@tools.refinery/client': path.join(thisDir, 'src'),
    },
  },
  build: {
    lib: {
      entry: {
        index: 'src/index.ts',
      },
      formats: ['es', 'cjs'],
      fileName(format, entryName) {
        let suffix: string;
        switch (format) {
          case 'es':
            suffix = 'mjs';
            break;
          case 'cjs':
            suffix = 'cjs';
            break;
          default:
            suffix = `${format}.js`;
            break;
        }
        return `${entryName}.${suffix}`;
      },
    },
    outDir,
    emptyOutDir: true,
    sourcemap: true,
    minify: mode !== 'development',
    rollupOptions: {
      // All dependecies should be listed here to prevent Rollup from bundling them.
      external: ['whatwg-fetch', 'zod'],
    },
  },
});
