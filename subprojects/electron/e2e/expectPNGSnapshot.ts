/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { mkdir, readFile, writeFile } from 'node:fs/promises';
import path from 'node:path';

import comparePNG, { type ComparePNGOptions } from './comparePNG';
import isUpdatingSnapshots from './isUpdatingSnapshots';

/**
 * Approximately compares `actual` against a PNG reference snapshot stored at
 * `snapshotPath`. Missing snapshots are created automatically, like
 * `toMatchFileSnapshot`.
 */
export default async function expectPNGSnapshot(
  actual: Buffer,
  snapshotPath: string,
  options: ComparePNGOptions = {},
): Promise<void> {
  let expected: Buffer | undefined;
  try {
    expected = await readFile(snapshotPath);
  } catch {
    expected = undefined;
  }

  if (isUpdatingSnapshots() || expected === undefined) {
    await mkdir(path.dirname(snapshotPath), { recursive: true });
    await writeFile(snapshotPath, actual);
    return;
  }

  await comparePNG(actual, expected, { label: snapshotPath, ...options });
}
