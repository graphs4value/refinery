import {
  ChangeDesc,
  ChangeSet,
  Transaction,
} from '@codemirror/state';
import { nanoid } from 'nanoid';

import type { EditorStore } from '../editor/EditorStore';
import { getLogger } from '../logging';
import type { XtextWebSocketClient } from './XtextWebSocketClient';
import { PendingTask } from '../utils/PendingTask';
import { Timer } from '../utils/Timer';
import {
  IContentAssistEntry,
  isContentAssistResult,
  isDocumentStateResult,
  isInvalidStateIdConflictResult,
} from './xtextServiceResults';

const UPDATE_TIMEOUT_MS = 500;

const WAIT_FOR_UPDATE_TIMEOUT_MS = 1000;

const log = getLogger('xtext.UpdateService');

export interface IAbortSignal {
  aborted: boolean;
}

export class UpdateService {
  resourceName: string;

  xtextStateId: string | null = null;

  private store: EditorStore;

  private pendingUpdate: ChangeDesc | null = null;

  private dirtyChanges: ChangeDesc;

  private webSocketClient: XtextWebSocketClient;

  private updateListeners: PendingTask<void>[] = [];

  private idleUpdateTimer = new Timer(() => {
    this.handleIdleUpdate();
  }, UPDATE_TIMEOUT_MS);

  constructor(store: EditorStore, webSocketClient: XtextWebSocketClient) {
    this.resourceName = `${nanoid(7)}.problem`;
    this.store = store;
    this.dirtyChanges = this.newEmptyChangeDesc();
    this.webSocketClient = webSocketClient;
  }

  onConnect(): Promise<void> {
    this.xtextStateId = null;
    return this.updateFullText();
  }

  onTransaction(transaction: Transaction): void {
    const { changes } = transaction;
    if (!changes.empty) {
      this.dirtyChanges = this.dirtyChanges.composeDesc(changes.desc);
      this.idleUpdateTimer.reschedule();
    }
  }

  computeChangesSinceLastUpdate(): ChangeDesc {
    return this.pendingUpdate?.composeDesc(this.dirtyChanges) || this.dirtyChanges;
  }

  private handleIdleUpdate() {
    if (!this.webSocketClient.isOpen || this.dirtyChanges.empty) {
      return;
    }
    if (this.pendingUpdate === null) {
      this.update().catch((error) => {
        log.error('Unexpected error during scheduled update', error);
      });
    }
    this.idleUpdateTimer.reschedule();
  }

  private newEmptyChangeDesc() {
    const changeSet = ChangeSet.of([], this.store.state.doc.length);
    return changeSet.desc;
  }

  async updateFullText(): Promise<void> {
    await this.withUpdate(() => this.doUpdateFullText());
  }

  private async doUpdateFullText(): Promise<[string, void]> {
    const result = await this.webSocketClient.send({
      resource: this.resourceName,
      serviceType: 'update',
      fullText: this.store.state.doc.sliceString(0),
    });
    if (isDocumentStateResult(result)) {
      return [result.stateId, undefined];
    }
    log.error('Unexpected full text update result:', result);
    throw new Error('Full text update failed');
  }

  async update(): Promise<void> {
    await this.prepareForDeltaUpdate();
    const delta = this.computeDelta();
    if (delta === null) {
      return;
    }
    log.trace('Editor delta', delta);
    await this.withUpdate(async () => {
      const result = await this.webSocketClient.send({
        resource: this.resourceName,
        serviceType: 'update',
        requiredStateId: this.xtextStateId,
        ...delta,
      });
      if (isDocumentStateResult(result)) {
        return [result.stateId, undefined];
      }
      if (isInvalidStateIdConflictResult(result)) {
        return this.doFallbackToUpdateFullText();
      }
      log.error('Unexpected delta text update result:', result);
      throw new Error('Delta text update failed');
    });
  }

  private doFallbackToUpdateFullText() {
    if (this.pendingUpdate === null) {
      throw new Error('Only a pending update can be extended');
    }
    log.warn('Delta update failed, performing full text update');
    this.xtextStateId = null;
    this.pendingUpdate = this.pendingUpdate.composeDesc(this.dirtyChanges);
    this.dirtyChanges = this.newEmptyChangeDesc();
    return this.doUpdateFullText();
  }

