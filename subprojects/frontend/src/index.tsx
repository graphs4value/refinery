import Box from '@mui/material/Box';
import CssBaseline from '@mui/material/CssBaseline';
import { configure } from 'mobx';
import React, { Suspense, lazy } from 'react';
import { createRoot } from 'react-dom/client';

import Loading from './Loading';
import RootStore, { RootStoreProvider } from './RootStore';
import WindowControlsOverlayColor from './WindowControlsOverlayColor';
import ThemeProvider from './theme/ThemeProvider';
import getLogger from './utils/getLogger';

const log = getLogger('index');

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
    child, student, adult, retired
}

% A child cannot have any dependents.
pred invalidTaxStatus(Person p) <->
    taxStatus(p, child),
    children(p, _q)
;
    taxStatus(p, retired),
    parent(p, q),
    !taxStatus(q, retired).

indiv family.
Family(family).
members(family, anne).
members(family, bob).
members(family, ciri).
children(anne, ciri).
?children(bob, ciri).
default children(ciri, *): false.
taxStatus(anne, adult).
age(anne, 35).
bobAge: 27.
age(bob, bobAge).
!age(ciri, bobAge).

scope Family = 1, Person += 5..10.
`;

configure({
  enforceActions: 'always',
});

const rootStore = new RootStore(initialValue);

const App = lazy(() => import('./App.js'));

const app = (
  <React.StrictMode>
    <RootStoreProvider rootStore={rootStore}>
      <ThemeProvider>
        <CssBaseline enableColorScheme />
        <WindowControlsOverlayColor />
        <Box height="100vh" overflow="auto">
          <Suspense fallback={<Loading />}>
            <App />
          </Suspense>
        </Box>
      </ThemeProvider>
    </RootStoreProvider>
  </React.StrictMode>
);

const rootElement = document.getElementById('app');
if (rootElement === null) {
  log.error('Root element not found');
} else {
  const root = createRoot(rootElement);
  root.render(app);
}
