import Box from '@mui/material/Box';
import Stack from '@mui/material/Stack';
import Toolbar from '@mui/material/Toolbar';
import { observer } from 'mobx-react-lite';
import React from 'react';

import Loading from '../Loading';
import { useRootStore } from '../RootStore';

import EditorArea from './EditorArea';
import EditorButtons from './EditorButtons';
import GenerateButton from './GenerateButton';
import SearchPanelPortal from './SearchPanelPortal';

function EditorPane(): JSX.Element {
  const { editorStore } = useRootStore();

  return (
    <Stack direction="column" flexGrow={1} flexShrink={1} overflow="auto">
      <Toolbar variant="dense">
        <EditorButtons editorStore={editorStore} />
        <GenerateButton editorStore={editorStore} />
      </Toolbar>
      <Box flexGrow={1} flexShrink={1} overflow="auto">
        {editorStore === undefined ? (
          <Loading />
        ) : (
          <>
            <SearchPanelPortal editorStore={editorStore} />
            <EditorArea editorStore={editorStore} />
          </>
        )}
      </Box>
    </Stack>
  );
}

export default observer(EditorPane);
