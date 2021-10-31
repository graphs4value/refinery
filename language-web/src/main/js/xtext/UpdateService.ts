import {
  ChangeDesc,
  ChangeSet,
  Transaction,
} from '@codemirror/state';
import { nanoid } from 'nanoid';

import type { EditorStore } from '../editor/EditorStore';
import type { XtextWebSocketClient } from './XtextWebSocketClient';
import { ConditionVariable } from '../utils/ConditionVariable';
import { getLogger } from '../utils/logger';
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

  private readonly store: EditorStore;

  /**
   * The changes being synchronized to the server if a full or delta text update is running,
   * `null` otherwise.
   */
  private pendingUpdate: ChangeDesc | null = null;

  /**
   * Local changes not yet sychronized to the server and not part of the running update, if any.
   */
  private dirtyChanges: ChangeDesc;

  private readonly webSocketClient: XtextWebSocketClient;

  private readonly updatedCondition = new ConditionVariable(
    () => this.pendingUpdate === null && this.xtextStateId !== null,
    WAIT_FOR_UPDATE_TIMEOUT_MS,
  );

  private readonly idleUpdateTimer = new Timer(() => {
    this.handleIdleUpdate();
  }, UPDATE_TIMEOUT_MS);

  constructor(store: EditorStore, webSocketClient: XtextWebSocketClient) {
    this.resourceName = `${nanoid(7)}.problem`;
    this.store = store;
    this.dirtyChanges = this.newEmptyChangeDesc();
    this.webSocketClient = webSocketClient;
  }

  onReconnect(): void {
    this.xtextStateId = null;
    this.updateFullText().catch((error) => {
      log.error('Unexpected error during initial update', error);
    });
  }

  onTransaction(transaction: Transaction): void {
    if (transaction.docChanged) {
      this.dirtyChanges = this.dirtyChanges.composeDesc(transaction.changes.desc);
      this.idleUpdateTimer.reschedule();
    }
  }

  /**
   * Computes the summary of any changes happened since the last complete update.
   *
   * The result reflects any changes that happened since the `xtextStateId`
   * version was uploaded to the server.
   *
   * @return the summary of changes since the last update
   */
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

  /**
   * Makes sure that the document state on the server reflects recent
   * local changes.
   *
   * Performs either an update with delta text or a full text update if needed.
   * If there are not local dirty changes, the promise resolves immediately.
   *
   * @return a promise resolving when the update is completed
   */
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
    if (delta !== null) {
      log.trace('Editor delta', delta);
      const entries = await this.withUpdate(async () => {
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
          // We must finish this state update transaction to prepare for any push events
          // before querying for content assist, so we just return `null` and will query
          // the content assist service later.
          return [newStateId, null];
        }
        log.error('Unextpected content assist result with delta update', result);
        throw new Error('Unexpexted content assist result with delta update');
      });
      if (entries !== null) {
        return entries;
      }
      if (signal.aborted) {
        return [];
      }
    }
    // Poscondition of `prepareForDeltaUpdate`: `xtextStateId !== null`
    return this.doFetchContentAssist(params, this.xtextStateId as string);
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

  /**
   * Executes an asynchronous callback that updates the state on the server.
   *
   * Ensures that updates happen sequentially and manages `pendingUpdate`
   * and `dirtyChanges` to reflect changes being synchronized to the server
   * and not yet synchronized to the server, respectively.
   *
   * Optionally, `callback` may return a second value that is retured by this function.
   *
   * Once the remote procedure call to update the server state finishes
   * and returns the new `stateId`, `callback` must return _immediately_
   * to ensure that the local `stateId` is updated likewise to be able to handle
   * push messages referring to the new `stateId` from the server.
   * If additional work is needed to compute the second value in some cases,
   * use `T | null` instead of `T` as a return type and signal the need for additional
   * computations by returning `null`. Thus additional computations can be performed
   * outside of the critical section.
   *
   * @param callback the asynchronous callback that updates the server state
   * @return a promise resolving to the second value returned by `callback`
   */
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
      this.updatedCondition.notifyAll();
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
      this.updatedCondition.rejectAll(e);
      throw e;
    }
  }

  /**
   * Ensures that there is some state available on the server (`xtextStateId`)
   * and that there is not pending update.
   *
   * After this function resolves, a delta text update is possible.
   *
   * @return a promise resolving when there is a valid state id but no pending update
   */
  private async prepareForDeltaUpdate() {
    // If no update is pending, but the full text hasn't been uploaded to the server yet,
    // we must start a full text upload.
    if (this.pendingUpdate === null && this.xtextStateId === null) {
      await this.updateFullText();
    }
    await this.updatedCondition.waitFor();
  }
}
