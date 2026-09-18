/*
 * Copyright 2021 TypeFox GmbH
 * Copyright 2025-2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: MIT AND EPL-2.0
 *
 * This file is based on
 * https://github.com/eclipse-langium/langium/blob/c3afb247d7dcfd41733c051b8db0395d50398b33/packages/generator-langium/templates/vscode/esbuild.mjs
 * It has been modified to support the Refinery project structure.
 */

import { format } from 'date-fns';
import * as esbuild from 'esbuild';

const mode = process.env['MODE'] ?? 'development';
const watch = process.argv.includes('--watch');
const minify = mode === 'production';
const modeString = JSON.stringify(mode);

const success = watch ? 'watch build succeeded' : 'build succeeded';

/**
 * Creates a new ESBuild context for the specify entry point.
 *
 * @param {string[]} entryPoints
 * @param {import('esbuild').Format} outputFormat
 * @param {string} extension
 * @param {import('esbuild').Platform} platform
 * @returns {Promise<import('esbuild').BuildContext>}
 */
function createContext(entryPoints, outputFormat, extension, platform) {
  return esbuild.context({
    entryPoints,
    outdir: `build/esbuild/${mode}`,
    bundle: true,
    treeShaking: true,
    target: 'ES2022',
    format: outputFormat,
    outExtension: {
      '.js': extension,
    },
    define: {
      'process.env.MODE': modeString,
      'process.env.NODE_ENV': modeString,
      'process.isDev': String(!minify),
    },
    loader: {
      '.ts': 'ts',
    },
    external: ['electron'],
    platform,
    sourcemap: !minify,
    minify,
    plugins: [
      {
        name: 'watch-plugin',
        setup(build) {
          build.onEnd((result) => {
            if (result.errors.length === 0) {
              const time = format(new Date(), `HH:mm:ss.sss`);
              console.log(
                `[${time}] ${entryPoints.join(', ')} ${mode} ${success}`,
              );
            }
          });
        },
      },
    ],
  });
}

const contexts = await Promise.all([
  createContext(['src/index.ts', 'src/cli/index.ts'], 'cjs', '.cjs', 'node'),
  createContext(
    ['src/gui/preload.ts', 'src/headless/preload.ts'],
    'iife',
    '.js',
    'browser',
  ),
]);

if (watch) {
  await Promise.all(contexts.map((ctx) => ctx.watch()));
} else {
  await Promise.all(
    contexts.map(async (ctx) => {
      await ctx.rebuild();
      await ctx.dispose();
    }),
  );
}
