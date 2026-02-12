/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { playwright } from '@vitest/browser-playwright';
import { coverageConfigDefaults, defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    projects: [
      {
        extends: 'vite.config.ts',
        test: {
          name: 'node',
          include: ['src/**/*.test.ts'],
          environment: 'node',
          globalSetup: ['src/__fixtures__/mockServer.ts'],
        },
      },
      {
        extends: 'vite.config.ts',
        test: {
          name: 'browser',
          include: ['src/**/*.test.ts'],
          environment: 'node',
          globalSetup: ['src/__fixtures__/mockServer.ts'],
          // Firefox has a limit on HTTP 1.1 requests to the same origin,
          // so avoid any paralellism to prevent deadlocks due to our mock server.
          maxConcurrency: 1,
          maxWorkers: 1,
          fileParallelism: false,
          browser: {
            enabled: true,
            headless: true,
            provider: playwright(),
            instances: [{ browser: 'chromium' }, { browser: 'firefox' }],
          },
        },
      },
    ],
    coverage: {
      reporter: ['lcovonly'],
      provider: 'istanbul',
      reportsDirectory: './build/coverage',
      include: ['src'],
      exclude: [...coverageConfigDefaults.exclude, '**/__fixtures__/**'],
    },
  },
});
