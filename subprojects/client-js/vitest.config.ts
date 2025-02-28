/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { coverageConfigDefaults, defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    coverage: {
      reporter: ['lcovonly'],
      provider: 'istanbul',
      reportsDirectory: './build/coverage',
      include: ['src'],
      exclude: [...coverageConfigDefaults.exclude, '**/__fixtures__/**'],
    },
  },
});
