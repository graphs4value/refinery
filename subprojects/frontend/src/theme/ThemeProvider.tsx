import {
  alpha,
  createTheme,
  type Components,
  type CSSObject,
  responsiveFontSizes,
  type ThemeOptions,
  ThemeProvider as MaterialUiThemeProvider,
  type TypographyStyle,
  type TypographyVariantsOptions,
} from '@mui/material/styles';
import { observer } from 'mobx-react-lite';
import React, { type ReactNode } from 'react';

import { useRootStore } from '../RootStore';

interface OuterPalette {
  background: string;
  border: string;
}

interface HighlightPalette {
  number: string;
  parameter: string;
  comment: string;
  activeLine: string;
  selection: string;
  lineNumber: string;
  foldPlaceholder: string;
  activeLintRange: string;
  occurences: {
    read: string;
    write: string;
  };
  search: {
    match: string;
    selected: string;
    contrastText: string;
  };
}

declare module '@mui/material/styles' {
  interface TypographyVariants {
    editor: TypographyStyle;
  }

  // eslint-disable-next-line @typescript-eslint/no-shadow -- Augment imported interface.
  interface TypographyVariantsOptions {
    editor: TypographyStyle;
  }

  interface Palette {
    outer: OuterPalette;
    highlight: HighlightPalette;
  }

  interface PaletteOptions {
    outer: OuterPalette;
    highlight: HighlightPalette;
  }
}

function getMUIThemeOptions(darkMode: boolean): ThemeOptions {
  const typography: TypographyVariantsOptions = {
    editor: {
      fontFamily: '"JetBrains MonoVariable", "JetBrains Mono", monospace',
      fontFeatureSettings: '"liga", "calt"',
      fontSize: '1rem',
      fontWeight: 400,
      lineHeight: 1.5,
      letterSpacing: 0,
      textRendering: 'optimizeLegibility',
    },
  };

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
    MuiTooltip: {
      styleOverrides: {
        tooltip: {
          background: alpha('#212121', 0.93),
          color: '#fff',
        },
        arrow: {
          color: alpha('#212121', 0.93),
        },
      },
    },
  };

  return darkMode
    ? {
        typography,
        components: {
          ...components,
          MuiTooltip: {
            ...(components.MuiTooltip || {}),
            styleOverrides: {
              ...(components.MuiTooltip?.styleOverrides || {}),
              tooltip: {
                ...((components.MuiTooltip?.styleOverrides?.tooltip as
                  | CSSObject
                  | undefined) || {}),
                color: '#ebebff',
              },
            },
          },
        },
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
          outer: {
            background: '#21252b',
            border: '#181a1f',
          },
          highlight: {
            number: '#6188a6',
            parameter: '#c8ae9d',
            comment: '#6b717d',
            activeLine: '#21252b',
            selection: '#3e4453',
            lineNumber: '#4b5263',
            foldPlaceholder: alpha('#ebebff', 0.12),
            activeLintRange: alpha('#fbc346', 0.28),
            occurences: {
              read: alpha('#ebebff', 0.24),
              write: alpha('#ebebff', 0.24),
            },
            search: {
              match: '#33eaff',
              selected: '#dd33fa',
              contrastText: '#21252b',
            },
          },
        },
      }
    : {
        typography,
        components,
        palette: {
          mode: 'light',
          primary: { main: '#0398a8' },
          outer: {
            background: '#f5f5f5',
            border: '#cacaca',
          },
          highlight: {
            number: '#3d79a2',
            parameter: '#6a3e3e',
            comment: 'rgba(0, 0, 0, 0.38)',
            activeLine: '#f5f5f5',
            selection: '#c8e4fb',
            lineNumber: 'rgba(0, 0, 0, 0.38)',
            foldPlaceholder: 'rgba(0, 0, 0, 0.12)',
            activeLintRange: alpha('#ed6c02', 0.24),
            occurences: {
              read: 'rgba(0, 0, 0, 0.12)',
              write: 'rgba(0, 0, 0, 0.12)',
            },
            search: {
              match: '#00bcd4',
              selected: '#d500f9',
              contrastText: '#ffffff',
            },
          },
        },
      };
}

function ThemeProvider({ children }: { children?: ReactNode }) {
  const {
    themeStore: { darkMode },
  } = useRootStore();

  const themeOptions = getMUIThemeOptions(darkMode);
  const theme = responsiveFontSizes(createTheme(themeOptions));

  return (
    <MaterialUiThemeProvider theme={theme}>{children}</MaterialUiThemeProvider>
  );
}

ThemeProvider.defaultProps = {
  children: undefined,
};

export default observer(ThemeProvider);
