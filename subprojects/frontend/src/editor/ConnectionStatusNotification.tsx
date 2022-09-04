import Button from '@mui/material/Button';
import { observer } from 'mobx-react-lite';
import { type SnackbarKey, useSnackbar } from 'notistack';
import React, { useEffect } from 'react';

import { ContrastThemeProvider } from '../theme/ThemeProvider';

import type EditorStore from './EditorStore';

const CONNECTING_DEBOUNCE_TIMEOUT = 250;

export default observer(function ConnectionStatusNotification({
  editorStore,
}: {
  editorStore: EditorStore;
}): null {
  const { opened, opening, connectionErrors } = editorStore;
  const { enqueueSnackbar, closeSnackbar } = useSnackbar();

  useEffect(() => {
    if (opening) {
      let key: SnackbarKey | undefined;
      let timeout: number | undefined = setTimeout(() => {
        timeout = undefined;
        key = enqueueSnackbar('Connecting to Refinery', {
          persist: true,
          action: (
            <Button onClick={() => editorStore.disconnect()} color="inherit">
              Cancel
            </Button>
          ),
        });
      }, CONNECTING_DEBOUNCE_TIMEOUT);
      return () => {
        if (timeout !== undefined) {
          clearTimeout(timeout);
        }
        if (key !== undefined) {
          closeSnackbar(key);
        }
      };
    }

    if (connectionErrors.length >= 1) {
      const key = enqueueSnackbar(
        <div>
          Connection error: <b>{connectionErrors[0]}</b>
          {connectionErrors.length >= 2 && (
            <>
              {' '}
              and <b>{connectionErrors.length - 1}</b> more{' '}
              {connectionErrors.length >= 3 ? 'errors' : 'error'}
            </>
          )}
        </div>,
        {
          persist: !opened,
          variant: 'error',
          action: opened ? (
            <ContrastThemeProvider>
              <Button onClick={() => editorStore.disconnect()} color="inherit">
                Disconnect
              </Button>
            </ContrastThemeProvider>
          ) : (
            <ContrastThemeProvider>
              <Button onClick={() => editorStore.connect()} color="inherit">
                Reconnect
              </Button>
              <Button onClick={() => editorStore.disconnect()} color="inherit">
                Cancel
              </Button>
            </ContrastThemeProvider>
          ),
        },
      );
      return () => closeSnackbar(key);
    }

    if (!opened) {
      const key = enqueueSnackbar(
        <div>
          <b>Not connected to Refinery:</b> Some editing features might be
          degraded
        </div>,
        {
          action: (
            <ContrastThemeProvider>
              <Button onClick={() => editorStore.connect()}>Reconnect</Button>
            </ContrastThemeProvider>
          ),
        },
      );
      return () => closeSnackbar(key);
    }

    return () => {};
  }, [
    editorStore,
    opened,
    opening,
    connectionErrors,
    closeSnackbar,
    enqueueSnackbar,
  ]);

  return null;
});
