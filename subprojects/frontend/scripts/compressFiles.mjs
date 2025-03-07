/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { readFile, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { promisify } from 'node:util';
import zlib from 'node:zlib';

import fg from 'fast-glob';

const brotliCompress = promisify(zlib.brotliCompress);

const gzip = promisify(zlib.gzip);

export const minRatio = 0.8;

/** @type {import('node:zlib').ZlibOptions} */
export const gzipOptions = {
  level: 9,
};

/** @type {import('node:zlib').BrotliOptions} */
export const brotliOptions = {
  params: {
    [zlib.constants.BROTLI_PARAM_QUALITY]: 11,
  },
};

async function compressFiles() {
  const cwd = path.resolve(import.meta.dirname, '../build/vite/production');
  const inputFiles = await fg(
    '**/*.{css,html,js,license,mjs,svg,txt,webmanifest}',
    { cwd },
  );
  const promises = inputFiles.map(async (inputFile) => {
    const absoluteInputFile = path.resolve(cwd, inputFile);
    const contents = await readFile(absoluteInputFile);
    if (contents.length <= 1500) {
      // Don't compress files smaller than the TCP MTU.
    }
    const maxLength = Math.floor(contents.length * minRatio);
    await Promise.all([
      (async () => {
        const gzipContents = await gzip(contents, gzipOptions);
        if (gzipContents.length <= maxLength) {
          await writeFile(`${absoluteInputFile}.gz`, gzipContents);
        }
      })(),
      (async () => {
        const brotliContents = await brotliCompress(contents, brotliOptions);
        if (brotliContents.length <= maxLength) {
          await writeFile(`${absoluteInputFile}.br`, brotliContents);
        }
      })(),
    ]);
  });
  await Promise.all(promises);
}

await compressFiles();
