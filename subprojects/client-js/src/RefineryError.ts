/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import type { RefineryResult } from './dto';

export function fromParsedResult(
  parsedResult: RefineryResult.Error,
  options?: ErrorOptions,
): Base {
  switch (parsedResult.result) {
    case 'timeout':
      return new Timeout(parsedResult, options);
    case 'cancelled':
      return new Cancelled(parsedResult, options);
    case 'requestError':
      return new RequestError(parsedResult, options);
    case 'serverError':
      return new ServerError(parsedResult, options);
    case 'invalidProblem':
      return new InvalidProblem(parsedResult, options);
    case 'unsatisfiable':
      return new Unsatisfiable(parsedResult, options);
  }
}

export abstract class Base<
  T extends RefineryResult.Error = RefineryResult.Error,
> extends Error {
  protected constructor(
    public readonly parsedResult: T,
    options?: ErrorOptions,
  ) {
    super(parsedResult.message, options);
    this.name = 'RefineryError';
  }

  get result(): T['result'] {
    return this.parsedResult.result;
  }
}

export class Timeout extends Base<RefineryResult.Timeout> {
  public constructor(
    parsedResult: RefineryResult.Timeout,
    options?: ErrorOptions,
  ) {
    super(parsedResult, options);
    this.name = 'RefineryError.Timeout';
  }
}

export class Cancelled extends Base<RefineryResult.Cancelled> {
  public constructor(
    parsedResult: RefineryResult.Cancelled,
    options?: ErrorOptions,
  ) {
    super(parsedResult, options);
    this.name = 'RefineryError.Cancelled';
  }
}

export class RequestError extends Base<RefineryResult.RequestError> {
  public constructor(
    parsedResult: RefineryResult.RequestError,
    options?: ErrorOptions,
  ) {
    super(parsedResult, options);
    this.name = 'RefineryError.RequestError';
  }

  get details(): RefineryResult.RequestError['details'] {
    return this.parsedResult.details;
  }
}

export class ServerError extends Base<RefineryResult.ServerError> {
  public constructor(
    parsedResult: RefineryResult.ServerError,
    options?: ErrorOptions,
  ) {
    super(parsedResult, options);
    this.name = 'RefineryError.ServerError';
  }
}

export class InvalidProblem extends Base<RefineryResult.InvalidProblem> {
  public constructor(
    parsedResult: RefineryResult.InvalidProblem,
    options?: ErrorOptions,
  ) {
    super(parsedResult, options);
    this.name = 'RefineryError.InvalidProblem';
  }

  get issues(): RefineryResult.InvalidProblem['issues'] {
    return this.parsedResult.issues;
  }
}

export class Unsatisfiable extends Base<RefineryResult.Unsatisfiable> {
  public constructor(
    parsedResult: RefineryResult.Unsatisfiable,
    options?: ErrorOptions,
  ) {
    super(parsedResult, options);
    this.name = 'RefineryError.Unsatisfiable';
  }
}
