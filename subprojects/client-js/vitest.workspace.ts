/*
 * SPDX-FileCopyrightText: 2021-2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { defineWorkspace } from 'vitest/config';
import { BrowserConfigOptions } from 'vitest/node';

export default defineWorkspace(
  (['node', 'chromium', 'firefox', 'webkit'] as const).map((name) => ({
    extends: 'vite.config.ts',
    test: {
      name,
      include: ['src/**/*.test.ts'],
      environment: 'node',
      globalSetup: ['src/test/mockServer.ts'],
      browser:
        name === 'node'
          ? ({ enabled: false } as BrowserConfigOptions)
          : {
              enabled: true,
              provider: 'playwright',
              name,
              headless: true,
            },
    },
  })),
);
