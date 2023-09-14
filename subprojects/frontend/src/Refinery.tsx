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

export default function Refinery(): JSX.Element {
  return (
    <SnackbarProvider TransitionComponent={Grow}>
      <UpdateNotification />
      <Stack direction="column" height="100%" overflow="auto">
        <TopBar />
        <WorkArea />
      </Stack>
    </SnackbarProvider>
  );
}
