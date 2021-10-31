export class Timer {
  readonly callback: () => void;

  readonly defaultTimeout: number;

  timeout: NodeJS.Timeout | null = null;

  constructor(callback: () => void, defaultTimeout = 0) {
    this.callback = () => {
      this.timeout = null;
      callback();
    };
    this.defaultTimeout = defaultTimeout;
  }

  schedule(timeout: number | null = null): void {
    if (this.timeout === null) {
      this.timeout = setTimeout(this.callback, timeout || this.defaultTimeout);
    }
  }

  reschedule(timeout: number | null = null): void {
    this.cancel();
    this.schedule(timeout);
  }

  cancel(): void {
    if (this.timeout !== null) {
      clearTimeout(this.timeout);
      this.timeout = null;
    }
  }
}
