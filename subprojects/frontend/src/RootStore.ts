import { getLogger } from 'loglevel';
import { makeAutoObservable, runInAction } from 'mobx';

import PWAStore from './PWAStore';
import type EditorStore from './editor/EditorStore';
import ThemeStore from './theme/ThemeStore';

const log = getLogger('RootStore');

export default class RootStore {
  editorStore: EditorStore | undefined;

  readonly pwaStore: PWAStore;

  readonly themeStore: ThemeStore;

  disposed = false;

  constructor(initialValue: string) {
    this.pwaStore = new PWAStore();
    this.themeStore = new ThemeStore();
    makeAutoObservable(this, {
      pwaStore: false,
      themeStore: false,
    });
    (async () => {
      const { default: EditorStore } = await import('./editor/EditorStore');
      runInAction(() => {
        if (this.disposed) {
          return;
        }
        this.editorStore = new EditorStore(initialValue, this.pwaStore);
      });
    })().catch((error) => {
      log.error('Failed to load EditorStore', error);
    });
  }

  dispose(): void {
    if (this.disposed) {
      return;
    }
    this.editorStore?.dispose();
    this.disposed = true;
  }
}
