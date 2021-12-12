import { observer } from 'mobx-react-lite';
import { ThemeProvider as MaterialUiThemeProvider } from '@mui/material/styles';
import React from 'react';

import { useRootStore } from '../RootStore';

export const ThemeProvider: React.FC = observer(({ children }) => {
  const { themeStore } = useRootStore();

  return (
    <MaterialUiThemeProvider theme={themeStore.materialUiTheme}>
      {children}
    </MaterialUiThemeProvider>
  );
});
