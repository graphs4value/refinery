export default class CancelledError extends Error {
  constructor() {
    super('Operation cancelled');
  }
}
