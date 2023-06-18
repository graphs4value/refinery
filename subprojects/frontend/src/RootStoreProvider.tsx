/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { type ReactNode, createContext, useContext } from 'react';

import type RootStore from './RootStore';

const StoreContext = createContext<RootStore | undefined>(undefined);

export function useRootStore(): RootStore {
  const rootStore = useContext(StoreContext);
  if (!rootStore) {
    throw new Error('useRootStore must be used within RootStoreProvider');
  }
  return rootStore;
}

export default function RootStoreProvider({
  children,
  rootStore,
}: {
  children?: ReactNode;
  rootStore: RootStore;
}): JSX.Element {
  return (
    <StoreContext.Provider value={rootStore}>{children}</StoreContext.Provider>
  );
}

RootStoreProvider.defaultProps = {
  children: undefined,
};
