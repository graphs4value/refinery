import { getLogger } from 'loglevel';
import { makeObservable, observable, runInAction } from 'mobx';
import React, { createContext, useContext } from 'react';

import type EditorStore from './editor/EditorStore';
import ThemeStore from './theme/ThemeStore';

const log = getLogger('RootStore');

export default class RootStore {
  editorStore: EditorStore | undefined;

  readonly themeStore: ThemeStore;

  constructor(initialValue: string) {
    this.themeStore = new ThemeStore();
    makeObservable(this, {
      editorStore: observable,
    });
    import('./editor/EditorStore')
      .then(({ default: EditorStore }) => {
        runInAction(() => {
          this.editorStore = new EditorStore(initialValue);
        });
      })
      .catch((error) => {
        log.error('Failed to load EditorStore', error);
      });
  }
}

const StoreContext = createContext<RootStore | undefined>(undefined);

export interface RootStoreProviderProps {
  children: JSX.Element;

  rootStore: RootStore;
}

export function RootStoreProvider({
  children,
  rootStore,
}: RootStoreProviderProps): JSX.Element {
  return (
    <StoreContext.Provider value={rootStore}>{children}</StoreContext.Provider>
  );
}

export const useRootStore = (): RootStore => {
  const rootStore = useContext(StoreContext);
  if (!rootStore) {
    throw new Error('useRootStore must be used within RootStoreProvider');
  }
  return rootStore;
};
