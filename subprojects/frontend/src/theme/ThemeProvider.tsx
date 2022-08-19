import {
  alpha,
  createTheme,
  type Components,
  type CSSObject,
  responsiveFontSizes,
  type Theme,
  type ThemeOptions,
  ThemeProvider as MaterialUiThemeProvider,
  type TypographyStyle,
  type TypographyVariantsOptions,
  useTheme,
} from '@mui/material/styles';
import { observer } from 'mobx-react-lite';
import React, { type ReactNode, createContext, useContext } from 'react';

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
      root: { '&.rounded': { borderRadius: '50em' } },
      text: { '&.rounded': { padding: '6px 16px' } },
      textSizeSmall: { '&.rounded': { padding: '4px 10px' } },
      textSizeLarge: { '&.rounded': { padding: '8px 22px' } },
    },
  },
  MuiToggleButton: {
    styleOverrides: {
      root: { '&.iconOnly': { borderRadius: '100%' } },
    },
  },
  MuiToggleButtonGroup: {
    styleOverrides: {
      root: {
        '&.rounded .MuiToggleButtonGroup-groupedHorizontal': {
          ':first-of-type': {
            paddingLeft: 15,
            borderRadius: '50em 0 0 50em',
          },
          ':last-of-type': {
            paddingRight: 15,
            borderRadius: '0 50em 50em 0',
          },
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

function createResponsiveTheme(options: ThemeOptions): Theme {
  return responsiveFontSizes(createTheme(options));
}

const lightTheme = createResponsiveTheme({
  typography,
  components,
  palette: {
    mode: 'light',
    primary: { main: '#038a99' },
    secondary: { main: '#e45649' },
    error: { main: '#ca1243' },
    warning: { main: '#c18401' },
    success: { main: '#50a14f' },
    info: { main: '#4078f2' },
    background: {
      default: '#ffffff',
      paper: '#ffffff',
    },
    text: {
      primary: '#35373e',
      secondary: '#696c77',
      disabled: '#8e8f97',
    },
    divider: alpha('#35373e', 0.21),
    outer: {
      background: '#fafafa',
      border: '#d4d4d6',
    },
    highlight: {
      number: '#0084bc',
      parameter: '#6a3e3e',
      comment: '#8e8f97',
      activeLine: '#fafafa',
      selection: '#c8e4fb',
      lineNumber: '#8e8f97',
      foldPlaceholder: alpha('#35373e', 0.08),
      activeLintRange: alpha('#f2a60d', 0.28),
      occurences: {
        read: alpha('#35373e', 0.16),
        write: alpha('#35373e', 0.16),
      },
      search: {
        match: '#00bcd4',
        selected: '#d500f9',
        contrastText: '#ffffff',
      },
    },
  },
});

const darkTheme = createResponsiveTheme({
  typography,
  components: {
    ...components,
    MuiSnackbarContent: {
      styleOverrides: {
        root: {
          color: '#f00',
          backgroundColor: '#000',
          border: `10px solid #ff0`,
        },
      },
    },
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
    secondary: { main: '#be5046' },
    error: { main: '#e06c75' },
    warning: { main: '#e5c07b' },
    success: { main: '#98c379' },
    info: { main: '#61afef' },
    background: {
      default: '#282c34',
      paper: '#21252b',
    },
    text: {
      primary: '#ebebff',
      secondary: '#abb2bf',
      disabled: '#828997',
    },
    divider: alpha('#abb2bf', 0.24),
    outer: {
      background: '#21252b',
      border: '#181a1f',
    },
    highlight: {
      number: '#6188a6',
      parameter: '#c8ae9d',
      comment: '#828997',
      activeLine: '#21252b',
      selection: '#3e4453',
      lineNumber: '#828997',
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
});

const ContrastThemeContext = createContext<Theme | undefined>(undefined);

function ThemeAndContrastThemeProvider({
  theme,
  contrastTheme,
  children,
}: {
  theme: Theme;
  contrastTheme: Theme;
  children?: ReactNode;
}): JSX.Element {
  return (
    <MaterialUiThemeProvider theme={theme}>
      <ContrastThemeContext.Provider value={contrastTheme}>
        {children}
      </ContrastThemeContext.Provider>
    </MaterialUiThemeProvider>
  );
}

ThemeAndContrastThemeProvider.defaultProps = {
  children: undefined,
};

export function ContrastThemeProvider({
  children,
}: {
  children?: ReactNode;
}): JSX.Element {
  const theme = useTheme();
  const contrastTheme = useContext(ContrastThemeContext);
  if (!contrastTheme) {
    throw new Error('ContrastThemeProvider must be used within ThemeProvider');
  }
  return (
    <ThemeAndContrastThemeProvider theme={contrastTheme} contrastTheme={theme}>
      {children}
    </ThemeAndContrastThemeProvider>
  );
}

ContrastThemeProvider.defaultProps = {
  children: undefined,
};

function ThemeProvider({ children }: { children?: ReactNode }): JSX.Element {
  const {
    themeStore: { darkMode },
  } = useRootStore();

  return (
    <ThemeAndContrastThemeProvider
      theme={darkMode ? darkTheme : lightTheme}
      contrastTheme={darkMode ? lightTheme : darkTheme}
    >
      {children}
    </ThemeAndContrastThemeProvider>
  );
}

ThemeProvider.defaultProps = {
  children: undefined,
};

export default observer(ThemeProvider);
