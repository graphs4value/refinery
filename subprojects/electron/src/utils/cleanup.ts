/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import getLogger from '../logger/getLogger';

const logger = getLogger('utils.cleanup');

const cleanupFunctions: (() => void | Promise<void>)[] = [];

// Cleanup functions are awaited in turn (registration order reversed, i.e.
// the most recently registered one runs first) so that a callback which
// depends on another having fully finished.
export default async function cleanup(): Promise<void> {
  for (const cleanupFunction of cleanupFunctions) {
    try {
      await cleanupFunction();
    } catch (error) {
      logger.error({ err: error }, 'Error while cleaning up');
    }
  }
}

export function onCleanup(callback: () => void | Promise<void>): void {
  cleanupFunctions.unshift(callback);
}
