/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { makeAutoObservable } from 'mobx';

import type { WindowState } from '../settings';

export default class WindowStore {
  private readonly reactionDisposers = new Set<() => void>();

  private disposed = false;

  modalDialogCount = 0;

  unsavedChanges = false;

  filePath: string | undefined;

  hash: string | undefined;

  width: number;

  height: number;

  maximized: boolean;

  constructor({ width, height, maximized }: WindowState) {
    this.width = width;
    this.height = height;
    this.maximized = maximized;
    makeAutoObservable<WindowStore, 'disposed' | 'reactionDisposers'>(this, {
      disposed: false,
      reactionDisposers: false,
      addReactionDisposer: false,
      dispose: false,
    });
  }

  setModalDialogCount(modalDialogCount: number): void {
    this.modalDialogCount = modalDialogCount;
  }

  setUnsavedChanges(unsavedChanges: boolean): void {
    this.unsavedChanges = unsavedChanges;
  }

  setFilePath(filePath: string | undefined): void {
    this.filePath = filePath;
    this.hash = undefined;
  }

  setHash(hash: string): void {
    this.filePath = undefined;
    this.hash = hash;
  }

  setWindowState({ width, height, maximized }: WindowState): void {
    this.width = width;
    this.height = height;
    this.maximized = maximized;
  }

  get windowState(): WindowState {
    return {
      width: this.width,
      height: this.height,
      maximized: this.maximized,
    };
  }

  addReactionDisposer(disposer: () => void): void {
    if (this.disposed) {
      disposer();
      return;
    }
    this.reactionDisposers.add(disposer);
  }

  dispose(): void {
    if (this.disposed) {
      return;
    }
    this.disposed = true;
    for (const disposer of this.reactionDisposers) {
      disposer();
    }
    this.reactionDisposers.clear();
  }
}
