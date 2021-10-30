import {
  Completion,
  CompletionContext,
  CompletionResult,
} from '@codemirror/autocomplete';
import type { Diagnostic } from '@codemirror/lint';
import {
  ChangeDesc,
  ChangeSet,
  Transaction,
} from '@codemirror/state';
import { nanoid } from 'nanoid';

import type { EditorStore } from './EditorStore';
import { getLogger } from '../logging';
import { Timer } from '../utils/Timer';
import {
  IContentAssistEntry,
  isContentAssistResult,
  isDocumentStateResult,
  isInvalidStateIdConflictResult,
  isValidationResult,
} from './xtextServiceResults';
import { XtextWebSocketClient } from './XtextWebSocketClient';
import { PendingTask } from '../utils/PendingTask';

const UPDATE_TIMEOUT_MS = 500;

const WAIT_FOR_UPDATE_TIMEOUT_MS = 1000;

const log = getLogger('XtextClient');

export class XtextClient {
  resourceName: string;

  webSocketClient: XtextWebSocketClient;

  xtextStateId: string | null = null;

  pendingUpdate: ChangeDesc | null;

  dirtyChanges: ChangeDesc;

  lastCompletion: CompletionResult | null = null;

  updateListeners: PendingTask<void>[] = [];

  updateTimer = new Timer(() => {
    this.handleUpdate();
  }, UPDATE_TIMEOUT_MS);

  store: EditorStore;

  constructor(store: EditorStore) {
    this.resourceName = `${nanoid(7)}.problem`;
    this.pendingUpdate = null;
    this.store = store;
    this.dirtyChanges = this.newEmptyChangeDesc();
    this.webSocketClient = new XtextWebSocketClient(
      async () => {
        this.xtextStateId = null;
        await this.updateFullText();
      },
      async (resource, stateId, service, push) => {
        await this.onPush(resource, stateId, service, push);
      },
    );
  }

  onTransaction(transaction: Transaction): void {
    const { changes } = transaction;
    if (!changes.empty) {
      if (this.shouldInvalidateCachedCompletion(transaction)) {
        log.trace('Invalidating cached completions');
        this.lastCompletion = null;
      }
      this.dirtyChanges = this.dirtyChanges.composeDesc(changes.desc);
      this.updateTimer.reschedule();
    }
  }

  private async onPush(resource: string, stateId: string, service: string, push: unknown) {
    if (resource !== this.resourceName) {
      log.error('Unknown resource name: expected:', this.resourceName, 'got:', resource);
      return;
    }
    if (stateId !== this.xtextStateId) {
      log.error('Unexpected xtext state id: expected:', this.xtextStateId, 'got:', resource);
      await this.updateFullText();
    }
    switch (service) {
      case 'validate':
        this.onValidate(push);
        return;
      case 'highlight':
        // TODO
        return;
      default:
        log.error('Unknown push service:', service);
        break;
    }
  }

  private onValidate(push: unknown) {
    if (!isValidationResult(push)) {
      log.error('Invalid validation result', push);
      return;
    }
    const allChanges = this.computeChangesSinceLastUpdate();
    const diagnostics: Diagnostic[] = [];
    push.issues.forEach((issue) => {
      if (issue.severity === 'ignore') {
        return;
      }
      diagnostics.push({
        from: allChanges.mapPos(issue.offset),
        to: allChanges.mapPos(issue.offset + issue.length),
        severity: issue.severity,
        message: issue.description,
      });
    });
    this.store.updateDiagnostics(diagnostics);
  }

  private computeChangesSinceLastUpdate() {
    return this.pendingUpdate?.composeDesc(this.dirtyChanges) || this.dirtyChanges;
  }

  private handleUpdate() {
    if (!this.webSocketClient.isOpen || this.dirtyChanges.empty) {
      return;
    }
    if (this.pendingUpdate === null) {
      this.update().catch((error) => {
        log.error('Unexpected error during scheduled update', error);
      });
    }
    this.updateTimer.reschedule();
  }

  private newEmptyChangeDesc() {
    const changeSet = ChangeSet.of([], this.store.state.doc.length);
    return changeSet.desc;
  }

