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

const initialValue = `% Metamodel

abstract class CompositeElement {
    contains Region[] regions
}

class Region {
    contains Vertex[] vertices opposite region
}

abstract class Vertex {
    container Region region opposite vertices
    contains Transition[] outgoingTransition opposite source
    Transition[] incomingTransition opposite target
}

class Transition {
    container Vertex source opposite outgoingTransition
    Vertex[1] target opposite incomingTransition
}

abstract class Pseudostate extends Vertex.

abstract class RegularState extends Vertex.

class Entry extends Pseudostate.

class Exit extends Pseudostate.

class Choice extends Pseudostate.

class FinalState extends RegularState.

class State extends RegularState, CompositeElement.

class Statechart extends CompositeElement.

% Constraints

%% Entry

pred entryInRegion(Region r, Entry e) <->
    vertices(r, e).

error noEntryInRegion(Region r) <->
    !entryInRegion(r, _).

error multipleEntryInRegion(Region r) <->
    entryInRegion(r, e1),
    entryInRegion(r, e2),
    e1 != e2.

error incomingToEntry(Transition t, Entry e) <->
    target(t, e).

error noOutgoingTransitionFromEntry(Entry e) <->
    !source(_, e).

error multipleTransitionFromEntry(Entry e, Transition t1, Transition t2) <->
    outgoingTransition(e, t1),
    outgoingTransition(e, t2),
    t1 != t2.

%% Exit

error outgoingFromExit(Transition t, Exit e) <->
    source(t, e).

%% Final

error outgoingFromFinal(Transition t, FinalState e) <->
    source(t, e).

%% State vs Region

pred stateInRegion(Region r, State s) <->
    vertices(r, s).

error noStateInRegion(Region r) <->
    !stateInRegion(r, _).

%% Choice

error choiceHasNoOutgoing(Choice c) <->
    !source(_, c).

error choiceHasNoIncoming(Choice c) <->
    !target(_, c).

% Instance model

Statechart(sct).

% Scope

scope node = 20..30, Region = 2..*, Choice = 1..*, Statechart += 0.
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
