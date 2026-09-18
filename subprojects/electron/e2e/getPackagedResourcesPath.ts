/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { getArchSuffix, resolvePackagedPath } from './packagedPaths';

/**
 * Path to the `electron-builder` unpacked `resources` directory (containing
 * `app.asar`, the bundled JRE, and the backend jars), relative to the
 * subproject root.
 */
function getRelativeResourcesPath(): string {
  const archSuffix = getArchSuffix();
  switch (process.platform) {
    case 'linux':
      return `build/dist/linux${archSuffix}-unpacked/resources`;
    case 'darwin':
      return `build/dist/mac${archSuffix}/Refinery.app/Contents/Resources`;
    case 'win32':
      return `build/dist/win${archSuffix}-unpacked/resources`;
    default:
      throw new Error(`Unsupported platform: ${process.platform}`);
  }
}

export default function getPackagedResourcesPath(): string {
  return resolvePackagedPath(
    getRelativeResourcesPath(),
    'Packaged resources directory',
  );
}
