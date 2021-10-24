import { getLogger } from '../logging';

const REQUEST_TIMEOUT_MS = 1000;

const log = getLogger('PendingRequest');

export class PendingRequest {
  private readonly resolveCallback: (value: unknown) => void;

  private readonly rejectCallback: (reason?: unknown) => void;

  private resolved = false;

  private timeoutId: NodeJS.Timeout;

  constructor(resolve: (value: unknown) => void, reject: (reason?: unknown) => void) {
    this.resolveCallback = resolve;
    this.rejectCallback = reject;
    this.timeoutId = setTimeout(() => {
      if (!this.resolved) {
        this.reject(new Error('Request timed out'));
      }
    }, REQUEST_TIMEOUT_MS);
  }

  resolve(value: unknown): void {
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
    }
    this.markResolved();
    this.rejectCallback(reason);
  }

  private markResolved() {
    this.resolved = true;
    clearTimeout(this.timeoutId);
  }
}
