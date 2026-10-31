/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import type { BrowserWindow } from 'electron';

export type OpenRequest = { filePath: string } | { hash: string };

interface PendingOpenRequest {
  request: OpenRequest | undefined;
  replacesDefaultWindow: boolean;
}

export default class OpenRequestHandler {
  private readonly pendingRequests: PendingOpenRequest[] = [];

  private openImmediately:
    ((request: OpenRequest | undefined) => BrowserWindow) | undefined;

  /**
   * Opens an additional window without replacing the default startup window.
   * This ensures the first instance still gets a window if another instance
   * sends a request while it is starting.
   */
  open(request?: OpenRequest): BrowserWindow | undefined {
    if (this.openImmediately === undefined) {
      this.pendingRequests.push({
        request,
        replacesDefaultWindow: false,
      });
      return undefined;
    }
    return this.openImmediately(request);
  }

  /**
   * Opens a resource supplied during initial startup. It takes the place of
   * the default untitled window instead of creating an unnecessary extra.
   */
  openInitial(request: OpenRequest): BrowserWindow | undefined {
    if (this.openImmediately === undefined) {
      this.pendingRequests.push({ request, replacesDefaultWindow: true });
      return undefined;
    }
    return this.openImmediately(request);
  }

  initialize(
    openImmediately: (request: OpenRequest | undefined) => BrowserWindow,
  ): [BrowserWindow, ...BrowserWindow[]] {
    if (this.openImmediately !== undefined) {
      throw new Error('OpenRequestHandler is already initialized');
    }
    this.openImmediately = openImmediately;
    if (
      !this.pendingRequests.some(
        ({ replacesDefaultWindow }) => replacesDefaultWindow,
      )
    ) {
      this.pendingRequests.unshift({
        request: undefined,
        replacesDefaultWindow: true,
      });
    }
    const results = this.pendingRequests.map(({ request }) =>
      openImmediately(request),
    );
    this.pendingRequests.length = 0;
    return results as [BrowserWindow, ...BrowserWindow[]];
  }
}
