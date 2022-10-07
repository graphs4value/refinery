import Box from '@mui/material/Box';
import CssBaseline from '@mui/material/CssBaseline';
import React, { StrictMode, Suspense, lazy } from 'react';

import Loading from './Loading';
import type RootStore from './RootStore';
import RootStoreProvider from './RootStoreProvider';
import WindowControlsOverlayColor from './WindowControlsOverlayColor';
import ThemeProvider from './theme/ThemeProvider';

const Refinery = lazy(() => import('./Refinery.js'));

export default function App({
  rootStore,
}: {
  rootStore: RootStore;
}): JSX.Element {
  return (
    <StrictMode>
      <RootStoreProvider rootStore={rootStore}>
        <ThemeProvider>
          <CssBaseline enableColorScheme />
          <WindowControlsOverlayColor />
          <Box height="100vh" overflow="auto">
            <Suspense fallback={<Loading />}>
              <Refinery />
            </Suspense>
          </Box>
        </ThemeProvider>
      </RootStoreProvider>
    </StrictMode>
  );
}
