/*
 * Copyright 2021 TypeFox GmbH
 * Copyright 2025 The Refinery Authors <https://refinery.tools/>
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

const ctx = await esbuild.context({
  entryPoints: ['src/index.ts'],
  outdir: `build/esbuild/${mode}`,
  bundle: true,
  treeShaking: true,
  target: 'ES2022',
  format: 'esm',
  outExtension: {
    '.js': '.mjs',
  },
  banner: {
    // Workaround from https://github.com/evanw/esbuild/issues/1921#issuecomment-2302290651
    js: "import { createRequire } from 'module'; const require = createRequire(import.meta.url);",
  },
  define: {
    'process.env.MODE': modeString,
    'process.env.NODE_ENV': modeString,
  },
  loader: { '.ts': 'ts' },
  platform: 'node',
  sourcemap: !minify,
  minify,
  plugins: [
    {
      name: 'watch-plugin',
      setup(build) {
        build.onEnd((result) => {
          if (result.errors.length === 0) {
            const time = format(new Date(), `HH:mm:ss.sss`);
            console.log(`[${time}] ${mode} ${success}`);
          }
        });
      },
    },
  ],
});

if (watch) {
  await ctx.watch();
} else {
  await ctx.rebuild();
  await ctx.dispose();
}
