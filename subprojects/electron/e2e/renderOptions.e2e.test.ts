/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { mkdtemp, readFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import path from 'node:path';

import * as cheerio from 'cheerio';
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
import loadPDF from './loadPDF';
import pdfHasEmbeddedFont, {
  pdfDocumentHasEmbeddedFont,
} from './pdfHasEmbeddedFont';
import { renderPDFDocumentToPNG } from './renderPDFToPNG';
import runCLI, { getPackagedCLIPath } from './runCLI';

const inputPath = path.join(import.meta.dirname, 'fixtures', 'theme.problem');
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

function svgHasEmbeddedFont($: cheerio.CheerioAPI): boolean {
  return $('style').text().includes('@font-face');
}

/**
 * `addBackground` in the frontend's `exportDiagram.tsx` only ever prepends a
 * full-canvas `<rect>` as a direct child of the root `<svg>` when rendering
 * with a solid (non-transparent) background. Every other `<rect>` (node
 * backgrounds, headers, ...) lives nested inside a `<g>`.
 */
function svgHasSolidBackground($: cheerio.CheerioAPI): boolean {
  return $('svg > rect').length > 0;
}

/**
 * `-theme auto` embeds the theme CSS twice: once as plain rules (light) and
 * once prefixed with `[data-theme="dark"]`, so a page embedding the SVG can
 * toggle that attribute to switch palettes.
 */
function svgIsThemeSensitive($: cheerio.CheerioAPI): boolean {
  const style = $('style').text();
  const fills = [...style.matchAll(/\.node \.node-bg\{fill:([^;}]+);?\}/g)].map(
    ([, fill]) => fill,
  );
  return fills.length === 2 && fills[0] !== fills[1];
}

// SVG layout itself (text positions, node sizes, ...) isn't snapshotted here,
// since it's measured live by the rendering Chromium instance and drifts by
// a fraction of a point across machines/font-rasterizer versions.
const svgCases = [
  {
    name: 'theme-light',
    args: ['-theme', 'light'],
    transparent: true,
    embedFonts: false,
    themeSensitive: false,
  },
  {
    name: 'theme-dark',
    args: ['-theme', 'dark'],
    transparent: true,
    embedFonts: false,
    themeSensitive: false,
  },
  {
    name: 'theme-auto',
    args: ['-theme', 'auto'],
    transparent: true,
    embedFonts: false,
    themeSensitive: true,
  },
  {
    name: 'theme-light-solid',
    args: ['-theme', 'light', '-transparent', 'false'],
    transparent: false,
    embedFonts: false,
    themeSensitive: false,
  },
  {
    name: 'theme-dark-solid',
    args: ['-theme', 'dark', '-transparent', 'false'],
    transparent: false,
    embedFonts: false,
    themeSensitive: false,
  },
  {
    name: 'theme-light-embed-fonts',
    args: ['-theme', 'light', '-embed-fonts', 'true'],
    transparent: true,
    embedFonts: true,
    themeSensitive: false,
  },
  {
    name: 'theme-dark-embed-fonts',
    args: ['-theme', 'dark', '-embed-fonts', 'true'],
    transparent: true,
    embedFonts: true,
    themeSensitive: false,
  },
];

describe('svg render options', () => {
  test.for(svgCases)(
    'renders with $name',
    async ({ args, transparent, embedFonts, themeSensitive }, { signal }) => {
      const outputPath = path.join(tempDir, 'out.svg');
      const result = await runCLI(
        cliPath,
        ['render', inputPath, '-output', outputPath, ...args],
        signal,
      );
      expect(result.exitCode).toBe(0);
      const contents = await readFile(outputPath, 'utf-8');
      const $ = cheerio.load(contents, { xmlMode: true });
      expect(svgHasSolidBackground($)).toBe(!transparent);
      expect(svgHasEmbeddedFont($)).toBe(embedFonts);
      expect(svgIsThemeSensitive($)).toBe(themeSensitive);
    },
  );
});

