import Grow from '@mui/material/Grow';
import Stack from '@mui/material/Stack';
import { SnackbarProvider } from 'notistack';
import React from 'react';

import TopBar from './TopBar';
import UpdateNotification from './UpdateNotification';
import EditorPane from './editor/EditorPane';

export default function Refinery(): JSX.Element {
  return (
    // @ts-expect-error -- notistack has problems with `exactOptionalPropertyTypes
    <SnackbarProvider TransitionComponent={Grow}>
      <UpdateNotification />
      <Stack direction="column" height="100vh" overflow="auto">
        <TopBar />
        <EditorPane />
      </Stack>
    </SnackbarProvider>
  );
}