  private async updateFullText() {
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

  async contentAssist(context: CompletionContext): Promise<CompletionResult> {
    const tokenBefore = context.tokenBefore(['QualifiedName']);
    if (tokenBefore === null && !context.explicit) {
      return {
        from: context.pos,
        options: [],
      };
    }
    const range = {
      from: tokenBefore?.from || context.pos,
      to: tokenBefore?.to || context.pos,
    };
    if (this.shouldReturnCachedCompletion(tokenBefore)) {
      log.trace('Returning cached completion result');
      // Postcondition of `shouldReturnCachedCompletion`: `lastCompletion !== null`
      return {
        ...this.lastCompletion as CompletionResult,
        ...range,
      };
    }
    const entries = await this.fetchContentAssist(context);
    if (context.aborted) {
      return {
        ...range,
        options: [],
      };
    }
    const options: Completion[] = [];
    entries.forEach((entry) => {
      options.push({
        label: entry.proposal,
        detail: entry.description,
        info: entry.documentation,
        type: entry.kind?.toLowerCase(),
        boost: entry.kind === 'KEYWORD' ? -90 : 0,
      });
    });
    log.debug('Fetched', options.length, 'completions from server');
    this.lastCompletion = {
      ...range,
      options,
      span: /[a-zA-Z0-9_:]/,
    };
    return this.lastCompletion;
  }

  private shouldReturnCachedCompletion(
    token: { from: number, to: number, text: string } | null,
  ) {
    if (token === null || this.lastCompletion === null) {
      return false;
    }
    const { from, to, text } = token;
    const { from: lastFrom, to: lastTo, span } = this.lastCompletion;
    if (!lastTo) {
      return true;
    }
    const transformedFrom = this.dirtyChanges.mapPos(lastFrom);
    const transformedTo = this.dirtyChanges.mapPos(lastTo, 1);
    return from >= transformedFrom && to <= transformedTo && span && span.exec(text);
  }

  private shouldInvalidateCachedCompletion(transaction: Transaction) {
    if (this.lastCompletion === null) {
      return false;
    }
    const { from: lastFrom, to: lastTo } = this.lastCompletion;
    if (!lastTo) {
      return true;
    }
    const transformedFrom = this.dirtyChanges.mapPos(lastFrom);
    const transformedTo = this.dirtyChanges.mapPos(lastTo, 1);
    let invalidate = false;
    transaction.changes.iterChangedRanges((fromA, toA) => {
      if (fromA < transformedFrom || toA > transformedTo) {
        invalidate = true;
      }
    });
    return invalidate;
  }

  private async fetchContentAssist(context: CompletionContext) {
    await this.prepareForDeltaUpdate();
    const delta = this.computeDelta();
    if (delta === null) {
      // Poscondition of `prepareForDeltaUpdate`: `xtextStateId !== null`
      return this.doFetchContentAssist(context, this.xtextStateId as string);
    }
    log.trace('Editor delta', delta);
    return await this.withUpdate(async () => {
      const result = await this.webSocketClient.send({
        requiredStateId: this.xtextStateId,
        ...this.computeContentAssistParams(context),
        ...delta,
      });
      if (isContentAssistResult(result)) {
        return [result.stateId, result.entries];
      }
      if (isInvalidStateIdConflictResult(result)) {
        const [newStateId] = await this.doFallbackToUpdateFullText();
        if (context.aborted) {
          return [newStateId, [] as IContentAssistEntry[]];
        }
        const entries = await this.doFetchContentAssist(context, newStateId);
        return [newStateId, entries];
      }
      log.error('Unextpected content assist result with delta update', result);
      throw new Error('Unexpexted content assist result with delta update');
    });
  }

  private async doFetchContentAssist(context: CompletionContext, expectedStateId: string) {
    const result = await this.webSocketClient.send({
      requiredStateId: expectedStateId,
      ...this.computeContentAssistParams(context),
    });
    if (isContentAssistResult(result) && result.stateId === expectedStateId) {
      return result.entries;
    }
    log.error('Unexpected content assist result', result);
    throw new Error('Unexpected content assist result');
  }

  private computeContentAssistParams(context: CompletionContext) {
    const tokenBefore = context.tokenBefore(['QualifiedName']);
    let selection = {};
    if (tokenBefore !== null) {
      selection = {
        selectionStart: tokenBefore.from,
        selectionEnd: tokenBefore.to,
      };
    }
    return {
      resource: this.resourceName,
      serviceType: 'assist',
      caretOffset: tokenBefore?.from || context.pos,
      ...selection,
    };
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
