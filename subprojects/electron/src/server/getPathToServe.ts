/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import path from 'node:path';

let bundleDir: string | undefined;
let baseResource: string | undefined;
if (process.isDev) {
  baseResource = process.env['REFINERY_BASE_RESOURCE'];
  if (baseResource) {
    bundleDir = path.resolve(baseResource);
  }
} else {
  bundleDir = path.resolve(__dirname, 'frontend');
}

export default function getPathToServe(pathname: string): string | undefined {
  if (!bundleDir) {
    return undefined;
  }
  const stripped = pathname.startsWith('/') ? pathname.substring(1) : pathname;
  const defaulted = stripped === '' ? 'index.html' : stripped;
  const pathToServe = path.resolve(bundleDir, defaulted);
  const relativePath = path.relative(bundleDir, pathToServe);
  if (!relativePath.startsWith('..') && !path.isAbsolute(relativePath)) {
    return pathToServe;
  }
  return undefined;
}
