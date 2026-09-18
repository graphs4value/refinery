/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { mkdtemp, readFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import path from 'node:path';

import {
  afterEach,
  beforeAll,
  beforeEach,
  describe,
  expect,
  test,
} from 'vitest';

import runCLI, { getPackagedCLIPath } from './runCLI';

const fixturesDir = path.join(import.meta.dirname, 'fixtures');
const minimalProblem = path.join(fixturesDir, 'minimal.problem');
const inconsistentProblem = path.join(fixturesDir, 'inconsistent.problem');

let cliPath: string;

beforeAll(() => {
  // Fails fast with a clear message if the app hasn't been packaged yet,
  // instead of every test timing out trying to spawn a missing binary.
  cliPath = getPackagedCLIPath();
});

let tempDir: string;

beforeEach(async () => {
  tempDir = await mkdtemp(path.join(tmpdir(), 'refinery-e2e-'));
});

afterEach(async () => {
  await rm(tempDir, { recursive: true, force: true });
});

describe('check', () => {
  test('exits successfully and reports consistency for a consistent model', async ({
    signal,
  }) => {
    const result = await runCLI(cliPath, ['check', minimalProblem], signal);
    expect(result.exitCode).toBe(0);
    expect(result.stdout).toContain('Model is consistent');
  });

  test('exits with a failure code and reports inconsistencies for an inconsistent model', async ({
    signal,
  }) => {
    const result = await runCLI(
      cliPath,
      ['check', inconsistentProblem],
      signal,
    );
    expect(result.exitCode).toBe(1);
    expect(result.stdout).toContain('Inconsistencies found in model');
  });

  test('reports a missing input file on standard error', async ({ signal }) => {
    const missingPath = path.join(tempDir, 'does-not-exist.problem');
    const result = await runCLI(cliPath, ['check', missingPath], signal);
    expect(result.exitCode).toBe(1);
    expect(result.stdout).toBe('');
    expect(result.stderr).not.toBe('');
  });
});

describe('semantics', () => {
  test('produces graphical output for a command other than render', async ({
    signal,
  }) => {
    const outputPath = path.join(tempDir, 'out.svg');
    const result = await runCLI(
      cliPath,
      ['semantics', minimalProblem, '-output', outputPath],
      signal,
    );
    expect(result.exitCode, result.stderr).toBe(0);
    const contents = await readFile(outputPath, 'utf-8');
    expect(contents).toMatch(/<svg[\s>]/i);
  });
});

describe('render', () => {
  test('produces an SVG file', async ({ signal }) => {
    const outputPath = path.join(tempDir, 'out.svg');
    const result = await runCLI(
      cliPath,
      ['render', minimalProblem, '-output', outputPath],
      signal,
    );
    expect(result.exitCode, result.stderr).toBe(0);
    const contents = await readFile(outputPath, 'utf-8');
    expect(contents).toMatch(/<svg[\s>]/i);
  });

  test('produces a PNG file', async ({ signal }) => {
    const outputPath = path.join(tempDir, 'out.png');
    const result = await runCLI(
      cliPath,
      ['render', minimalProblem, '-output', outputPath],
      signal,
    );
    expect(result.exitCode, result.stderr).toBe(0);
    const contents = await readFile(outputPath);
    expect(contents.subarray(0, 8)).toEqual(
      Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
    );
  });

  test('produces a PDF file', async ({ signal }) => {
    const outputPath = path.join(tempDir, 'out.pdf');
    const result = await runCLI(
      cliPath,
      ['render', minimalProblem, '-output', outputPath],
      signal,
    );
    expect(result.exitCode, result.stderr).toBe(0);
    const contents = await readFile(outputPath);
    expect(contents.subarray(0, 5).toString('ascii')).toBe('%PDF-');
  });
});
