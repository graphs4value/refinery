import { getLogger } from './logger';

const log = getLogger('utils.PendingTask');

export class PendingTask<T> {
  private readonly resolveCallback: (value: T) => void;

  private readonly rejectCallback: (reason?: unknown) => void;

  private resolved = false;

  private timeout: NodeJS.Timeout | null;

  constructor(
    resolveCallback: (value: T) => void,
    rejectCallback: (reason?: unknown) => void,
    timeoutMs?: number,
    timeoutCallback?: () => void,
  ) {
    this.resolveCallback = resolveCallback;
    this.rejectCallback = rejectCallback;
    if (timeoutMs) {
      this.timeout = setTimeout(() => {
        if (!this.resolved) {
          this.reject(new Error('Request timed out'));
          if (timeoutCallback) {
            timeoutCallback();
          }
        }
      }, timeoutMs);
    } else {
      this.timeout = null;
    }
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
    if (this.timeout !== null) {
      clearTimeout(this.timeout);
    }
  }
}
