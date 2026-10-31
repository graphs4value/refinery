/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import ms from 'ms';

import ServerManager from './ServerManager';

/** Timeout applied to a single health-check request. */
const HEALTH_FETCH_TIMEOUT = ms('1s');

export default abstract class HttpServerManager<
  SpawnOptions = undefined,
> extends ServerManager<SpawnOptions> {
  static readonly hostname = '127.0.0.1';

  constructor(
    name: string,
    public readonly port: number,
  ) {
    super(name);
  }

  get healthAddress(): string {
    return `http://${HttpServerManager.hostname}:${this.port}/health`;
  }

  protected override async checkHealthy(): Promise<boolean> {
    try {
      const response = await fetch(this.healthAddress, {
        signal: AbortSignal.timeout(HEALTH_FETCH_TIMEOUT),
      });
      return response.ok;
    } catch {
      // Connection refused, request timeout, etc.: the server is not up yet.
      return false;
    }
  }
}
