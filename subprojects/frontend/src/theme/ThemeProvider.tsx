import {
  createTheme,
  responsiveFontSizes,
  type ThemeOptions,
  ThemeProvider as MaterialUiThemeProvider,
} from '@mui/material/styles';
import { observer } from 'mobx-react-lite';
import React, { type ReactNode } from 'react';

import { useRootStore } from '../RootStore';

import EditorTheme from './EditorTheme';

function getMUIThemeOptions(currentTheme: EditorTheme): ThemeOptions {
  switch (currentTheme) {
    case EditorTheme.Light:
      return {
        palette: {
          primary: {
            main: '#56b6c2',
          },
        },
      };
    case EditorTheme.Dark:
      return {
        palette: {
          primary: {
            main: '#56b6c2',
          },
        },
      };
    default:
      throw new Error(`Unknown theme: ${currentTheme}`);
  }
}

function ThemeProvider({ children }: { children?: ReactNode }) {
  const {
    themeStore: { currentTheme, darkMode },
  } = useRootStore();

  const themeOptions = getMUIThemeOptions(currentTheme);
  const theme = responsiveFontSizes(
    createTheme({
      ...themeOptions,
      palette: {
        mode: darkMode ? 'dark' : 'light',
        ...(themeOptions.palette ?? {}),
      },
    }),
  );

  return (
    <MaterialUiThemeProvider theme={theme}>{children}</MaterialUiThemeProvider>
  );
}

ThemeProvider.defaultProps = {
  children: undefined,
};

export default observer(ThemeProvider);
