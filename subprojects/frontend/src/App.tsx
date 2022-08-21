import Stack from '@mui/material/Stack';
import React from 'react';

import TopBar from './TopBar';
import EditorPane from './editor/EditorPane';

export default function App(): JSX.Element {
  return (
    <Stack direction="column" height="100vh" overflow="auto">
      <TopBar />
      <EditorPane />
    </Stack>
  );
}
