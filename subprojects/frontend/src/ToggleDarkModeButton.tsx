/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import DarkModeOutlinedIcon from '@mui/icons-material/DarkModeOutlined';
import LightModeOutlinedIcon from '@mui/icons-material/LightModeOutlined';
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import { observer } from 'mobx-react-lite';
import { flushSync } from 'react-dom';

import { useRootStore } from './RootStoreProvider';
import ThemeStore from './theme/ThemeStore';
import getLogger from './utils/getLogger';

const logger = getLogger('ToggleDarkModeButton');

function toggleWithViewTransition(themeStore: ThemeStore): void {
  document.body.classList.add('notransition');
  const transition = document.startViewTransition(() => {
    flushSync(() => themeStore.toggleDarkMode());
  });
  transition.finished
    .finally(() => {
      document.body.classList.remove('notransition');
    })
    .catch((error) => {
      logger.error('Transition failed when toggling dark mode', error);
    });
}

function toggleWithoutViewTransition(themeStore: ThemeStore): void {
  document.body.classList.add('notransition');
  try {
    flushSync(() => themeStore.toggleDarkMode());
  } finally {
    document.body.classList.remove('notransition');
  }
}

export default observer(function ToggleDarkModeButton(): React.ReactElement {
  const { themeStore } = useRootStore();
  const { darkMode } = themeStore;

  const callback = () => {
    if (
      'startViewTransition' in document ||
      window.matchMedia('(prefers-reduced-motion: reduce)').matches
    ) {
      toggleWithViewTransition(themeStore);
    } else {
      toggleWithoutViewTransition(themeStore);
    }
  };

  return (
    <Tooltip title={darkMode ? 'Switch to light mode' : 'Switch to dark mode'}>
      <IconButton color="inherit" onClick={callback}>
        {darkMode ? <DarkModeOutlinedIcon /> : <LightModeOutlinedIcon />}
      </IconButton>
    </Tooltip>
  );
});
