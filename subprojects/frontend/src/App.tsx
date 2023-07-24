/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import Box from '@mui/material/Box';
import CssBaseline from '@mui/material/CssBaseline';
import { throttle } from 'lodash-es';
import {
  StrictMode,
  Suspense,
  lazy,
  useState,
  useEffect,
  useMemo,
} from 'react';

import Loading from './Loading';
import type RootStore from './RootStore';
import RootStoreProvider from './RootStoreProvider';
import WindowControlsOverlayColor from './WindowControlsOverlayColor';
import ThemeProvider from './theme/ThemeProvider';

const Refinery = lazy(() => import('./Refinery.js'));

function useInnerHeight(): number {
  const [innerHeight, setInnerHeight] = useState(window.innerHeight);
  const resizeHandler = useMemo(
    () => throttle(() => setInnerHeight(window.innerHeight), 250),
    [],
  );
  useEffect(() => {
    window.addEventListener('resize', resizeHandler, { passive: true });
    return () => {
      window.removeEventListener('resize', resizeHandler);
      resizeHandler.cancel();
    };
  }, [resizeHandler]);
  return innerHeight;
}

export default function App({
  rootStore,
}: {
  rootStore: RootStore;
}): JSX.Element {
  // See https://css-tricks.com/the-trick-to-viewport-units-on-mobile/
  const innerHeight = useInnerHeight();

  return (
    <StrictMode>
      <RootStoreProvider rootStore={rootStore}>
        <ThemeProvider>
          <CssBaseline enableColorScheme />
          <WindowControlsOverlayColor />
          <Box height={`${innerHeight}px`} overflow="hidden">
            <Suspense fallback={<Loading />}>
              <Refinery />
            </Suspense>
          </Box>
        </ThemeProvider>
      </RootStoreProvider>
    </StrictMode>
  );
}
