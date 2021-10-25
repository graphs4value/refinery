import type { Diagnostic } from '@codemirror/lint';
import {
  ChangeDesc,
  ChangeSet,
  Transaction,
} from '@codemirror/state';
import { nanoid } from 'nanoid';

import type { EditorStore } from './EditorStore';
import { getLogger } from '../logging';
import {
  isDocumentStateResult,
  isServiceConflictResult,
  isValidationResult,
} from './xtextServiceResults';
import { XtextWebSocketClient } from './XtextWebSocketClient';

const UPDATE_TIMEOUT_MS = 300;

const log = getLogger('XtextClient');

enum UpdateAction {
  ForceReconnect,

  FullTextUpdate,
}

export class XtextClient {
  resourceName: string;

  webSocketClient: XtextWebSocketClient;

  xtextStateId: string | null = null;

  pendingUpdate: ChangeDesc | null;

  dirtyChanges: ChangeDesc;

  updateTimeout: NodeJS.Timeout | null = null;

  store: EditorStore;

  constructor(store: EditorStore) {
    this.resourceName = `${nanoid(7)}.problem`;
    this.pendingUpdate = null;
    this.store = store;
    this.dirtyChanges = this.newEmptyChangeDesc();
    this.webSocketClient = new XtextWebSocketClient(
      () => {
        this.updateFullText().catch((error) => {
          log.error('Unexpected error during initial update', error);
        });
      },
      (resource, stateId, service, push) => {
        this.onPush(resource, stateId, service, push).catch((error) => {
          log.error('Unexected error during push message handling', error);
        });
      },
    );
  }

  onTransaction(transaction: Transaction): void {
    const { changes } = transaction;
    if (!changes.empty) {
      this.webSocketClient.ensureOpen();
      this.dirtyChanges = this.dirtyChanges.composeDesc(changes.desc);
      this.scheduleUpdate();
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
    if (this.pendingUpdate === null) {
      return this.dirtyChanges;
    }
    return this.pendingUpdate.composeDesc(this.dirtyChanges);
  }

  private scheduleUpdate() {
    if (this.updateTimeout !== null) {
      clearTimeout(this.updateTimeout);
    }
    this.updateTimeout = setTimeout(() => {
      this.updateTimeout = null;
      if (!this.webSocketClient.isOpen || this.dirtyChanges.empty) {
        return;
      }
      if (!this.pendingUpdate) {
        this.updateDeltaText().catch((error) => {
          log.error('Unexpected error during scheduled update', error);
        });
      }
      this.scheduleUpdate();
    }, UPDATE_TIMEOUT_MS);
  }

  private newEmptyChangeDesc() {
    const changeSet = ChangeSet.of([], this.store.state.doc.length);
    return changeSet.desc;
  }

  private async updateFullText() {
    await this.withUpdate(async () => {
      const result = await this.webSocketClient.send({
        resource: this.resourceName,
        serviceType: 'update',
        fullText: this.store.state.doc.sliceString(0),
      });
      if (isDocumentStateResult(result)) {
        return result.stateId;
      }
      if (isServiceConflictResult(result)) {
        log.error('Full text update conflict:', result.conflict);
        if (result.conflict === 'canceled') {
          return UpdateAction.FullTextUpdate;
        }
        return UpdateAction.ForceReconnect;
      }
      log.error('Unexpected full text update result:', result);
      return UpdateAction.ForceReconnect;
    });
  }

  private async updateDeltaText() {
    if (this.xtextStateId === null) {
      await this.updateFullText();
      return;
    }
    const delta = this.computeDelta();
    log.debug('Editor delta', delta);
    await this.withUpdate(async () => {
      const result = await this.webSocketClient.send({
        resource: this.resourceName,
        serviceType: 'update',
        requiredStateId: this.xtextStateId,
        ...delta,
      });
      if (isDocumentStateResult(result)) {
        return result.stateId;
      }
      if (isServiceConflictResult(result)) {
        log.error('Delta text update conflict:', result.conflict);
        return UpdateAction.FullTextUpdate;
      }
      log.error('Unexpected delta text update result:', result);
      return UpdateAction.ForceReconnect;
    });
  }

  private computeDelta() {
    if (this.dirtyChanges.empty) {
      return {};
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

  private async withUpdate(callback: () => Promise<string | UpdateAction>) {
    if (this.pendingUpdate !== null) {
      log.error('Another update is pending, will not perform update');
      return;
    }
    this.pendingUpdate = this.dirtyChanges;
    this.dirtyChanges = this.newEmptyChangeDesc();
    let newStateId: string | UpdateAction = UpdateAction.ForceReconnect;
    try {
      newStateId = await callback();
    } catch (error) {
      log.error('Error while updating state', error);
    } finally {
      if (typeof newStateId === 'string') {
        this.xtextStateId = newStateId;
        this.pendingUpdate = null;
      } else {
        this.dirtyChanges = this.pendingUpdate.composeDesc(this.dirtyChanges);
        this.pendingUpdate = null;
        switch (newStateId) {
          case UpdateAction.ForceReconnect:
            this.webSocketClient.forceReconnectDueToError();
            break;
          case UpdateAction.FullTextUpdate:
            await this.updateFullText();
            break;
        }
      }
    }
  }
}
