import Box from '@mui/material/Box';
import Skeleton from '@mui/material/Skeleton';
import Stack from '@mui/material/Stack';
import Toolbar from '@mui/material/Toolbar';
import { useTheme } from '@mui/material/styles';
import useMediaQuery from '@mui/material/useMediaQuery';
import { observer } from 'mobx-react-lite';
import React, { useState } from 'react';

import { useRootStore } from '../RootStoreProvider';

import ConnectionStatusNotification from './ConnectionStatusNotification';
import EditorArea from './EditorArea';
import EditorButtons from './EditorButtons';
import GenerateButton from './GenerateButton';
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
  const { breakpoints } = useTheme();
  const showGenerateButton = useMediaQuery(breakpoints.up('sm'));

  return (
    <Stack direction="column" flexGrow={1} flexShrink={1} overflow="auto">
      <Toolbar variant="dense">
        <EditorButtons editorStore={editorStore} />
        {showGenerateButton && <GenerateButton editorStore={editorStore} />}
      </Toolbar>
      <Box display="flex" flexGrow={1} flexShrink={1} overflow="auto">
        {editorStore === undefined ? (
          <EditorLoading />
        ) : (
          <>
            <ConnectionStatusNotification editorStore={editorStore} />
            <SearchPanelPortal editorStore={editorStore} />
            <EditorArea editorStore={editorStore} />
          </>
        )}
      </Box>
    </Stack>
  );
});
