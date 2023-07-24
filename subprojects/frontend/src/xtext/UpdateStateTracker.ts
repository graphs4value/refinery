/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import {
  type ChangeDesc,
  ChangeSet,
  type ChangeSpec,
  StateEffect,
  type Transaction,
} from '@codemirror/state';

import type EditorStore from '../editor/EditorStore';
import PriorityMutex from '../utils/PriorityMutex';

const WAIT_FOR_UPDATE_TIMEOUT_MS = 1000;

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

export interface Delta {
  deltaOffset: number;

  deltaReplaceLength: number;

  deltaText: string;
}

export default class UpdateStateTracker {
  private _xtextStateId: string | undefined;

  /**
   * The changes marked for synchronization to the server if a full or delta text update
   * is running, `undefined` otherwise.
   *
   * Must be `undefined` upon entering the critical section of `mutex`,
   * may only be changed in the critical section of `mutex`,
   * and will be set to `undefined` (marking any changes as dirty again) when leaving it.
   *
   * Methods named with an `Exclusive` suffix in this class assume that the mutex is held
   * and may mutate this field.
   */
  private pendingChanges: ChangeSet | undefined;

  /**
   * Local changes not yet sychronized to the server and not part of the current update, if any.
   */
  private dirtyChanges: ChangeSet;

  /**
   * Locked when we try to modify the state on the server.
   */
  private readonly mutex = new PriorityMutex(WAIT_FOR_UPDATE_TIMEOUT_MS);

  constructor(private readonly store: EditorStore) {
    this.dirtyChanges = this.newEmptyChangeSet();
  }

  get xtextStateId(): string | undefined {
    return this._xtextStateId;
  }

  private get hasDirtyChanges(): boolean {
    return !this.dirtyChanges.empty;
  }

  get needsUpdate(): boolean {
    return this.hasDirtyChanges || this.xtextStateId === undefined;
  }

  get lockedForUpdate(): boolean {
    return this.mutex.locked;
  }

  get hasPendingChanges(): boolean {
    return this.lockedForUpdate || this.needsUpdate;
  }

  hasChangesSince(xtextStateId: string): boolean {
    return this.xtextStateId !== xtextStateId || this.hasPendingChanges;
  }

  /**
   * Extends the current set of changes with `transaction`.
   *
   * Also determines if the transaction has made local changes
   * that will have to be synchronized to the server
   *
   * @param transaction the transaction that affected the editor
   * @returns `true` if the transaction requires and idle update, `false` otherwise
   */
  onTransaction(transaction: Transaction): boolean {
    const setDirtyChangesEffect = transaction.effects.find((effect) =>
      effect.is(setDirtyChanges),
    ) as StateEffect<ChangeSet> | undefined;
    if (setDirtyChangesEffect) {
      const { value } = setDirtyChangesEffect;
      if (this.pendingChanges !== undefined) {
        // Do not clear `pendingUpdate`, because that would indicate an update failure
        // to `withUpdateExclusive`.
        this.pendingChanges = ChangeSet.empty(value.length);
      }
      this.dirtyChanges = value;
      return false;
    }
    if (transaction.docChanged) {
      this.dirtyChanges = this.dirtyChanges.compose(transaction.changes);
      return true;
    }
    return false;
  }

  invalidateStateId(): void {
    this._xtextStateId = undefined;
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
      this.pendingChanges?.composeDesc(this.dirtyChanges.desc) ??
      this.dirtyChanges.desc
    );
  }

  prepareDeltaUpdateExclusive(): Delta | undefined {
    this.ensureLocked();
    this.markDirtyChangesAsPendingExclusive();
    if (this.pendingChanges === undefined || this.pendingChanges.empty) {
      return undefined;
    }
    let minFromA = Number.MAX_SAFE_INTEGER;
    let maxToA = 0;
    let minFromB = Number.MAX_SAFE_INTEGER;
    let maxToB = 0;
    this.pendingChanges.iterChangedRanges((fromA, toA, fromB, toB) => {
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

  prepareFullTextUpdateExclusive(): void {
    this.ensureLocked();
    this.markDirtyChangesAsPendingExclusive();
  }

  private markDirtyChangesAsPendingExclusive(): void {
    if (!this.lockedForUpdate) {
      throw new Error('Cannot update state without locking the mutex');
    }
    if (this.hasDirtyChanges) {
      this.pendingChanges =
        this.pendingChanges?.compose(this.dirtyChanges) ?? this.dirtyChanges;
      this.dirtyChanges = this.newEmptyChangeSet();
    }
  }

  private newEmptyChangeSet(): ChangeSet {
    return ChangeSet.of([], this.store.state.doc.length);
  }

  setStateIdExclusive(
    newStateId: string,
    remoteChanges?: ChangeSpec | undefined,
  ): void {
    this.ensureLocked();
    if (remoteChanges !== undefined) {
      this.applyRemoteChangesExclusive(remoteChanges);
    }
    this._xtextStateId = newStateId;
    this.pendingChanges = undefined;
  }

  private applyRemoteChangesExclusive(changeSpec: ChangeSpec): void {
    const pendingChanges =
      this.pendingChanges?.compose(this.dirtyChanges) ?? this.dirtyChanges;
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

  private ensureLocked(): void {
    if (!this.lockedForUpdate) {
      throw new Error('Cannot update state without locking the mutex');
    }
  }

  runExclusive<T>(
    callback: () => Promise<T>,
    highPriority = false,
  ): Promise<T> {
    return this.mutex.runExclusive(async () => {
      try {
        return await callback();
      } finally {
        if (this.pendingChanges !== undefined) {
          this.dirtyChanges = this.pendingChanges.compose(this.dirtyChanges);
          this.pendingChanges = undefined;
        }
      }
    }, highPriority);
  }
}
