import type { ChangeDesc, Transaction } from '@codemirror/state';
import { E_CANCELED, E_TIMEOUT } from 'async-mutex';
import { debounce } from 'lodash-es';
import { nanoid } from 'nanoid';

import type EditorStore from '../editor/EditorStore';
import getLogger from '../utils/getLogger';

import UpdateStateTracker, {
  type LockedState,
  type PendingUpdate,
} from './UpdateStateTracker';
import type { StateUpdateResult, Delta } from './UpdateStateTracker';
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

const log = getLogger('xtext.UpdateService');

export interface AbortSignal {
  aborted: boolean;
}

export type CancellableResult<T> =
  | { cancelled: false; data: T }
  | { cancelled: true };

export interface ContentAssistParams {
  caretOffset: number;

  proposalsLimit: number;
}

export default class UpdateService {
  readonly resourceName: string;

  private readonly tracker: UpdateStateTracker;

  private readonly idleUpdateLater = debounce(
    () => this.idleUpdate(),
    UPDATE_TIMEOUT_MS,
  );

  constructor(
    private readonly store: EditorStore,
    private readonly webSocketClient: XtextWebSocketClient,
  ) {
    this.resourceName = `${nanoid(7)}.problem`;
    this.tracker = new UpdateStateTracker(store);
  }

  get xtextStateId(): string | undefined {
    return this.tracker.xtextStateId;
  }

  computeChangesSinceLastUpdate(): ChangeDesc {
    return this.tracker.computeChangesSinceLastUpdate();
  }

