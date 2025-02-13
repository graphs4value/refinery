/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { defineWorkspace } from 'vitest/config';

// Only run Webkit tests in the CI environment or whe explicitly requested,
// because Playwright only supports specific environments that may be unavaiable
// on a developer machine. See https://playwright.dev/docs/intro#system-requirements
const isCI = process.env['CI'] === 'true';

export default defineWorkspace([
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
      browser: {
        enabled: true,
        headless: true,
        provider: 'playwright',
        instances: [
          { browser: 'chromium' },
          { browser: 'firefox' },
          ...(isCI ? [{ browser: 'webkit' }] : []),
        ],
      },
    },
  },
]);
