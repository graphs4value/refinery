import { configure } from 'mobx';
import { type Root, createRoot } from 'react-dom/client';

import App from './App';
import RootStore from './RootStore';

const initialValue = `class Family {
    contains Person[] members
}

class Person {
    Person[] children opposite parent
    Person[0..1] parent opposite children
    int age
    TaxStatus taxStatus
}

enum TaxStatus {
    CHILD, STUDENT, ADULT, RETIRED
}

% A child cannot have any dependents.
pred invalidTaxStatus(Person p) <->
    taxStatus(p, CHILD),
    children(p, _q)
;
    parent(p, q),
    age(q) < age(p)
;
    taxStatus(p, RETIRED),
    parent(p, q),
    !taxStatus(q, RETIRED).

indiv family.
Family(family).
members(family, anne).
members(family, bob).
members(family, ciri).
children(anne, ciri).
?children(bob, ciri).
default children(ciri, *): false.
taxStatus(anne, ADULT).
age(bob): 21..35.
age(ciri): 10.

scope Family = 1, Person += 5..10.
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
