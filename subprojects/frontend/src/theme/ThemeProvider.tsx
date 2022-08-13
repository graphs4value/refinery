import {
  alpha,
  createTheme,
  type Components,
  responsiveFontSizes,
  type ThemeOptions,
  ThemeProvider as MaterialUiThemeProvider,
} from '@mui/material/styles';
import { observer } from 'mobx-react-lite';
import React, { type ReactNode } from 'react';

import { useRootStore } from '../RootStore';

import EditorTheme from './EditorTheme';

interface HighlightStyles {
  number: string;
  parameter: string;
  comment: string;
  activeLine: string;
  occurences: {
    read: string;
    write: string;
  };
}

declare module '@mui/material/styles' {
  interface Palette {
    divider2: string;
    selection: Palette['primary'];
    highlight: HighlightStyles;
  }

  interface PaletteOptions {
    divider2: string;
    selection: PaletteOptions['primary'];
    highlight: HighlightStyles;
  }
}

function getMUIThemeOptions(currentTheme: EditorTheme): ThemeOptions {
  const components: Components = {
    MuiButton: {
      styleOverrides: {
        root: { borderRadius: '50em' },
        text: { padding: '6px 16px' },
        textSizeSmall: { padding: '4px 10px' },
        textSizeLarge: { padding: '8px 22px' },
      },
    },
    MuiToggleButtonGroup: {
      styleOverrides: {
        groupedHorizontal: {
          borderRadius: '50em',
          ':first-of-type': { paddingLeft: 15 },
          ':last-of-type': { paddingRight: 15 },
          '&.MuiToggleButton-sizeSmall': {
            ':first-of-type': { paddingLeft: 9 },
            ':last-of-type': { paddingRight: 9 },
          },
          '&.MuiToggleButton-sizeLarge': {
            ':first-of-type': { paddingLeft: 21 },
            ':last-of-type': { paddingRight: 21 },
          },
        },
      },
    },
  };

  switch (currentTheme) {
    case EditorTheme.Light:
      return {
        components,
        palette: {
          mode: 'light',
          primary: { main: '#0097a7' },
          selection: {
            main: '#c8e4fb',
            contrastText: '#000',
          },
          divider2: '#d7d7d7',
          highlight: {
            number: '#1976d2',
            parameter: '#6a3e3e',
            comment: alpha('#000', 0.38),
            activeLine: '#f5f5f5',
            occurences: {
              read: '#ceccf7',
              write: '#f0d8a8',
            },
          },
        },
      };
    case EditorTheme.Dark:
      return {
        components,
        palette: {
          mode: 'dark',
          primary: { main: '#56b6c2' },
          error: { main: '#e06c75' },
          warning: { main: '#e5c07b' },
          success: { main: '#57a470' },
          info: { main: '#52b8ff' },
          background: {
            default: '#282c34',
            paper: '#21252b',
          },
          text: {
            primary: '#ebebff',
            secondary: '#abb2bf',
            disabled: '#4b5263',
          },
          divider: alpha('#abb2bf', 0.16),
          divider2: '#181a1f',
          selection: {
            main: '#3e4453',
            contrastText: '#fff',
          },
          highlight: {
            number: '#6188a6',
            parameter: '#c8ae9d',
            comment: '#6b717d',
            activeLine: '#21252b',
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
