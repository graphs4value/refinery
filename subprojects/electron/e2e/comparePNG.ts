/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { mkdir, writeFile } from 'node:fs/promises';
import path from 'node:path';

import pixelmatch from 'pixelmatch';
import { PNG } from 'pngjs';
import { expect } from 'vitest';

export interface ComparePNGOptions {
  /** Fraction of pixels (0 to 1) allowed to differ before the comparison fails. */
  maxDiffRatio?: number;
  /**
   * Pixels of width/height slack to tolerate (and crop away before
   * diffing), for comparing outputs of independent rendering pipelines that
   * round page dimensions slightly differently.
   */
  maxSizeDiff?: number;
  /** Label used in assertion failure messages. */
  label?: string;
  /**
   * Subdirectory of `__diffs__` that diff artifacts are saved under.
   */
  diffGroup?: 'png' | 'pdf';
}

// Absorb per-platform text rendering differences only.
const DEFAULT_MAX_DIFF_RATIO = process.platform === 'win32' ? 0.015 : 0.005;
const DEFAULT_MAX_SIZE_DIFF = 0;
const DEFAULT_DIFF_GROUP = 'png';

// Absorb both per-platform text measurement differences and rounding
// differences between PNG and PDF rendering.
export const COMPARE_PDF_OPTIONS: ComparePNGOptions = {
  maxDiffRatio: 0.03,
  maxSizeDiff: 1,
  diffGroup: 'pdf',
};

const diffsDir = path.join(import.meta.dirname, '__diffs__');

function slugify(label: string): string {
  return path
    .basename(label)
    .replace(/\.png$/i, '')
    .replace(/[^a-z0-9.-]+/gi, '_');
}

async function saveDiffArtifacts(
  label: string,
  diffGroup: 'png' | 'pdf',
  actual: Buffer,
  expected: Buffer,
  diff: PNG,
): Promise<void> {
  if (process.env['CI'] !== 'true') {
    return;
  }
  const groupDir = path.join(diffsDir, diffGroup);
  await mkdir(groupDir, { recursive: true });
  const slug = slugify(label);
  await Promise.all([
    writeFile(path.join(groupDir, `${slug}.actual.png`), actual),
    writeFile(path.join(groupDir, `${slug}.expected.png`), expected),
    writeFile(path.join(groupDir, `${slug}.diff.png`), PNG.sync.write(diff)),
  ]);
}

function cropTopLeft(png: PNG, width: number, height: number): PNG {
  const cropped = new PNG({ width, height });
  PNG.bitblt(png, cropped, 0, 0, width, height, 0, 0);
  return cropped;
}

/**
 * Approximately compares two PNGs pixel by pixel, since rendering isn't
 * guaranteed to be byte-identical across platforms or rendering pipelines.
 */
export default async function comparePNG(
  actual: Buffer,
  expected: Buffer,
  {
    maxDiffRatio = DEFAULT_MAX_DIFF_RATIO,
    maxSizeDiff = DEFAULT_MAX_SIZE_DIFF,
    label = 'image',
    diffGroup = DEFAULT_DIFF_GROUP,
  }: ComparePNGOptions = {},
): Promise<void> {
  const actualPng = PNG.sync.read(actual);
  const expectedPng = PNG.sync.read(expected);
  const widthDiff = Math.abs(actualPng.width - expectedPng.width);
  const heightDiff = Math.abs(actualPng.height - expectedPng.height);
  const sizeMessage = `${label} has an unexpected size (actual: ${actualPng.width}x${actualPng.height}, expected: ${expectedPng.width}x${expectedPng.height})`;

  const width = Math.min(actualPng.width, expectedPng.width);
  const height = Math.min(actualPng.height, expectedPng.height);
  const croppedActual = cropTopLeft(actualPng, width, height);
  const croppedExpected = cropTopLeft(expectedPng, width, height);
  const diffPng = new PNG({ width, height });
  const diffPixels = pixelmatch(
    croppedActual.data,
    croppedExpected.data,
    diffPng.data,
    width,
    height,
    { threshold: 0.1 },
  );
  const diffRatio = diffPixels / (width * height);

  // Logged unconditionally (not just on failure) so CI logs can be used to
  // tune `maxDiffRatio`/`maxSizeDiff` from comparisons that currently pass.
  // eslint-disable-next-line no-console
  console.info(
    `[comparePNG] ${label}: ${diffPixels}/${width * height} px differ ` +
      `(${(diffRatio * 100).toFixed(2)}%, max ${(maxDiffRatio * 100).toFixed(2)}%); ` +
      `size diff ${widthDiff}x${heightDiff} (max ${maxSizeDiff})`,
  );

  // Saved (in CI) before asserting, so a failed assertion still leaves the
  // artifacts on disk for the upload step to pick up.
  await saveDiffArtifacts(label, diffGroup, actual, expected, diffPng);

  expect(widthDiff, sizeMessage).toBeLessThanOrEqual(maxSizeDiff);
  expect(heightDiff, sizeMessage).toBeLessThanOrEqual(maxSizeDiff);
  expect(
    diffRatio,
    `${label} differs by ${diffPixels} pixels (${(diffRatio * 100).toFixed(2)}%)`,
  ).toBeLessThanOrEqual(maxDiffRatio);
}
