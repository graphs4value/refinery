/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Typography from '@mui/material/Typography';
import { observer } from 'mobx-react-lite';

import { useRootStore } from '../RootStoreProvider';

import Dialog from './Dialog';
import DialogActionBar from './DialogActionBar';
import DialogTitleBar from './DialogTitleBar';

export default observer(function ErrorDialog(): React.ReactElement {
  const rootStore = useRootStore();
  const { errorDialog } = rootStore;
  const fatal = errorDialog?.fatal ?? false;
  const close = () => rootStore.closeErrorDialog();

  return (
    <Dialog
      open={errorDialog !== undefined}
      onClose={fatal ? undefined : close}
      maxWidth="sm"
    >
      {errorDialog !== undefined && (
        <>
          <DialogTitleBar
            close={errorDialog.fatal ? undefined : close}
            title={errorDialog.title}
          />
          <Box sx={{ p: 2 }}>
            <Typography>{errorDialog.body}</Typography>
          </Box>
          {errorDialog.fatal && (
            <DialogActionBar>
              <Button color="error" onClick={close}>
                Close window
              </Button>
            </DialogActionBar>
          )}
        </>
      )}
    </Dialog>
  );
});
