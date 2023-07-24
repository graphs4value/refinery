/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import Button from '@mui/material/Button';
import { observer } from 'mobx-react-lite';
import { useEffect } from 'react';

import { ContrastThemeProvider } from '../theme/ThemeProvider';
import useDelayedSnackbar from '../utils/useDelayedSnackbar';

import type EditorStore from './EditorStore';

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
  const enqueueLater = useDelayedSnackbar(350);

  useEffect(() => {
    if (opening) {
      return enqueueLater(
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
    enqueueLater,
  ]);

  return null;
});
