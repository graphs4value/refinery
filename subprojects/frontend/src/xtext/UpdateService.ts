import {
  type ChangeDesc,
  ChangeSet,
  type ChangeSpec,
  StateEffect,
  type Transaction,
} from '@codemirror/state';
import { nanoid } from 'nanoid';

import type EditorStore from '../editor/EditorStore';
import ConditionVariable from '../utils/ConditionVariable';
import Timer from '../utils/Timer';
import getLogger from '../utils/getLogger';

import type XtextWebSocketClient from './XtextWebSocketClient';
import {
  type ContentAssistEntry,
  ContentAssistResult,
  DocumentStateResult,
  FormattingResult,
  isConflictResult,
} from './xtextServiceResults';

const UPDATE_TIMEOUT_MS = 500;

const WAIT_FOR_UPDATE_TIMEOUT_MS = 1000;

const log = getLogger('xtext.UpdateService');

/**
 * State effect used to override the dirty changes after a transaction.
 *
 * If this state effect is _not_ present in a transaction,
 * the transaction will be appended to the current dirty changes.
 *
 * If this state effect is present, the current dirty changes will be replaced
 * by the value of this effect.
 */
const setDirtyChanges = StateEffect.define<ChangeSet>();

export interface IAbortSignal {
  aborted: boolean;
}

interface StateUpdateResult<T> {
  newStateId: string;

  data: T;
}

interface Delta {
  deltaOffset: number;

  deltaReplaceLength: number;

  deltaText: string;
}

export default class UpdateService {
  resourceName: string;

  xtextStateId: string | undefined;

  private readonly store: EditorStore;

  /**
   * The changes being synchronized to the server if a full or delta text update is running,
   * `undefined` otherwise.
   */
  private pendingUpdate: ChangeSet | undefined;

  /**
   * Local changes not yet sychronized to the server and not part of the running update, if any.
   */
  private dirtyChanges: ChangeSet;

  private readonly webSocketClient: XtextWebSocketClient;

  private readonly updatedCondition = new ConditionVariable(
    () => this.pendingUpdate === undefined && this.xtextStateId !== undefined,
    WAIT_FOR_UPDATE_TIMEOUT_MS,
  );

  private readonly idleUpdateTimer = new Timer(() => {
    this.handleIdleUpdate();
  }, UPDATE_TIMEOUT_MS);

  constructor(store: EditorStore, webSocketClient: XtextWebSocketClient) {
    this.resourceName = `${nanoid(7)}.problem`;
    this.store = store;
    this.dirtyChanges = this.newEmptyChangeSet();
    this.webSocketClient = webSocketClient;
  }

  onReconnect(): void {
    this.xtextStateId = undefined;
    this.updateFullText().catch((error) => {
      log.error('Unexpected error during initial update', error);
    });
  }

  onTransaction(transaction: Transaction): void {
    const setDirtyChangesEffect = transaction.effects.find((effect) =>
      effect.is(setDirtyChanges),
    ) as StateEffect<ChangeSet> | undefined;
    if (setDirtyChangesEffect) {
      const { value } = setDirtyChangesEffect;
      if (this.pendingUpdate !== undefined) {
        this.pendingUpdate = ChangeSet.empty(value.length);
      }
      this.dirtyChanges = value;
      return;
    }
    if (transaction.docChanged) {
      this.dirtyChanges = this.dirtyChanges.compose(transaction.changes);
      this.idleUpdateTimer.reschedule();
    }
  }

  /**
   * Computes the summary of any changes happened since the last complete update.
   *
   * The result reflects any changes that happened since the `xtextStateId`
   * version was uploaded to the server.
   *
   * @returns the summary of changes since the last update
   */
  computeChangesSinceLastUpdate(): ChangeDesc {
    return (
      this.pendingUpdate?.composeDesc(this.dirtyChanges.desc) ??
      this.dirtyChanges.desc
    );
  }

