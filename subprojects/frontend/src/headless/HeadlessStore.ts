/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { makeAutoObservable, runInAction } from 'mobx';

import type GraphStore from '../graph/GraphStore';
import type ExportSettingsStore from '../graph/export/ExportSettingsStore';

export class Request {
  pending = true;

  constructor(
    private readonly store: HeadlessStore,
    private readonly id: string,
    public readonly graph: GraphStore,
    public readonly settings: ExportSettingsStore,
    private readonly _resolve: (
      result: Uint8Array | PromiseLike<Uint8Array>,
    ) => void,
    private readonly _reject: (reason: unknown) => void,
  ) {
    makeAutoObservable(this);
  }

  resolve(result: Uint8Array | PromiseLike<Uint8Array>) {
    if (this.pending) {
      this.pending = false;
      this.store.deleteRequest(this.id);
      this._resolve(result);
    }
  }

  reject(reason: unknown) {
    if (this.pending) {
      this.pending = false;
      this.store.deleteRequest(this.id);
      this._reject(
        reason instanceof Error ? reason : new Error(String(reason)),
      );
    }
  }
}

export default class HeadlessStore {
  readonly pendingRequests = new Map<string, Request>();

  constructor() {
    makeAutoObservable(this, {
      exportGraph: false,
    });
  }

  exportGraph(
    id: string,
    graph: GraphStore,
    settings: ExportSettingsStore,
  ): Promise<Uint8Array> {
    return new Promise<Uint8Array>((resolve, reject) => {
      const request = new Request(this, id, graph, settings, resolve, reject);
      runInAction(() => this.pendingRequests.set(id, request));
    });
  }

  deleteRequest(id: string) {
    this.pendingRequests.delete(id);
  }
}
