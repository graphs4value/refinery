/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { describe, expect, test } from 'vitest';

import getWindowSize from './windowSize';

describe('getWindowSize', () => {
  test('preserves a size that fits a display', () => {
    expect(
      getWindowSize({ width: 1024, height: 768, maximized: false }, [
        { width: 1920, height: 1080 },
      ]),
    ).toEqual({ width: 1024, height: 768 });
  });

  test('enforces the minimum window size', () => {
    expect(
      getWindowSize({ width: 100, height: 200, maximized: false }, [
        { width: 1920, height: 1080 },
      ]),
    ).toEqual({ width: 640, height: 480 });
  });

  test('clamps both dimensions to the display work area', () => {
    expect(
      getWindowSize({ width: 2000, height: 1600, maximized: false }, [
        { width: 1200, height: 900 },
      ]),
    ).toEqual({ width: 1200, height: 900 });
  });

  test('uses the display that preserves the largest window area', () => {
    expect(
      getWindowSize({ width: 3000, height: 1800, maximized: false }, [
        { width: 3440, height: 1440 },
        { width: 1920, height: 2160 },
      ]),
    ).toEqual({ width: 3000, height: 1440 });
  });

  test('falls back to the requested size without display information', () => {
    expect(
      getWindowSize({ width: 1200, height: 900, maximized: false }, []),
    ).toEqual({ width: 1200, height: 900 });
  });
});
