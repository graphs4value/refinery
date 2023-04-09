/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import Button from '@mui/material/Button';
import { observer } from 'mobx-react-lite';
import { useEffect } from 'react';

import { useRootStore } from './RootStoreProvider';
import { ContrastThemeProvider } from './theme/ThemeProvider';
import useDelayedSnackbar from './utils/useDelayedSnackbar';

export default observer(function UpdateNotification(): null {
  const { pwaStore } = useRootStore();
  const { needsUpdate, updateError } = pwaStore;
  const enqueueLater = useDelayedSnackbar();

  useEffect(() => {
    if (needsUpdate) {
      return enqueueLater('An update for Refinery is available', {
        persist: true,
        action: (
          <ContrastThemeProvider>
            <Button color="primary" onClick={() => pwaStore.reloadWithUpdate()}>
              Reload
            </Button>
            <Button color="inherit" onClick={() => pwaStore.dismissUpdate()}>
              Dismiss
            </Button>
          </ContrastThemeProvider>
        ),
      });
    }

    if (updateError) {
      return enqueueLater('Failed to download update', {
        variant: 'error',
        action: (
          <ContrastThemeProvider>
            <Button color="inherit" onClick={() => pwaStore.checkForUpdates()}>
              Try again
            </Button>
            <Button color="inherit" onClick={() => pwaStore.dismissError()}>
              Dismiss
            </Button>
          </ContrastThemeProvider>
        ),
      });
    }

    return () => {};
  }, [pwaStore, needsUpdate, updateError, enqueueLater]);

  return null;
});
