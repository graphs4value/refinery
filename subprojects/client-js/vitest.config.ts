/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { playwright } from '@vitest/browser-playwright';
import { coverageConfigDefaults, defineConfig } from 'vitest/config';

// Only run Webkit tests in the CI environment or whe explicitly requested,
// because Playwright only supports specific environments that may be unavailable
// on a developer machine. See https://playwright.dev/docs/intro#system-requirements
const isCI = process.env['CI'] === 'true';

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
            instances: [
              { browser: 'chromium' },
              { browser: 'firefox' },
              ...(isCI ? [{ browser: 'webkit' as const }] : []),
            ],
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
