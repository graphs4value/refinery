/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import CancelledError from './CancelledError';
import PendingTask from './PendingTask';
import getLogger from './getLogger';

const log = getLogger('utils.PriorityMutex');

export default class PriorityMutex {
  private readonly lowPriorityQueue: PendingTask<void>[] = [];

  private readonly highPriorityQueue: PendingTask<void>[] = [];

  private _locked = false;

  constructor(private readonly timeout: number) {}

  get locked(): boolean {
    return this._locked;
  }

  async runExclusive<T>(
    callback: () => Promise<T>,
    highPriority = false,
  ): Promise<T> {
    await this.acquire(highPriority);
    try {
      return await callback();
    } finally {
      this.release();
    }
  }

  cancelAllWaiting(): void {
    [this.highPriorityQueue, this.lowPriorityQueue].forEach((queue) =>
      queue.forEach((task) => task.reject(new CancelledError())),
    );
  }

  private acquire(highPriority: boolean): Promise<void> {
    if (!this.locked) {
      this._locked = true;
      return Promise.resolve();
    }
    const queue = highPriority ? this.highPriorityQueue : this.lowPriorityQueue;
    return new Promise((resolve, reject) => {
      const task = new PendingTask(resolve, reject, this.timeout, () => {
        const index = queue.indexOf(task);
        if (index < 0) {
          log.error('Timed out task already removed from queue');
          return;
        }
        queue.splice(index, 1);
      });
      queue.push(task);
    });
  }

  private release(): void {
    if (!this.locked) {
      throw new Error('Trying to release already released mutext');
    }
    const task =
      this.highPriorityQueue.shift() ?? this.lowPriorityQueue.shift();
    if (task === undefined) {
      this._locked = false;
      return;
    }
    task.resolve();
  }
}
