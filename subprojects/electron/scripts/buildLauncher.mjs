/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

/*
 * Cross-compiles `src/refinery-launcher.c` into `build/launcher/refinery.exe`
 * for Windows, using the Zig toolchain provisioned by `installZig.mjs`.
 */

import { mkdir, stat } from 'node:fs/promises';
import path from 'node:path';

import { run, zigExe } from './zig.mjs';

const root = path.join(import.meta.dirname, '..');
const srcFile = path.join(root, 'src', 'refinery-launcher.c');
const outDir = path.join(root, 'build', 'launcher');
const outFile = path.join(outDir, 'refinery.exe');

/** Windows target to build (matches the single `refinery.exe` embedded by electron-builder). */
const TARGET = 'x86_64-windows-gnu';

try {
  await stat(zigExe);
} catch {
  throw new Error(
    `Zig toolchain not found at ${zigExe}. Run \`yarn run zig:install\` ` +
      '(the Gradle `installZig` task) first.',
  );
}

await mkdir(outDir, { recursive: true });

console.log(`Compiling ${path.relative(root, outFile)} for ${TARGET}`);
await run(zigExe, [
  'cc',
  '-target',
  TARGET,
  '-O2',
  '-Wl,--subsystem,console',
  '-o',
  outFile,
  srcFile,
  '-lkernel32',
]);
