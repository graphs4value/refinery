/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import Box from '@mui/material/Box';
import CssBaseline from '@mui/material/CssBaseline';
import { styled, ThemeProvider } from '@mui/material/styles';
import { configure } from 'mobx';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';

import { lightTheme } from '../theme/ThemeProvider';
import getLogger from '../utils/getLogger';

import HeadlessApp from './HeadlessApp';
import HeadlessStore from './HeadlessStore';
import serveIPCRequests from './serveIPCRequests';

const logger = getLogger('headless');

// Make sure `styled` ends up in the entry chunk.
// https://github.com/mui/material-ui/issues/32727#issuecomment-1659945548
(window as unknown as { fixViteIssue: unknown }).fixViteIssue = styled;

configure({
  enforceActions: 'always',
});

const store = new HeadlessStore();

// `dotSource.ts` measures node and relation labels by rendering them into a
// hidden `<div>` and reading back `getBoundingClientRect()`, which is
// synchronous and does not wait for `font-display: swap` to swap in the real
// font. Relation labels for abstract classes are rendered in italics, so
// unless we force both the normal and the italic face to be fetched up
// front, a request that renders its first italic label before either face
// happens to have been used elsewhere gets measured against the fallback
// font instead.
Promise.all([
  document.fonts.load('400 12pt "Open Sans Variable"'),
  document.fonts.load('italic 400 12pt "Open Sans Variable"'),
])
  .catch((err: unknown) => {
    logger.error({ err }, 'Failed to preload fonts');
  })
  .finally(() => serveIPCRequests(store));

document.addEventListener('DOMContentLoaded', () => {
  const rootElement = document.getElementById('app');
  if (rootElement !== null) {
    const root = createRoot(rootElement);
    root.render(
      <StrictMode>
        <ThemeProvider theme={lightTheme}>
          <CssBaseline enableColorScheme />
          <Box>
            <HeadlessApp store={store} />
          </Box>
        </ThemeProvider>
      </StrictMode>,
    );
  }
});
