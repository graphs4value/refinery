/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import Box from '@mui/material/Box';
import Skeleton from '@mui/material/Skeleton';
import Stack from '@mui/material/Stack';
import Toolbar from '@mui/material/Toolbar';
import { observer } from 'mobx-react-lite';
import { useState } from 'react';
import { useResizeDetector } from 'react-resize-detector';

import { useRootStore } from '../RootStoreProvider';

import AnalysisErrorNotification from './AnalysisErrorNotification';
import ConnectionStatusNotification from './ConnectionStatusNotification';
import EditorArea from './EditorArea';
import EditorButtons from './EditorButtons';
import SearchPanelPortal from './SearchPanelPortal';

function EditorLoading(): JSX.Element {
  const [skeletonSizes] = useState(() =>
    new Array(10).fill(0).map(() => Math.random() * 60 + 10),
  );

  return (
    <Box mx={2} width="100%">
      {skeletonSizes.map((length, i) => (
        /* eslint-disable-next-line react/no-array-index-key --
          Random placeholders have no identity.
        */
        <Skeleton key={i} width={`${length}%`} />
      ))}
    </Box>
  );
}

export default observer(function EditorPane(): JSX.Element {
  const { editorStore } = useRootStore();
  const { width, ref } = useResizeDetector();

  return (
    <Stack direction="column" height="100%" overflow="auto" ref={ref}>
      <Toolbar
        variant="dense"
        sx={{
          overflowY: 'scroll',
          scrollbarWidth: 'none',
          '::-webkit-scrollbar': {
            background: 'transparent',
            width: 0,
            height: 0,
          },
        }}
      >
        <EditorButtons editorStore={editorStore} />
      </Toolbar>
      <Box display="flex" flexGrow={1} flexShrink={1} overflow="auto">
        {editorStore === undefined ? (
          <EditorLoading />
        ) : (
          <>
            <AnalysisErrorNotification editorStore={editorStore} />
            <ConnectionStatusNotification editorStore={editorStore} />
            <SearchPanelPortal editorStore={editorStore} width={width} />
            <EditorArea editorStore={editorStore} />
          </>
        )}
      </Box>
    </Stack>
  );
});
