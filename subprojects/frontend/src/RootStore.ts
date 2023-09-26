/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { getLogger } from 'loglevel';
import { makeAutoObservable, runInAction } from 'mobx';

import PWAStore from './PWAStore';
import type EditorStore from './editor/EditorStore';
import Compressor from './persistence/Compressor';
import ThemeStore from './theme/ThemeStore';

const log = getLogger('RootStore');

export default class RootStore {
  private readonly compressor = new Compressor((text) =>
    this.setInitialValue(text),
  );

  private initialValue: string | undefined;

  private editorStoreClass: typeof EditorStore | undefined;

  editorStore: EditorStore | undefined;

  readonly pwaStore: PWAStore;

  readonly themeStore: ThemeStore;

  disposed = false;

  constructor() {
    this.pwaStore = new PWAStore();
    this.themeStore = new ThemeStore();
    makeAutoObservable<RootStore, 'compressor' | 'editorStoreClass'>(this, {
      compressor: false,
      editorStoreClass: false,
      pwaStore: false,
      themeStore: false,
    });
    (async () => {
      const { default: EditorStore } = await import('./editor/EditorStore');
      runInAction(() => {
        if (this.disposed) {
          return;
        }
        this.editorStoreClass = EditorStore;
        if (this.initialValue !== undefined) {
          this.setInitialValue(this.initialValue);
        }
      });
    })().catch((error) => {
      log.error('Failed to load EditorStore', error);
    });
    this.compressor.decompressInitial();
  }

  private setInitialValue(initialValue: string): void {
    this.initialValue = initialValue;
    if (this.editorStoreClass !== undefined) {
      const EditorStore = this.editorStoreClass;
      this.editorStore = new EditorStore(
        this.initialValue,
        this.pwaStore,
        (text) => this.compressor.compress(text),
      );
    }
  }

  dispose(): void {
    if (this.disposed) {
      return;
    }
    this.editorStore?.dispose();
    this.compressor.dispose();
    this.disposed = true;
  }
}
