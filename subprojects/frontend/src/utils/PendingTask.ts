/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import TimeoutError from './TimeoutError';
import getLogger from './getLogger';

const log = getLogger('utils.PendingTask');

export default class PendingTask<T> {
  private readonly resolveCallback: (value: T) => void;

  private readonly rejectCallback: (reason?: unknown) => void;

  private resolved = false;

  private timeout: number | undefined;

  constructor(
    resolveCallback: (value: T) => void,
    rejectCallback: (reason?: unknown) => void,
    timeoutMs: number | undefined,
    timeoutCallback?: (() => void) | undefined,
  ) {
    this.resolveCallback = resolveCallback;
    this.rejectCallback = rejectCallback;
    this.timeout = setTimeout(() => {
      if (!this.resolved) {
        this.reject(new TimeoutError());
        timeoutCallback?.();
      }
    }, timeoutMs);
  }

  resolve(value: T): void {
    if (this.resolved) {
      log.warn('Trying to resolve already resolved promise');
      return;
    }
    this.markResolved();
    this.resolveCallback(value);
  }

  reject(reason?: unknown): void {
    if (this.resolved) {
      log.warn('Trying to reject already resolved promise');
      return;
    }
    this.markResolved();
    this.rejectCallback(reason);
  }

  private markResolved() {
    this.resolved = true;
    if (this.timeout !== undefined) {
      clearTimeout(this.timeout);
      this.timeout = undefined;
    }
  }
}
