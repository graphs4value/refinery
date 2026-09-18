/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { existsSync } from 'node:fs';
import path from 'node:path';

export const root = path.join(import.meta.dirname, '..');

// `electron-builder` only omits the arch suffix from the unpacked directory
// name when building for its default arch, which is `x64` unless a
// `<platform>.defaultArch` is configured (it isn't here, for any platform).
export function getArchSuffix(): string {
  return process.arch === 'x64' ? '' : `-${process.arch}`;
}

/**
 * Resolves `relativePath` (relative to the subproject root) into an absolute
 * path, failing fast with a clear message if it doesn't exist -- instead of
 * every test that needs it timing out trying to spawn a missing binary.
 */
export function resolvePackagedPath(
  relativePath: string,
  what: string,
): string {
  const fullPath = path.join(root, relativePath);
  if (!existsSync(fullPath)) {
    throw new Error(
      `${what} not found at ${fullPath}. Run \`yarn run build\` first.`,
    );
  }
  return fullPath;
}
