export default class Timer {
  private readonly callback: () => void;

  private readonly defaultTimeout: number;

  private timeout: number | undefined;

  constructor(callback: () => void, defaultTimeout = 0) {
    this.callback = () => {
      this.timeout = undefined;
      callback();
    };
    this.defaultTimeout = defaultTimeout;
  }

  schedule(timeout?: number | undefined): void {
    if (this.timeout === undefined) {
      this.timeout = setTimeout(this.callback, timeout ?? this.defaultTimeout);
    }
  }

  reschedule(timeout?: number | undefined): void {
    this.cancel();
    this.schedule(timeout);
  }

  cancel(): void {
    if (this.timeout !== undefined) {
      clearTimeout(this.timeout);
      this.timeout = undefined;
    }
  }
}
