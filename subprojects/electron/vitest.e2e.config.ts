/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import ms from 'ms';
import { defineConfig } from 'vitest/config';

const timeout = ms('1m');

export default defineConfig({
  test: {
    environment: 'node',
    include: ['e2e/**/*.test.ts'],
    // Real JVM and Electron cold starts are much slower than the mocked unit suite.
    testTimeout: timeout,
    hookTimeout: timeout,
    fileParallelism: false,
    // Every test here is "slow" by the default reporter's 300ms threshold,
    // which shows per-test lines without their `describe` names (those are
    // only shown for single-file runs). `verbose` always prints the full
    // `describe > test` path instead.
    reporters: ['verbose', 'json'],
    outputFile: './build/e2e/results.json',
    env: {
      // An unpacked `electron-builder` output doesn't have `chrome-sandbox`
      // configured the way an installed package does, so Chromium's SUID
      // sandbox aborts on startup unless it's disabled here.
      REFINERY_NO_SANDBOX: '1',
    },
  },
});
