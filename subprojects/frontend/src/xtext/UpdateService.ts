import {
  type ChangeDesc,
  ChangeSet,
  type ChangeSpec,
  StateEffect,
  type Transaction,
} from '@codemirror/state';
import { E_CANCELED, E_TIMEOUT, Mutex, withTimeout } from 'async-mutex';
import { debounce } from 'lodash-es';
import { nanoid } from 'nanoid';

import type EditorStore from '../editor/EditorStore';
import getLogger from '../utils/getLogger';

import type XtextWebSocketClient from './XtextWebSocketClient';
import {
  type ContentAssistEntry,
  ContentAssistResult,
  DocumentStateResult,
  FormattingResult,
  isConflictResult,
  OccurrencesResult,
} from './xtextServiceResults';

const UPDATE_TIMEOUT_MS = 500;

const WAIT_FOR_UPDATE_TIMEOUT_MS = 1000;

const FORMAT_TEXT_RETRIES = 5;

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

export interface AbortSignal {
  aborted: boolean;
}

export interface ContentAssistParams {
  caretOffset: number;

  proposalsLimit: number;
}

export type CancellableResult<T> =
  | { cancelled: false; data: T }
  | { cancelled: true };

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
  readonly resourceName: string;

  xtextStateId: string | undefined;

  private readonly store: EditorStore;

  private readonly mutex = withTimeout(new Mutex(), WAIT_FOR_UPDATE_TIMEOUT_MS);

  /**
   * The changes being synchronized to the server if a full or delta text update is running
   * withing a `withUpdateExclusive` block, `undefined` otherwise.
   *
   * Must be `undefined` before and after entering the critical section of `mutex`
   * and may only be changes in the critical section of `mutex`.
   *
   * Methods named with an `Exclusive` suffix in this class assume that the mutex is held
   * and may call `withUpdateExclusive` or `doFallbackUpdateFullTextExclusive`
   * to mutate this field.
   *
   * Methods named with a `do` suffix assume that they are called in a `withUpdateExclusive`
   * block and require this field to be non-`undefined`.
   */
  private pendingUpdate: ChangeSet | undefined;

  /**
   * Local changes not yet sychronized to the server and not part of the running update, if any.
   */
  private dirtyChanges: ChangeSet;

  private readonly webSocketClient: XtextWebSocketClient;

  private readonly idleUpdateLater = debounce(
    () => this.idleUpdate(),
    UPDATE_TIMEOUT_MS,
  );

  constructor(store: EditorStore, webSocketClient: XtextWebSocketClient) {
    this.resourceName = `${nanoid(7)}.problem`;
    this.store = store;
    this.dirtyChanges = this.newEmptyChangeSet();
    this.webSocketClient = webSocketClient;
  }

  onReconnect(): void {
    this.xtextStateId = undefined;
    this.updateFullText().catch((error) => {
      // Let E_TIMEOUT errors propagate, since if the first update times out,
      // we can't use the connection.
      if (error === E_CANCELED) {
        // Content assist will perform a full-text update anyways.
        log.debug('Full text update cancelled');
        return;
      }
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
        // Do not clear `pendingUpdate`, because that would indicate an update failure
        // to `withUpdateExclusive`.
        this.pendingUpdate = ChangeSet.empty(value.length);
      }
      this.dirtyChanges = value;
      return;
    }
    if (transaction.docChanged) {
      this.dirtyChanges = this.dirtyChanges.compose(transaction.changes);
      this.idleUpdateLater();
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

  private idleUpdate(): void {
    if (!this.webSocketClient.isOpen || this.dirtyChanges.empty) {
      return;
    }
    if (!this.mutex.isLocked()) {
      this.update().catch((error) => {
        if (error === E_CANCELED || error === E_TIMEOUT) {
          log.debug('Idle update cancelled');
          return;
        }
        log.error('Unexpected error during scheduled update', error);
      });
    }
    this.idleUpdateLater();
  }

  private newEmptyChangeSet(): ChangeSet {
    return ChangeSet.of([], this.store.state.doc.length);
  }

  private updateFullText(): Promise<void> {
    return this.runExclusive(() => this.updateFullTextExclusive());
  }

  private async updateFullTextExclusive(): Promise<void> {
    await this.withVoidUpdateExclusive(() => this.doUpdateFullTextExclusive());
  }

  private async doUpdateFullTextExclusive(): Promise<string> {
    const result = await this.webSocketClient.send({
      resource: this.resourceName,
      serviceType: 'update',
      fullText: this.store.state.doc.sliceString(0),
    });
    const { stateId } = DocumentStateResult.parse(result);
    return stateId;
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
  private async update(): Promise<void> {
    // We may check here for the delta to avoid locking,
    // but we'll need to recompute the delta in the critical section,
    // because it may have changed by the time we can acquire the lock.
    if (this.dirtyChanges.empty) {
      return;
    }
    await this.runExclusive(() => this.updateExclusive());
  }

  private async updateExclusive(): Promise<void> {
    if (this.xtextStateId === undefined) {
      await this.updateFullTextExclusive();
    }
    const delta = this.computeDelta();
    if (delta === undefined) {
      return;
    }
    log.trace('Editor delta', delta);
    await this.withVoidUpdateExclusive(async () => {
      const result = await this.webSocketClient.send({
        resource: this.resourceName,
        serviceType: 'update',
        requiredStateId: this.xtextStateId,
        ...delta,
      });
      const parsedDocumentStateResult = DocumentStateResult.safeParse(result);
      if (parsedDocumentStateResult.success) {
        return parsedDocumentStateResult.data.stateId;
      }
      if (isConflictResult(result, 'invalidStateId')) {
        return this.doFallbackUpdateFullTextExclusive();
      }
      throw parsedDocumentStateResult.error;
    });
  }

  async fetchOccurrences(
    getCaretOffset: () => CancellableResult<number>,
  ): Promise<CancellableResult<OccurrencesResult>> {
    try {
      await this.update();
    } catch (error) {
      if (error === E_CANCELED || error === E_TIMEOUT) {
        return { cancelled: true };
      }
      throw error;
    }
    if (!this.dirtyChanges.empty || this.mutex.isLocked()) {
      // Just give up if another update is in progress.
      return { cancelled: true };
    }
    const caretOffsetResult = getCaretOffset();
    if (caretOffsetResult.cancelled) {
      return { cancelled: true };
    }
    const expectedStateId = this.xtextStateId;
    const data = await this.webSocketClient.send({
      resource: this.resourceName,
      serviceType: 'occurrences',
      caretOffset: caretOffsetResult.data,
      expectedStateId,
    });
    if (
      // The query must have reached the server without being conflicted with an update
      // or cancelled server-side.
      isConflictResult(data) ||
      // And no state update should have occurred since then.
      this.xtextStateId !== expectedStateId ||
      // And there should be no change to the editor text since then.
      !this.dirtyChanges.empty ||
      // And there should be no state update in progress.
      this.mutex.isLocked()
    ) {
      return { cancelled: true };
    }
    const parsedOccurrencesResult = OccurrencesResult.safeParse(data);
    if (!parsedOccurrencesResult.success) {
      log.error(
        'Unexpected occurences result',
        data,
        'not an OccurrencesResult:',
        parsedOccurrencesResult.error,
      );
      throw parsedOccurrencesResult.error;
    }
    if (parsedOccurrencesResult.data.stateId !== expectedStateId) {
      return { cancelled: true };
    }
    return { cancelled: false, data: parsedOccurrencesResult.data };
  }

  async fetchContentAssist(
    params: ContentAssistParams,
    signal: AbortSignal,
  ): Promise<ContentAssistEntry[]> {
    if (!this.mutex.isLocked && this.xtextStateId !== undefined) {
      return this.fetchContentAssistFetchOnly(params, this.xtextStateId);
    }
    // Content assist updates should have priority over other updates.
    this.mutex.cancel();
    try {
      return await this.runExclusive(() =>
        this.fetchContentAssistExclusive(params, signal),
      );
    } catch (error) {
      if ((error === E_CANCELED || error === E_TIMEOUT) && signal.aborted) {
        return [];
      }
      throw error;
    }
  }

  private async fetchContentAssistExclusive(
    params: ContentAssistParams,
    signal: AbortSignal,
  ): Promise<ContentAssistEntry[]> {
    if (this.xtextStateId === undefined) {
      await this.updateFullTextExclusive();
    }
    if (signal.aborted) {
      return [];
    }
    const delta = this.computeDelta();
    if (delta !== undefined) {
      log.trace('Editor delta', delta);
      // Try to fetch while also performing a delta update.
      const fetchUpdateEntries = await this.withUpdateExclusive(() =>
        this.doFetchContentAssistWithDeltaExclusive(params, delta),
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
    return this.fetchContentAssistFetchOnly(params, this.xtextStateId);
  }

  private async doFetchContentAssistWithDeltaExclusive(
    params: ContentAssistParams,
    delta: Delta,
  ): Promise<StateUpdateResult<ContentAssistEntry[] | undefined>> {
    const fetchUpdateResult = await this.webSocketClient.send({
      ...params,
      resource: this.resourceName,
      serviceType: 'assist',
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
      const newStateId = await this.doFallbackUpdateFullTextExclusive();
      // We must finish this state update transaction to prepare for any push events
      // before querying for content assist, so we just return `undefined` and will query
      // the content assist service later.
      return { newStateId, data: undefined };
    }
    throw parsedContentAssistResult.error;
  }

  private async fetchContentAssistFetchOnly(
    params: ContentAssistParams,
    requiredStateId: string,
  ): Promise<ContentAssistEntry[]> {
    // Fallback to fetching without a delta update.
    const fetchOnlyResult = await this.webSocketClient.send({
      ...params,
      resource: this.resourceName,
      serviceType: 'assist',
      requiredStateId,
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
    let retries = 0;
    while (retries < FORMAT_TEXT_RETRIES) {
      try {
        // eslint-disable-next-line no-await-in-loop -- Use a loop for sequential retries.
        await this.runExclusive(() => this.formatTextExclusive());
        return;
      } catch (error) {
        // Let timeout errors propagate to give up formatting on a flaky connection.
        if (error === E_CANCELED && retries < FORMAT_TEXT_RETRIES) {
          retries += 1;
        } else {
          throw error;
        }
      }
    }
  }

  private async formatTextExclusive(): Promise<void> {
    await this.updateExclusive();
    let { from, to } = this.store.state.selection.main;
    if (to <= from) {
      from = 0;
      to = this.store.state.doc.length;
    }
    log.debug('Formatting from', from, 'to', to);
    await this.withVoidUpdateExclusive(async () => {
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
      return stateId;
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

  private runExclusive<T>(callback: () => Promise<T>): Promise<T> {
    return this.mutex.runExclusive(async () => {
      if (this.pendingUpdate !== undefined) {
        throw new Error('Update is pending before entering critical section');
      }
      const result = await callback();
      if (this.pendingUpdate !== undefined) {
        throw new Error('Update is pending after entering critical section');
      }
      return result;
    });
  }

  private withVoidUpdateExclusive(
    callback: () => Promise<string>,
  ): Promise<void> {
    return this.withUpdateExclusive<void>(async () => {
      const newStateId = await callback();
      return { newStateId, data: undefined };
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
  private async withUpdateExclusive<T>(
    callback: () => Promise<StateUpdateResult<T>>,
  ): Promise<T> {
    if (this.pendingUpdate !== undefined) {
      throw new Error('Delta updates are not reentrant');
    }
    this.pendingUpdate = this.dirtyChanges;
    this.dirtyChanges = this.newEmptyChangeSet();
    let data: T;
    try {
      ({ newStateId: this.xtextStateId, data } = await callback());
      this.pendingUpdate = undefined;
    } catch (e) {
      log.error('Error while update', e);
      if (this.pendingUpdate === undefined) {
        log.error('pendingUpdate was cleared during update');
      } else {
        this.dirtyChanges = this.pendingUpdate.compose(this.dirtyChanges);
      }
      this.pendingUpdate = undefined;
      this.webSocketClient.forceReconnectOnError();
      throw e;
    }
    return data;
  }

  private doFallbackUpdateFullTextExclusive(): Promise<string> {
    if (this.pendingUpdate === undefined) {
      throw new Error('Only a pending update can be extended');
    }
    log.warn('Delta update failed, performing full text update');
    this.xtextStateId = undefined;
    this.pendingUpdate = this.pendingUpdate.compose(this.dirtyChanges);
    this.dirtyChanges = this.newEmptyChangeSet();
    return this.doUpdateFullTextExclusive();
  }
}