  private handleIdleUpdate(): void {
    if (!this.webSocketClient.isOpen || this.dirtyChanges.empty) {
      return;
    }
    if (this.pendingUpdate === undefined) {
      this.update().catch((error) => {
        log.error('Unexpected error during scheduled update', error);
      });
    }
    this.idleUpdateTimer.reschedule();
  }

  private newEmptyChangeSet(): ChangeSet {
    return ChangeSet.of([], this.store.state.doc.length);
  }

  async updateFullText(): Promise<void> {
    await this.withUpdate(() => this.doUpdateFullText());
  }

  private async doUpdateFullText(): Promise<StateUpdateResult<void>> {
    const result = await this.webSocketClient.send({
      resource: this.resourceName,
      serviceType: 'update',
      fullText: this.store.state.doc.sliceString(0),
    });
    const { stateId } = DocumentStateResult.parse(result);
    return { newStateId: stateId, data: undefined };
  }

  /**
   * Makes sure that the document state on the server reflects recent
   * local changes.
   *
   * Performs either an update with delta text or a full text update if needed.
   * If there are not local dirty changes, the promise resolves immediately.
   *
   * @returns a promise resolving when the update is completed
   */
  async update(): Promise<void> {
    await this.prepareForDeltaUpdate();
    const delta = this.computeDelta();
    if (delta === undefined) {
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
      const parsedDocumentStateResult = DocumentStateResult.safeParse(result);
      if (parsedDocumentStateResult.success) {
        return {
          newStateId: parsedDocumentStateResult.data.stateId,
          data: undefined,
        };
      }
      if (isConflictResult(result, 'invalidStateId')) {
        return this.doFallbackToUpdateFullText();
      }
      throw parsedDocumentStateResult.error;
    });
  }

  private doFallbackToUpdateFullText(): Promise<StateUpdateResult<void>> {
    if (this.pendingUpdate === undefined) {
      throw new Error('Only a pending update can be extended');
    }
    log.warn('Delta update failed, performing full text update');
    this.xtextStateId = undefined;
    this.pendingUpdate = this.pendingUpdate.compose(this.dirtyChanges);
    this.dirtyChanges = this.newEmptyChangeSet();
    return this.doUpdateFullText();
  }

  async fetchContentAssist(
    params: Record<string, unknown>,
    signal: IAbortSignal,
  ): Promise<ContentAssistEntry[]> {
    await this.prepareForDeltaUpdate();
    if (signal.aborted) {
      return [];
    }
    const delta = this.computeDelta();
    if (delta !== undefined) {
      log.trace('Editor delta', delta);
      // Try to fetch while also performing a delta update.
      const fetchUpdateEntries = await this.withUpdate(() =>
        this.doFetchContentAssistWithDelta(params, delta),
      );
      if (fetchUpdateEntries !== undefined) {
        return fetchUpdateEntries;
      }
      if (signal.aborted) {
        return [];
      }
    }
    if (this.xtextStateId === undefined) {
      throw new Error('failed to obtain Xtext state id');
    }
    return this.doFetchContentAssistFetchOnly(params, this.xtextStateId);
  }

  private async doFetchContentAssistWithDelta(
    params: Record<string, unknown>,
    delta: Delta,
  ): Promise<StateUpdateResult<ContentAssistEntry[] | undefined>> {
    const fetchUpdateResult = await this.webSocketClient.send({
      ...params,
      requiredStateId: this.xtextStateId,
      ...delta,
    });
    const parsedContentAssistResult =
      ContentAssistResult.safeParse(fetchUpdateResult);
    if (parsedContentAssistResult.success) {
      const { stateId, entries: resultEntries } =
        parsedContentAssistResult.data;
      return { newStateId: stateId, data: resultEntries };
    }
    if (isConflictResult(fetchUpdateResult, 'invalidStateId')) {
      log.warn('Server state invalid during content assist');
      const { newStateId } = await this.doFallbackToUpdateFullText();
      // We must finish this state update transaction to prepare for any push events
      // before querying for content assist, so we just return `undefined` and will query
      // the content assist service later.
      return { newStateId, data: undefined };
    }
    throw parsedContentAssistResult.error;
  }

  private async doFetchContentAssistFetchOnly(
    params: Record<string, unknown>,
    requiredStateId: string,
  ): Promise<ContentAssistEntry[]> {
    // Fallback to fetching without a delta update.
    const fetchOnlyResult = await this.webSocketClient.send({
      ...params,
      requiredStateId: this.xtextStateId,
    });
    const { stateId, entries: fetchOnlyEntries } =
      ContentAssistResult.parse(fetchOnlyResult);
    if (stateId !== requiredStateId) {
      throw new Error(
        `Unexpected state id, expected: ${requiredStateId} got: ${stateId}`,
      );
    }
    return fetchOnlyEntries;
  }

  async formatText(): Promise<void> {
    await this.update();
    let { from, to } = this.store.state.selection.main;
    if (to <= from) {
      from = 0;
      to = this.store.state.doc.length;
    }
    log.debug('Formatting from', from, 'to', to);
    await this.withUpdate<void>(async () => {
      const result = await this.webSocketClient.send({
        resource: this.resourceName,
        serviceType: 'format',
        selectionStart: from,
        selectionEnd: to,
      });
      const { stateId, formattedText } = FormattingResult.parse(result);
      this.applyBeforeDirtyChanges({
        from,
        to,
        insert: formattedText,
      });
      return { newStateId: stateId, data: undefined };
    });
  }

  private computeDelta(): Delta | undefined {
    if (this.dirtyChanges.empty) {
      return undefined;
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

  private applyBeforeDirtyChanges(changeSpec: ChangeSpec): void {
    const pendingChanges =
      this.pendingUpdate?.compose(this.dirtyChanges) ?? this.dirtyChanges;
    const revertChanges = pendingChanges.invert(this.store.state.doc);
    const applyBefore = ChangeSet.of(changeSpec, revertChanges.newLength);
    const redoChanges = pendingChanges.map(applyBefore.desc);
    const changeSet = revertChanges.compose(applyBefore).compose(redoChanges);
    this.store.dispatch({
      changes: changeSet,
      // Keep the current set of dirty changes (but update them according the re-formatting)
      // and to not add the formatting the dirty changes.
      effects: [setDirtyChanges.of(redoChanges)],
    });
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
   * use `T | undefined` instead of `T` as a return type and signal the need for additional
   * computations by returning `undefined`. Thus additional computations can be performed
   * outside of the critical section.
   *
   * @param callback the asynchronous callback that updates the server state
   * @returns a promise resolving to the second value returned by `callback`
   */
  private async withUpdate<T>(
    callback: () => Promise<StateUpdateResult<T>>,
  ): Promise<T> {
    if (this.pendingUpdate !== undefined) {
      throw new Error('Another update is pending, will not perform update');
    }
    this.pendingUpdate = this.dirtyChanges;
    this.dirtyChanges = this.newEmptyChangeSet();
    try {
      const { newStateId, data } = await callback();
      this.xtextStateId = newStateId;
      this.pendingUpdate = undefined;
      this.updatedCondition.notifyAll();
      return data;
    } catch (e) {
      log.error('Error while update', e);
      if (this.pendingUpdate === undefined) {
        log.error('pendingUpdate was cleared during update');
      } else {
        this.dirtyChanges = this.pendingUpdate.compose(this.dirtyChanges);
      }
      this.pendingUpdate = undefined;
      this.webSocketClient.forceReconnectOnError();
      this.updatedCondition.rejectAll(e);
      throw e;
    }
  }

  /**
   * Ensures that there is some state available on the server (`xtextStateId`)
   * and that there is no pending update.
   *
   * After this function resolves, a delta text update is possible.
   *
   * @returns a promise resolving when there is a valid state id but no pending update
   */
  private async prepareForDeltaUpdate(): Promise<void> {
    // If no update is pending, but the full text hasn't been uploaded to the server yet,
    // we must start a full text upload.
    if (this.pendingUpdate === undefined && this.xtextStateId === undefined) {
      await this.updateFullText();
    }
    await this.updatedCondition.waitFor();
  }
}
