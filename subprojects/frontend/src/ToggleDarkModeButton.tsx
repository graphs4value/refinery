import DarkModeIcon from '@mui/icons-material/DarkMode';
import LightModeIcon from '@mui/icons-material/LightMode';
import IconButton from '@mui/material/IconButton';
import { observer } from 'mobx-react-lite';
import React from 'react';

import { useRootStore } from './RootStoreProvider';

export default observer(function ToggleDarkModeButton(): JSX.Element {
  const { themeStore } = useRootStore();
  const { darkMode } = themeStore;

  return (
    <IconButton
      color="inherit"
      onClick={() => themeStore.toggleDarkMode()}
      aria-label={darkMode ? 'Switch to light mode' : 'Switch to dark mode'}
    >
      {darkMode ? <LightModeIcon /> : <DarkModeIcon />}
    </IconButton>
  );
});
