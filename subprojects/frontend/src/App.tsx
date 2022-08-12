import Box from '@mui/material/Box';
import React from 'react';

import TopBar from './TopBar';
import EditorArea from './editor/EditorArea';
import EditorButtons from './editor/EditorButtons';
import GenerateButton from './editor/GenerateButton';

export default function App(): JSX.Element {
  return (
    <Box display="flex" flexDirection="column" sx={{ height: '100vh' }}>
      <TopBar />
      <Box
        display="flex"
        justifyContent="space-between"
        alignItems="center"
        p={1}
      >
        <EditorButtons />
        <GenerateButton />
      </Box>
      <Box flexGrow={1} flexShrink={1} sx={{ overflow: 'auto' }}>
        <EditorArea />
      </Box>
    </Box>
  );
}
