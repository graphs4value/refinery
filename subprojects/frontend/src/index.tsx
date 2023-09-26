/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { styled } from '@mui/material/styles';
import { configure } from 'mobx';
import { type Root, createRoot } from 'react-dom/client';

import App from './App';
import RootStore from './RootStore';

// Make sure `styled` ends up in the entry chunk.
// https://github.com/mui/material-ui/issues/32727#issuecomment-1659945548
(window as unknown as { fixViteIssue: unknown }).fixViteIssue = styled;

configure({
  enforceActions: 'always',
});

let HotRootStore = RootStore;
let HotApp = App;

function createStore(): RootStore {
  return new HotRootStore();
}

let rootStore = createStore();

let root: Root | undefined;

function render(): void {
  root?.render(<HotApp rootStore={rootStore} />);
}

if (import.meta.hot) {
  import.meta.hot.accept('./App', (module) => {
    if (module === undefined) {
      return;
    }
    ({ default: HotApp } = module as unknown as typeof import('./App'));
    render();
  });
  import.meta.hot.accept('./RootStore', (module) => {
    if (module === undefined) {
      return;
    }
    ({ default: HotRootStore } =
      module as unknown as typeof import('./RootStore'));
    rootStore.dispose();
    rootStore = createStore();
    render();
  });
}

document.addEventListener('DOMContentLoaded', () => {
  const rootElement = document.getElementById('app');
  if (rootElement !== null) {
    root = createRoot(rootElement);
    render();
  }
});
