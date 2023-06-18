/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { configure } from 'mobx';
import { type Root, createRoot } from 'react-dom/client';

import App from './App';
import RootStore from './RootStore';

const initialValue = `// Metamodel
class Person {
    Person[] friend opposite friend
}

class Post {
    Person author
    Post[0..1] replyTo
}

// Constraints
error replyToNotFriend(Post x, Post y) <->
    replyTo(x, y),
    author(x, xAuthor),
    author(y, yAuthor),
    !friend(xAuthor, yAuthor).

error replyToCycle(Post x) <-> replyTo+(x,x).

// Instance model
Person(a).
Person(b).
friend(a, b).
friend(b, a).
Post(p1).
author(p1, a).
Post(p2).
author(p2, b).
replyTo(p2, p1).

!author(Post::new, a). // Automatically inferred: author(Post::new, b).

// Scope
scope Post = 10..15, Person += 0.
`;

configure({
  enforceActions: 'always',
});

let HotRootStore = RootStore;
let HotApp = App;

function createStore(): RootStore {
  return new HotRootStore(initialValue);
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
