/*
 * SPDX-FileCopyrightText: 2021-2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { defineWorkspace } from 'vitest/config';

export default defineWorkspace([
  {
    extends: 'vite.config.ts',
    test: {
      name: 'node',
      include: ['src/**/*.test.ts'],
      environment: 'node',
      globalSetup: ['src/test/mockServer.ts'],
    },
  },
  {
    extends: 'vite.config.ts',
    test: {
      name: 'browser',
      include: ['src/**/*.test.ts'],
      environment: 'node',
      globalSetup: ['src/test/mockServer.ts'],
      browser: {
        enabled: true,
        headless: true,
        provider: 'playwright',
        instances: [
          { browser: 'chromium' },
          { browser: 'firefox' },
          { browser: 'webkit' },
        ],
      },
    },
  },
]);
