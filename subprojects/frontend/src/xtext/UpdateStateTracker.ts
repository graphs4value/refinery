/**
 * @file State tracker for pushing updates to the Xtext server.
 *
 * This file implements complex logic to avoid missing or overwriting state updates
 * and to avoid sending conflicting updates to the Xtext server.
 *
 * The `LockedState` and `PendingUpdate` objects are used as capabilities to
 * signify whether the socket to the Xtext server is locked for updates and
 * whether an update is in progress, respectively.
 * Always use these objects only received as an argument of a lambda expression
 * or method and never leak them into class field or global variables.
 * The presence of such an objects in the scope should always imply that
 * the corresponding condition holds.
 */

import {
  type ChangeDesc,
  ChangeSet,
  type ChangeSpec,
  StateEffect,
  type Transaction,
} from '@codemirror/state';
import { E_CANCELED, Mutex, withTimeout } from 'async-mutex';

import type EditorStore from '../editor/EditorStore';
import getLogger from '../utils/getLogger';

const WAIT_FOR_UPDATE_TIMEOUT_MS = 1000;

const log = getLogger('xtext.UpdateStateTracker');

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

export interface StateUpdateResult<T> {
  /** The new state ID on the server or `undefined` if no update was performed. */
  newStateId: string | undefined;

  /** Optional data payload received during the update. */
  data: T;
}

/**
 * Signifies a capability that the Xtext server state is locked for update.
 */
export interface LockedState {
  /**
   *
   * @param callback the asynchronous callback that updates the server state
   * @returns a promise resolving after the update
   */
  updateExclusive(
    callback: (pendingUpdate: PendingUpdate) => Promise<string | undefined>,
  ): Promise<void>;

  /**
   * Executes an asynchronous callback that updates the state on the server.
   *
   * If the callback returns `undefined` as the `newStateId`,
   * the update is assumed to be aborted and any pending changes will be marked as dirt again.
   * Any exceptions thrown in `callback` will also cause the update to be aborted.
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
   * If additional asynchronous work is needed to compute the second value in some cases,
   * use `T | undefined` instead of `T` as a return type and signal the need for additional
   * computations by returning `undefined`. Thus additional computations can be performed
   * outside of the critical section.
   *
   * @param callback the asynchronous callback that updates the server state
   * @returns a promise resolving to the second value returned by `callback`
   */
  withUpdateExclusive<T>(
    callback: (pendingUpdate: PendingUpdate) => Promise<StateUpdateResult<T>>,
  ): Promise<T>;
}

export interface Delta {
  deltaOffset: number;

  deltaReplaceLength: number;

  deltaText: string;
}

/**
 * Signifies a capability that dirty changes are being marked for uploading.
 */
export interface PendingUpdate {
  prepareDeltaUpdateExclusive(): Delta | undefined;

  extendPendingUpdateExclusive(): void;

  applyBeforeDirtyChangesExclusive(changeSpec: ChangeSpec): void;
}

export default class UpdateStateTracker {
  xtextStateId: string | undefined;

  /**
   * The changes being synchronized to the server if a full or delta text update is running
   * withing a `withUpdateExclusive` block, `undefined` otherwise.
   *
   * Must be `undefined` before and after entering the critical section of `mutex`
   * and may only be changes in the critical section of `mutex`.
   *
   * Methods named with an `Exclusive` suffix in this class assume that the mutex is held
   * and may call `updateExclusive` or `withUpdateExclusive` to mutate this field.
   *
   * Methods named with a `do` suffix assume that they are called in a `withUpdateExclusive`
   * block and require this field to be non-`undefined`.
   */
  private pendingChanges: ChangeSet | undefined;

  /**
   * Local changes not yet sychronized to the server and not part of the current update, if any.
   */
  private dirtyChanges: ChangeSet;

  /**
   * Locked when we try to modify the state on the server.
   */
  private readonly mutex = withTimeout(new Mutex(), WAIT_FOR_UPDATE_TIMEOUT_MS);

  constructor(private readonly store: EditorStore) {
    this.dirtyChanges = this.newEmptyChangeSet();
  }

