import Box from '@mui/material/Box';
import Toolbar from '@mui/material/Toolbar';
import React from 'react';

import TopBar from './TopBar';
import EditorArea from './editor/EditorArea';
import EditorButtons from './editor/EditorButtons';
import GenerateButton from './editor/GenerateButton';

export default function App(): JSX.Element {
  return (
    <Box display="flex" flexDirection="column" sx={{ height: '100vh' }}>
      <TopBar />
      <Toolbar variant="dense">
        <EditorButtons />
        <GenerateButton />
      </Toolbar>
      <Box flexGrow={1} flexShrink={1} overflow="auto">
        <EditorArea />
      </Box>
    </Box>
  );
}
