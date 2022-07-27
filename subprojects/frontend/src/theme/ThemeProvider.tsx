import { observer } from 'mobx-react-lite';
import { ThemeProvider as MaterialUiThemeProvider } from '@mui/material/styles';
import React, { type ReactNode } from 'react';

import { useRootStore } from '../RootStore';

export const ThemeProvider: React.FC<{ children: ReactNode }> = observer(({ children }) => {
  const { themeStore } = useRootStore();

  return (
    <MaterialUiThemeProvider theme={themeStore.materialUiTheme}>
      {children}
    </MaterialUiThemeProvider>
  );
});
