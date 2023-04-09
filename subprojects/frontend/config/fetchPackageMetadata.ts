/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { readFile } from 'node:fs/promises';
import path from 'node:path';

import z from 'zod';

const PackageInfo = z.object({
  name: z.string().min(1),
  version: z.string().min(1),
});

export default async function fetchPackageMetadata(
  thisDir: string,
): Promise<void> {
  const contents = await readFile(path.join(thisDir, 'package.json'), 'utf8');
  const { name: packageName, version: packageVersion } = PackageInfo.parse(
    JSON.parse(contents),
  );
  process.env['VITE_PACKAGE_NAME'] ??= packageName;
  process.env['VITE_PACKAGE_VERSION'] ??= packageVersion;
}
