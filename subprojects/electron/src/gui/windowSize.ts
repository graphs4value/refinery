/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import type { Size } from 'electron';

import type { WindowState } from '../settings';

export const MIN_WINDOW_WIDTH = 640;

export const MIN_WINDOW_HEIGHT = 480;

export default function getWindowSize(
  windowState: WindowState,
  workAreaSizes: readonly Size[],
): Size {
  const requestedSize = {
    width: Math.max(MIN_WINDOW_WIDTH, windowState.width),
    height: Math.max(MIN_WINDOW_HEIGHT, windowState.height),
  };
  let bestSize = requestedSize;
  let bestArea = -1;
  for (const workAreaSize of workAreaSizes) {
    const candidate = {
      width: Math.max(
        MIN_WINDOW_WIDTH,
        Math.min(requestedSize.width, workAreaSize.width),
      ),
      height: Math.max(
        MIN_WINDOW_HEIGHT,
        Math.min(requestedSize.height, workAreaSize.height),
      ),
    };
    const area = candidate.width * candidate.height;
    if (area > bestArea) {
      bestSize = candidate;
      bestArea = area;
    }
  }
  return bestSize;
}
