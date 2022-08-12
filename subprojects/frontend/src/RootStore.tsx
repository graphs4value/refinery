import React, { createContext, useContext } from 'react';

import EditorStore from './editor/EditorStore';
import ThemeStore from './theme/ThemeStore';

export default class RootStore {
  editorStore;

  themeStore;

  constructor(initialValue: string) {
    this.themeStore = new ThemeStore();
    this.editorStore = new EditorStore(initialValue, this.themeStore);
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
