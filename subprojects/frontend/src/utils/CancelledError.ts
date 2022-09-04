export default class CancelledError extends Error {
  constructor(message = 'Operation cancelled') {
    super(message);
  }
}
