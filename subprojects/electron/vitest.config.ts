/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import path from 'node:path';

import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    environment: 'node',
    include: ['src/**/*.test.ts'],
    coverage: {
      reporter: ['lcovonly'],
      provider: 'istanbul',
      reportsDirectory: './build/coverage',
      include: ['src/**/*.ts'],
    },
  },
  resolve: {
    alias: {
      '@tools.refinery/frontend': path.resolve(
        import.meta.dirname,
        '../frontend/src',
      ),
    },
  },
});
