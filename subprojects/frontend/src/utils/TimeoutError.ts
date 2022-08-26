export default class TimeoutError extends Error {
  constructor() {
    super('Operation timed out');
  }
}
