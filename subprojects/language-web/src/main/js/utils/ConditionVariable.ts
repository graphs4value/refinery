import { getLogger } from './logger';
import { PendingTask } from './PendingTask';

const log = getLogger('utils.ConditionVariable');

export type Condition = () => boolean;

export class ConditionVariable {
  condition: Condition;

  defaultTimeout: number;

  listeners: PendingTask<void>[] = [];

  constructor(condition: Condition, defaultTimeout = 0) {
    this.condition = condition;
    this.defaultTimeout = defaultTimeout;
  }

  async waitFor(timeoutMs: number | null = null): Promise<void> {
    if (this.condition()) {
      return;
    }
    const timeoutOrDefault = timeoutMs || this.defaultTimeout;
    let nowMs = Date.now();
    const endMs = nowMs + timeoutOrDefault;
    while (!this.condition() && nowMs < endMs) {
      const remainingMs = endMs - nowMs;
      const promise = new Promise<void>((resolve, reject) => {
        if (this.condition()) {
          resolve();
          return;
        }
        const task = new PendingTask(resolve, reject, remainingMs);
        this.listeners.push(task);
      });
      // We must keep waiting until the update has completed,
      // so the tasks can't be started in parallel.
      // eslint-disable-next-line no-await-in-loop
      await promise;
      nowMs = Date.now();
    }
    if (!this.condition()) {
      log.error('Condition still does not hold after', timeoutOrDefault, 'ms');
      throw new Error('Failed to wait for condition');
    }
  }

  notifyAll(): void {
    this.clearListenersWith((listener) => listener.resolve());
  }

  rejectAll(error: unknown): void {
    this.clearListenersWith((listener) => listener.reject(error));
  }

  private clearListenersWith(callback: (listener: PendingTask<void>) => void) {
    // Copy `listeners` so that we don't get into a race condition
    // if one of the listeners adds another listener.
    const { listeners } = this;
    this.listeners = [];
    listeners.forEach(callback);
  }
}
