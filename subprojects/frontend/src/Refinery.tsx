/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import Grow from '@mui/material/Grow';
import Stack from '@mui/material/Stack';
import { SnackbarProvider } from 'notistack';

import TopBar from './TopBar';
import UpdateNotification from './UpdateNotification';
import WorkArea from './WorkArea';
import ConfirmationDialog from './dialog/ConfirmationDialog';
import ErrorDialog from './dialog/ErrorDialog';

export default function Refinery(): React.ReactElement {
  return (
    <SnackbarProvider TransitionComponent={Grow}>
      <UpdateNotification />
      <ConfirmationDialog />
      <ErrorDialog />
      <Stack direction="column" sx={{ height: '100%', overflow: 'auto' }}>
        <TopBar />
        <WorkArea />
      </Stack>
    </SnackbarProvider>
  );
}
