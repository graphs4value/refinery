import Button from '@mui/material/Button';
import {
  type OptionsObject as SnackbarOptionsObject,
  useSnackbar,
} from 'notistack';
import React, { useEffect } from 'react';
// eslint-disable-next-line import/no-unresolved -- Importing virtual module.
import { registerSW } from 'virtual:pwa-register';

import { ContrastThemeProvider } from './theme/ThemeProvider';
import getLogger from './utils/getLogger';

const log = getLogger('RegisterServiceWorker');

function UpdateSnackbarActions({
  closeCurrentSnackbar,
  enqueueSnackbar,
  updateSW,
}: {
  closeCurrentSnackbar: () => void;
  enqueueSnackbar: (
    message: string,
    options?: SnackbarOptionsObject | undefined,
  ) => void;
  updateSW: (reloadPage: boolean) => Promise<void>;
}): JSX.Element {
  return (
    <ContrastThemeProvider>
      <Button
        color="primary"
        onClick={() => {
          closeCurrentSnackbar();
          updateSW(true).catch((error) => {
            log.error('Failed to update service worker', error);
            enqueueSnackbar('Failed to download update', {
              variant: 'error',
            });
          });
        }}
      >
        Reload
      </Button>
      <Button color="inherit" onClick={closeCurrentSnackbar}>
        Dismiss
      </Button>
    </ContrastThemeProvider>
  );
}

export default function RegisterServiceWorker(): null {
  const { enqueueSnackbar, closeSnackbar } = useSnackbar();
  useEffect(() => {
    if (window.location.host === 'localhost') {
      // Do not register service worker during local development.
      return;
    }
    if (!('serviceWorker' in navigator)) {
      log.debug('No service worker support found');
      return;
    }
    const updateSW = registerSW({
      onNeedRefresh() {
        const key = enqueueSnackbar('An update for Refinery is available', {
          persist: true,
          action: (
            <UpdateSnackbarActions
              closeCurrentSnackbar={() => closeSnackbar(key)}
              enqueueSnackbar={enqueueSnackbar}
              updateSW={updateSW}
            />
          ),
        });
      },
      onOfflineReady() {
        log.debug('Service worker is ready for offline use');
      },
      onRegistered() {
        log.debug('Registered service worker');
      },
      onRegisterError(error) {
        log.error('Failed to register service worker', error);
      },
    });
  }, [enqueueSnackbar, closeSnackbar]);
  return null;
}