  get locekdForUpdate(): boolean {
    return this.mutex.isLocked();
  }

  get hasDirtyChanges(): boolean {
    return !this.dirtyChanges.empty;
  }

  get upToDate(): boolean {
    return !this.locekdForUpdate && !this.hasDirtyChanges;
  }

  hasChangesSince(xtextStateId: string): boolean {
    return (
      this.xtextStateId !== xtextStateId ||
      this.locekdForUpdate ||
      this.hasDirtyChanges
    );
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
    this.xtextStateId = undefined;
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

  private newEmptyChangeSet(): ChangeSet {
    return ChangeSet.of([], this.store.state.doc.length);
  }

  private readonly pendingUpdate: PendingUpdate = {
    prepareDeltaUpdateExclusive: (): Delta | undefined => {
      this.pendingUpdate.extendPendingUpdateExclusive();
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
    },
    extendPendingUpdateExclusive: (): void => {
      if (!this.locekdForUpdate) {
        throw new Error('Cannot update state without locking the mutex');
      }
      if (this.hasDirtyChanges) {
        this.pendingChanges =
          this.pendingChanges?.compose(this.dirtyChanges) ?? this.dirtyChanges;
        this.dirtyChanges = this.newEmptyChangeSet();
      }
    },
    applyBeforeDirtyChangesExclusive: (changeSpec: ChangeSpec): void => {
      if (!this.locekdForUpdate) {
        throw new Error('Cannot update state without locking the mutex');
      }
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
    },
  };

  private readonly lockedState: LockedState = {
    updateExclusive: (
      callback: (pendingUpdate: PendingUpdate) => Promise<string | undefined>,
    ): Promise<void> => {
      return this.lockedState.withUpdateExclusive<void>(
        async (pendingUpdate) => {
          const newStateId = await callback(pendingUpdate);
          return { newStateId, data: undefined };
        },
      );
    },
    withUpdateExclusive: async <T>(
      callback: (pendingUpdate: PendingUpdate) => Promise<StateUpdateResult<T>>,
    ): Promise<T> => {
      if (!this.locekdForUpdate) {
        throw new Error('Cannot update state without locking the mutex');
      }
      if (this.pendingChanges !== undefined) {
        throw new Error('Delta updates are not reentrant');
      }
      let newStateId: string | undefined;
      let data: T;
      try {
        ({ newStateId, data } = await callback(this.pendingUpdate));
      } catch (e) {
        log.error('Error while update', e);
        this.cancelUpdate();
        throw e;
      }
      if (newStateId === undefined) {
        this.cancelUpdate();
      } else {
        this.xtextStateId = newStateId;
        this.pendingChanges = undefined;
      }
      return data;
    },
  };

  private cancelUpdate(): void {
    if (this.pendingChanges === undefined) {
      return;
    }
    this.dirtyChanges = this.pendingChanges.compose(this.dirtyChanges);
    this.pendingChanges = undefined;
  }

  runExclusive<T>(
    callback: (lockedState: LockedState) => Promise<T>,
  ): Promise<T> {
    return this.mutex.runExclusive(async () => {
      if (this.pendingChanges !== undefined) {
        throw new Error('Update is pending before entering critical section');
      }
      const result = await callback(this.lockedState);
      if (this.pendingChanges !== undefined) {
        throw new Error('Update is pending after entering critical section');
      }
      return result;
    });
  }

  runExclusiveHighPriority<T>(
    callback: (lockedState: LockedState) => Promise<T>,
  ): Promise<T> {
    this.mutex.cancel();
    return this.runExclusive(callback);
  }

  async runExclusiveWithRetries<T>(
    callback: (lockedState: LockedState) => Promise<T>,
    maxRetries = 5,
  ): Promise<T> {
    let retries = 0;
    while (retries < maxRetries) {
      try {
        // eslint-disable-next-line no-await-in-loop -- Use a loop for sequential retries.
        return await this.runExclusive(callback);
      } catch (error) {
        // Let timeout errors propagate to give up retrying on a flaky connection.
        if (error !== E_CANCELED) {
          throw error;
        }
        retries += 1;
      }
    }
    throw E_CANCELED;
  }
}
