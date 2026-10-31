/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { readdirSync } from 'node:fs';
import { mkdtemp, readFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import path from 'node:path';

import { PNG } from 'pngjs';
import {
  afterEach,
  beforeAll,
  beforeEach,
  describe,
  expect,
  test,
} from 'vitest';

import comparePNG, { COMPARE_PDF_OPTIONS } from './comparePNG';
import expectPNGSnapshot from './expectPNGSnapshot';
import isUpdatingSnapshots from './isUpdatingSnapshots';
import renderPDFToPNG from './renderPDFToPNG';
import runCLI, { getPackagedCLIPath } from './runCLI';

const renderingFixturesDir = path.join(
  import.meta.dirname,
  'fixtures',
  'rendering',
);
const renderingFixtures = readdirSync(renderingFixturesDir)
  .filter((name) => name.endsWith('.problem'))
  .toSorted();
const snapshotsDir = path.join(import.meta.dirname, '__snapshots__');

let cliPath: string;

beforeAll(() => {
  cliPath = getPackagedCLIPath();
});

let tempDir: string;

beforeEach(async () => {
  tempDir = await mkdtemp(path.join(tmpdir(), 'refinery-e2e-'));
});

afterEach(async () => {
  await rm(tempDir, { recursive: true, force: true });
});

describe('png rendering', () => {
  test.for(renderingFixtures)('renders %s', async (fixtureName, { signal }) => {
    const inputPath = path.join(renderingFixturesDir, fixtureName);
    const outputPath = path.join(tempDir, 'out.png');
    const result = await runCLI(
      cliPath,
      ['render', inputPath, '-output', outputPath, '-transparent', 'false'],
      signal,
    );
    expect(result.exitCode).toBe(0);
    const contents = await readFile(outputPath);
    const snapshotName = fixtureName.replace(/\.problem$/, '.png');
    await expectPNGSnapshot(contents, path.join(snapshotsDir, snapshotName));
  });
});

describe.skipIf(isUpdatingSnapshots())('pdf rendering', () => {
  // Only the embedded-fonts case is covered: rendering without embedded
  // fonts to a canvas would need to load system fonts by hand, and
  // wouldn't tell us whether the font was actually omitted from the PDF
  // anyway (that's better checked by inspecting the PDF's own font
  // resources, not by rasterizing it).
  test.for(renderingFixtures)('renders %s', async (fixtureName, { signal }) => {
    const inputPath = path.join(renderingFixturesDir, fixtureName);
    const outputPath = path.join(tempDir, 'out.pdf');
    const result = await runCLI(
      cliPath,
      [
        'render',
        inputPath,
        '-output',
        outputPath,
        '-transparent',
        'false',
        '-embed-fonts',
        'true',
      ],
      signal,
    );
    expect(result.exitCode).toBe(0);
    const pdf = await readFile(outputPath);
    const pngSnapshotName = fixtureName.replace(/\.problem$/, '.png');
    const referencePng = await readFile(
      path.join(snapshotsDir, pngSnapshotName),
    );
    const { width } = PNG.sync.read(referencePng);
    const rasterized = await renderPDFToPNG(pdf, width);
    // The independent PDF and PNG rendering pipelines round fractional page
    // dimensions slightly differently, and pdf.js's own rasterizer produces
    // different anti-aliasing than the PNG export's, so this is a looser,
    // approximate comparison rather than an exact pixel match.
    await comparePNG(rasterized, referencePng, {
      ...COMPARE_PDF_OPTIONS,
      label: `${fixtureName} (pdf vs png)`,
    });
  });
});