  onReconnect(): void {
    this.tracker.invalidateStateId();
    this.updateFullTextOrThrow().catch((error) => {
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
    if (this.tracker.onTransaction(transaction)) {
      this.idleUpdateLater();
    }
  }

  private idleUpdate(): void {
    if (!this.webSocketClient.isOpen || !this.tracker.hasDirtyChanges) {
      return;
    }
    if (!this.tracker.locekdForUpdate) {
      this.updateOrThrow().catch((error) => {
        if (error === E_CANCELED || error === E_TIMEOUT) {
          log.debug('Idle update cancelled');
          return;
        }
        log.error('Unexpected error during scheduled update', error);
      });
    }
    this.idleUpdateLater();
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
  private async updateOrThrow(): Promise<void> {
    // We may check here for the delta to avoid locking,
    // but we'll need to recompute the delta in the critical section,
    // because it may have changed by the time we can acquire the lock.
    if (
      !this.tracker.hasDirtyChanges &&
      this.tracker.xtextStateId !== undefined
    ) {
      return;
    }
    await this.tracker.runExclusive((lockedState) =>
      this.updateExclusive(lockedState),
    );
  }

  private async updateExclusive(lockedState: LockedState): Promise<void> {
    if (this.xtextStateId === undefined) {
      await this.updateFullTextExclusive(lockedState);
    }
    if (!this.tracker.hasDirtyChanges) {
      return;
    }
    await lockedState.updateExclusive(async (pendingUpdate) => {
      const delta = pendingUpdate.prepareDeltaUpdateExclusive();
      if (delta === undefined) {
        return undefined;
      }
      log.trace('Editor delta', delta);
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
        return this.doUpdateFullTextExclusive(pendingUpdate);
      }
      throw parsedDocumentStateResult.error;
    });
  }

  private updateFullTextOrThrow(): Promise<void> {
    return this.tracker.runExclusive((lockedState) =>
      this.updateFullTextExclusive(lockedState),
    );
  }

  private async updateFullTextExclusive(
    lockedState: LockedState,
  ): Promise<void> {
    await lockedState.updateExclusive((pendingUpdate) =>
      this.doUpdateFullTextExclusive(pendingUpdate),
    );
  }

  private async doUpdateFullTextExclusive(
    pendingUpdate: PendingUpdate,
  ): Promise<string> {
    log.debug('Performing full text update');
    pendingUpdate.extendPendingUpdateExclusive();
    const result = await this.webSocketClient.send({
      resource: this.resourceName,
      serviceType: 'update',
      fullText: this.store.state.doc.sliceString(0),
    });
    const { stateId } = DocumentStateResult.parse(result);
    return stateId;
  }

  async fetchContentAssist(
    params: ContentAssistParams,
    signal: AbortSignal,
  ): Promise<ContentAssistEntry[]> {
    if (this.tracker.upToDate && this.xtextStateId !== undefined) {
      return this.fetchContentAssistFetchOnly(params, this.xtextStateId);
    }
    try {
      return await this.tracker.runExclusiveHighPriority((lockedState) =>
        this.fetchContentAssistExclusive(params, lockedState, signal),
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
    lockedState: LockedState,
    signal: AbortSignal,
  ): Promise<ContentAssistEntry[]> {
    if (this.xtextStateId === undefined) {
      await this.updateFullTextExclusive(lockedState);
    }
    if (signal.aborted) {
      return [];
    }
    if (this.tracker.hasDirtyChanges) {
      // Try to fetch while also performing a delta update.
      const fetchUpdateEntries = await lockedState.withUpdateExclusive(
        async (pendingUpdate) => {
          const delta = pendingUpdate.prepareDeltaUpdateExclusive();
          if (delta === undefined) {
            return { newStateId: undefined, data: undefined };
          }
          log.trace('Editor delta', delta);
          return this.doFetchContentAssistWithDeltaExclusive(
            params,
            pendingUpdate,
            delta,
          );
        },
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
    pendingUpdate: PendingUpdate,
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
      const newStateId = await this.doUpdateFullTextExclusive(pendingUpdate);
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

  formatText(): Promise<void> {
    return this.tracker.runExclusiveWithRetries((lockedState) =>
      this.formatTextExclusive(lockedState),
    );
  }

  private async formatTextExclusive(lockedState: LockedState): Promise<void> {
    await this.updateExclusive(lockedState);
    let { from, to } = this.store.state.selection.main;
    if (to <= from) {
      from = 0;
      to = this.store.state.doc.length;
    }
    log.debug('Formatting from', from, 'to', to);
    await lockedState.updateExclusive(async (pendingUpdate) => {
      const result = await this.webSocketClient.send({
        resource: this.resourceName,
        serviceType: 'format',
        selectionStart: from,
        selectionEnd: to,
      });
      const { stateId, formattedText } = FormattingResult.parse(result);
      pendingUpdate.applyBeforeDirtyChangesExclusive({
        from,
        to,
        insert: formattedText,
      });
      return stateId;
    });
  }

  async fetchOccurrences(
    getCaretOffset: () => CancellableResult<number>,
  ): Promise<CancellableResult<OccurrencesResult>> {
    try {
      await this.updateOrThrow();
    } catch (error) {
      if (error === E_CANCELED || error === E_TIMEOUT) {
        return { cancelled: true };
      }
      throw error;
    }
    if (!this.tracker.upToDate) {
      // Just give up if another update is in progress.
      return { cancelled: true };
    }
    const caretOffsetResult = getCaretOffset();
    if (caretOffsetResult.cancelled) {
      return { cancelled: true };
    }
    const expectedStateId = this.xtextStateId;
    if (expectedStateId === undefined) {
      // If there is no state on the server, don't bother with finding occurrences.
      return { cancelled: true };
    }
    const data = await this.webSocketClient.send({
      resource: this.resourceName,
      serviceType: 'occurrences',
      caretOffset: caretOffsetResult.data,
      expectedStateId,
    });
    if (
      isConflictResult(data) ||
      this.tracker.hasChangesSince(expectedStateId)
    ) {
      return { cancelled: true };
    }
    const parsedOccurrencesResult = OccurrencesResult.parse(data);
    if (parsedOccurrencesResult.stateId !== expectedStateId) {
      return { cancelled: true };
    }
    return { cancelled: false, data: parsedOccurrencesResult };
  }
}