  async fetchContentAssist(
    params: Record<string, unknown>,
    signal: IAbortSignal,
  ): Promise<IContentAssistEntry[]> {
    await this.prepareForDeltaUpdate();
    if (signal.aborted) {
      return [];
    }
    const delta = this.computeDelta();
    if (delta === null) {
      // Poscondition of `prepareForDeltaUpdate`: `xtextStateId !== null`
      return this.doFetchContentAssist(params, this.xtextStateId as string);
    }
    log.trace('Editor delta', delta);
    return this.withUpdate(async () => {
      const result = await this.webSocketClient.send({
        ...params,
        requiredStateId: this.xtextStateId,
        ...delta,
      });
      if (isContentAssistResult(result)) {
        return [result.stateId, result.entries];
      }
      if (isInvalidStateIdConflictResult(result)) {
        const [newStateId] = await this.doFallbackToUpdateFullText();
        if (signal.aborted) {
          return [newStateId, []];
        }
        const entries = await this.doFetchContentAssist(params, newStateId);
        return [newStateId, entries];
      }
      log.error('Unextpected content assist result with delta update', result);
      throw new Error('Unexpexted content assist result with delta update');
    });
  }

  private async doFetchContentAssist(params: Record<string, unknown>, expectedStateId: string) {
    const result = await this.webSocketClient.send({
      ...params,
      requiredStateId: expectedStateId,
    });
    if (isContentAssistResult(result) && result.stateId === expectedStateId) {
      return result.entries;
    }
    log.error('Unexpected content assist result', result);
    throw new Error('Unexpected content assist result');
  }

  private computeDelta() {
    if (this.dirtyChanges.empty) {
      return null;
    }
    let minFromA = Number.MAX_SAFE_INTEGER;
    let maxToA = 0;
    let minFromB = Number.MAX_SAFE_INTEGER;
    let maxToB = 0;
    this.dirtyChanges.iterChangedRanges((fromA, toA, fromB, toB) => {
      minFromA = Math.min(minFromA, fromA);
      maxToA = Math.max(maxToA, toA);
      minFromB = Math.min(minFromB, fromB);
      maxToB = Math.max(maxToB, toB);
    });
    return {
      deltaOffset: minFromA,
      deltaReplaceLength: maxToA - minFromA,
      deltaText: this.store.state.doc.sliceString(minFromB, maxToB),
    };
  }

  private async withUpdate<T>(callback: () => Promise<[string, T]>): Promise<T> {
    if (this.pendingUpdate !== null) {
      throw new Error('Another update is pending, will not perform update');
    }
    this.pendingUpdate = this.dirtyChanges;
    this.dirtyChanges = this.newEmptyChangeDesc();
    let newStateId: string | null = null;
    try {
      let result: T;
      [newStateId, result] = await callback();
      this.xtextStateId = newStateId;
      this.pendingUpdate = null;
      // Copy `updateListeners` so that we don't get into a race condition
      // if one of the listeners adds another listener.
      const listeners = this.updateListeners;
      this.updateListeners = [];
      listeners.forEach((listener) => {
        listener.resolve();
      });
      return result;
    } catch (e) {
      log.error('Error while update', e);
      if (this.pendingUpdate === null) {
        log.error('pendingUpdate was cleared during update');
      } else {
        this.dirtyChanges = this.pendingUpdate.composeDesc(this.dirtyChanges);
      }
      this.pendingUpdate = null;
      this.webSocketClient.forceReconnectOnError();
      const listeners = this.updateListeners;
      this.updateListeners = [];
      listeners.forEach((listener) => {
        listener.reject(e);
      });
      throw e;
    }
  }

  private async prepareForDeltaUpdate() {
    if (this.pendingUpdate === null) {
      if (this.xtextStateId === null) {
        return;
      }
      await this.updateFullText();
    }
    let nowMs = Date.now();
    const endMs = nowMs + WAIT_FOR_UPDATE_TIMEOUT_MS;
    while (this.pendingUpdate !== null && nowMs < endMs) {
      const timeoutMs = endMs - nowMs;
      const promise = new Promise((resolve, reject) => {
        const task = new PendingTask(resolve, reject, timeoutMs);
        this.updateListeners.push(task);
      });
      // We must keep waiting uptil the update has completed,
      // so the tasks can't be started in parallel.
      // eslint-disable-next-line no-await-in-loop
      await promise;
      nowMs = Date.now();
    }
    if (this.pendingUpdate !== null || this.xtextStateId === null) {
      log.error('No successful update in', WAIT_FOR_UPDATE_TIMEOUT_MS, 'ms');
      throw new Error('Failed to wait for successful update');
    }
  }
}