const pngCases = [
  { snapshotName: 'theme-light', args: ['-theme', 'light'] },
  { snapshotName: 'theme-dark', args: ['-theme', 'dark'] },
  {
    snapshotName: 'theme-light-solid',
    args: ['-theme', 'light', '-transparent', 'false'],
  },
  {
    snapshotName: 'theme-dark-solid',
    args: ['-theme', 'dark', '-transparent', 'false'],
  },
  { snapshotName: 'theme-light-2x', args: ['-theme', 'light', '-scale', '2'] },
];

describe('png render options', () => {
  test.for(pngCases)(
    'renders with $snapshotName',
    async ({ snapshotName, args }, { signal }) => {
      const outputPath = path.join(tempDir, 'out.png');
      const result = await runCLI(
        cliPath,
        ['render', inputPath, '-output', outputPath, ...args],
        signal,
      );
      expect(result.exitCode).toBe(0);
      const contents = await readFile(outputPath);
      await expectPNGSnapshot(
        contents,
        path.join(snapshotsDir, `${snapshotName}.png`),
      );
    },
  );
});

// pdf.js always rasterizes onto an opaque canvas, so only the
// solid-background cases have a matching PNG snapshot.
const pdfCases = [
  {
    snapshotName: 'theme-light-solid',
    args: ['-theme', 'light', '-transparent', 'false'],
  },
  {
    snapshotName: 'theme-dark-solid',
    args: ['-theme', 'dark', '-transparent', 'false'],
  },
];

describe.skipIf(isUpdatingSnapshots())('pdf render options', () => {
  test.for(pdfCases)(
    'renders with $snapshotName',
    async ({ snapshotName, args }, { signal }) => {
      const outputPath = path.join(tempDir, 'out.pdf');
      const result = await runCLI(
        cliPath,
        [
          'render',
          inputPath,
          '-output',
          outputPath,
          '-embed-fonts',
          'true',
          ...args,
        ],
        signal,
      );
      expect(result.exitCode).toBe(0);
      const referencePng = await readFile(
        path.join(snapshotsDir, `${snapshotName}.png`),
      );
      const pdf = await readFile(outputPath);
      const document = await loadPDF(pdf);
      let rasterized;
      try {
        expect(await pdfDocumentHasEmbeddedFont(document)).toBe(true);
        const { width } = PNG.sync.read(referencePng);
        rasterized = await renderPDFDocumentToPNG(document, width);
      } finally {
        await document.destroy();
      }
      await comparePNG(rasterized, referencePng, {
        ...COMPARE_PDF_OPTIONS,
        label: `${snapshotName} (pdf vs png)`,
      });
    },
  );

  // Rendering without embedded fonts to a canvas would need to load system
  // fonts by hand, and wouldn't tell us anything more than this already
  // does, so we only check that no font got embedded here.
  test.for(['light', 'dark'])(
    'does not embed fonts with -theme %s -embed-fonts false',
    async (theme, { signal }) => {
      const outputPath = path.join(tempDir, 'out.pdf');
      const result = await runCLI(
        cliPath,
        [
          'render',
          inputPath,
          '-output',
          outputPath,
          '-theme',
          theme,
          '-embed-fonts',
          'false',
        ],
        signal,
      );
      expect(result.exitCode).toBe(0);
      const pdf = await readFile(outputPath);
      expect(await pdfHasEmbeddedFont(pdf)).toBe(false);
    },
  );
});

test('concretization uses the concrete model color palette', async ({
  signal,
}) => {
  const outputPath = path.join(tempDir, 'out.png');
  const result = await runCLI(
    cliPath,
    [
      'concretize',
      path.join(import.meta.dirname, 'fixtures', 'rendering', 'errors.problem'),
      '-output',
      outputPath,
      '-transparent',
      'false',
    ],
    signal,
  );
  expect(result.exitCode).toBe(0);
  const contents = await readFile(outputPath);
  await expectPNGSnapshot(
    contents,
    path.join(snapshotsDir, `errors-concretize.png`),
  );
});
