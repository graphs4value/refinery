import {
  createTheme,
  responsiveFontSizes,
  type ThemeOptions,
  ThemeProvider as MaterialUiThemeProvider,
} from '@mui/material/styles';
import { observer } from 'mobx-react-lite';
import React, { type CSSProperties, type ReactNode } from 'react';

import { useRootStore } from '../RootStore';

import EditorTheme from './EditorTheme';

interface HighlightStyles {
  number: CSSProperties['color'];
  parameter: CSSProperties['color'];
  occurences: {
    read: CSSProperties['color'];
    write: CSSProperties['color'];
  };
}

declare module '@mui/material/styles' {
  interface Palette {
    selection: Palette['primary'];
    highlight: HighlightStyles;
  }

  interface PaletteOptions {
    selection: PaletteOptions['primary'];
    highlight: HighlightStyles;
  }
}

function getMUIThemeOptions(currentTheme: EditorTheme): ThemeOptions {
  switch (currentTheme) {
    case EditorTheme.Light:
      return {
        palette: {
          mode: 'light',
          primary: { main: '#0097a7' },
          selection: {
            main: '#c8e4fb',
            contrastText: '#000',
          },
          highlight: {
            number: '#1976d2',
            parameter: '#6a3e3e',
            occurences: {
              read: '#ceccf7',
              write: '#f0d8a8',
            },
          },
        },
      };
    case EditorTheme.Dark:
      return {
        palette: {
          mode: 'dark',
          primary: { main: '#56b6c2' },
          selection: {
            main: '#3e4453',
            contrastText: '#fff',
          },
          highlight: {
            number: '#6188a6',
            parameter: '#c8ae9d',
            occurences: {
              read: 'rgba(255, 255, 255, 0.15)',
              write: 'rgba(255, 255, 128, 0.4)',
            },
          },
        },
      };
    default:
      throw new Error(`Unknown theme: ${currentTheme}`);
  }
}

function ThemeProvider({ children }: { children?: ReactNode }) {
  const {
    themeStore: { currentTheme },
  } = useRootStore();

  const themeOptions = getMUIThemeOptions(currentTheme);
  const theme = responsiveFontSizes(createTheme(themeOptions));

  return (
    <MaterialUiThemeProvider theme={theme}>{children}</MaterialUiThemeProvider>
  );
}

ThemeProvider.defaultProps = {
  children: undefined,
};

export default observer(ThemeProvider);
