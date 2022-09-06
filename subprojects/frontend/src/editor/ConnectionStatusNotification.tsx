import Button from '@mui/material/Button';
import { observer } from 'mobx-react-lite';
import {
  useSnackbar,
  type SnackbarKey,
  type SnackbarMessage,
  type OptionsObject,
} from 'notistack';
import React, { useEffect } from 'react';

import { ContrastThemeProvider } from '../theme/ThemeProvider';

import type EditorStore from './EditorStore';

const DEBOUNCE_TIMEOUT = 350;

function enqueueLater(
  enqueueSnackbar: (
    message: SnackbarMessage,
    options: OptionsObject | undefined,
  ) => SnackbarKey,
  closeSnackbar: (key: SnackbarKey) => void,
  message: SnackbarMessage,
  options?: OptionsObject | undefined,
  debounceTimeout = DEBOUNCE_TIMEOUT,
): () => void {
  let key: SnackbarKey | undefined;
  let timeout: number | undefined = setTimeout(() => {
    timeout = undefined;
    key = enqueueSnackbar(message, options);
  }, debounceTimeout);
  return () => {
    if (timeout !== undefined) {
      clearTimeout(timeout);
    }
    if (key !== undefined) {
      closeSnackbar(key);
    }
  };
}

export default observer(function ConnectionStatusNotification({
  editorStore,
}: {
  editorStore: EditorStore;
}): null {
  const {
    opened,
    opening,
    connectionErrors,
    disconnectedByUser,
    networkMissing,
  } = editorStore;
  const { enqueueSnackbar, closeSnackbar } = useSnackbar();

  useEffect(() => {
    if (opening) {
      return enqueueLater(
        enqueueSnackbar,
        closeSnackbar,
        'Connecting to Refinery',
        {
          persist: true,
          action: (
            <Button onClick={() => editorStore.disconnect()} color="inherit">
              Cancel
            </Button>
          ),
        },
        500,
      );
    }

    if (connectionErrors.length >= 1 && !opening) {
      return enqueueLater(
        enqueueSnackbar,
        closeSnackbar,
        <div>
          Connection error:{' '}
          <b>{connectionErrors[connectionErrors.length - 1]}</b>
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
            <Button onClick={() => editorStore.disconnect()} color="inherit">
              Disconnect
            </Button>
          ) : (
            <>
              <Button onClick={() => editorStore.connect()} color="inherit">
                Reconnect
              </Button>
              <Button onClick={() => editorStore.disconnect()} color="inherit">
                Cancel
              </Button>
            </>
          ),
        },
      );
    }

    if (networkMissing) {
      if (disconnectedByUser) {
        return enqueueLater(
          enqueueSnackbar,
          closeSnackbar,
          <div>
            <b>No network connection:</b> Some editing features might be
            degraded
          </div>,
          {
            action: (
              <ContrastThemeProvider>
                <Button onClick={() => editorStore.connect()} color="primary">
                  Try reconnecting
                </Button>
              </ContrastThemeProvider>
            ),
          },
          0,
        );
      }

      return enqueueLater(
        enqueueSnackbar,
        closeSnackbar,
        <div>
          <b>No network connection:</b> Refinery will try to reconnect when the
          connection is restored
        </div>,
        {
          persist: true,
          action: (
            <ContrastThemeProvider>
              <Button onClick={() => editorStore.connect()} color="primary">
                Try now
              </Button>
              <Button onClick={() => editorStore.disconnect()} color="inherit">
                Cancel
              </Button>
            </ContrastThemeProvider>
          ),
        },
      );
    }

    if (disconnectedByUser) {
      return enqueueLater(
        enqueueSnackbar,
        closeSnackbar,
        <div>
          <b>Not connected to Refinery:</b> Some editing features might be
          degraded
        </div>,
        {
          action: (
            <ContrastThemeProvider>
              <Button onClick={() => editorStore.connect()} color="primary">
                Reconnect
              </Button>
            </ContrastThemeProvider>
          ),
        },
        0,
      );
    }

    return () => {};
  }, [
    editorStore,
    opened,
    opening,
    connectionErrors,
    disconnectedByUser,
    networkMissing,
    closeSnackbar,
    enqueueSnackbar,
  ]);

  return null;
});
