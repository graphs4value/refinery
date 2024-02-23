/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import {
  alpha,
  createTheme,
  responsiveFontSizes,
  type Theme,
  type ThemeOptions,
  ThemeProvider as MaterialUiThemeProvider,
  type TypographyStyle,
  type CSSObject,
} from '@mui/material/styles';
import { observer } from 'mobx-react-lite';
import { type ReactNode, createContext, useContext } from 'react';

import { useRootStore } from '../RootStoreProvider';

interface OuterPalette {
  background: string;
  border: string;
}

interface TypeHashPalette {
  text: string;
  box: string;
}

interface HighlightPalette {
  number: string;
  parameter: string;
  comment: string;
  activeLine: string;
  selection: string;
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
  typeHash: TypeHashPalette[];
}

declare module '@mui/material/styles' {
  interface TypographyVariants {
    fontWeightEditorNormal: number;
    fontWeightEditorTypeHash: number;
    fontWeightEditorBold: number;
    editor: TypographyStyle;
  }

  interface TypographyVariantsOptions {
    fontWeightEditorNormal?: number;
    fontWeightEditorTypeHash?: number;
    fontWeightEditorBold?: number;
    editor?: TypographyStyle;
  }

  interface Palette {
    outer: OuterPalette;
    highlight: HighlightPalette;
  }

  interface PaletteOptions {
    outer?: Partial<OuterPalette>;
    highlight?: Partial<HighlightPalette>;
  }
}

function createResponsiveTheme(
  options: ThemeOptions,
  overrides: ThemeOptions = {},
): Theme {
  const theme = createTheme({
    ...options,
    typography: {
      fontFamily:
        '"Open Sans Variable", "Open Sans", "Roboto", "Helvetica", "Arial", sans-serif',
      fontWeightMedium: 500,
      fontWeightEditorNormal: 400,
      fontWeightEditorTypeHash: 500,
      fontWeightEditorBold: 700,
      button: {
        fontWeight: 600,
        fontVariationSettings: '"wdth" 87.5',
        fontSize: '1rem',
        lineHeight: 1.5,
      },
      editor: {
        fontFamily:
          '"JetBrains Mono Variable", "JetBrains Mono", "Cascadia Code", "Fira Code", monospace',
        fontFeatureSettings: '"liga", "calt"',
        // `rem` for JetBrains MonoVariable make the text too large in Safari.
        fontSize: '16px',
        fontWeight: 400,
        lineHeight: 1.5,
        letterSpacing: 0,
        textRendering: 'optimizeLegibility',
      },
      ...(options.typography ?? {}),
    },
  });

  function shadedButtonStyle(color: string): CSSObject {
    const opacity = theme.palette.action.focusOpacity;
    return {
      background: alpha(color, opacity),
      ':hover': {
        background: alpha(color, opacity + theme.palette.action.hoverOpacity),
        '@media(hover: none)': {
          background: alpha(color, opacity),
        },
      },
      '&.Mui-disabled': {
        background: alpha(theme.palette.text.disabled, opacity),
      },
    };
  }

  const themeWithComponents = createTheme(theme, {
    components: {
      MuiCssBaseline: {
        styleOverrides: {
          body: {
            overscrollBehavior: 'contain',
          },
        },
      },
      MuiButton: {
        styleOverrides: {
          root: {
            '&.rounded': { borderRadius: '50em' },
            '.MuiButton-startIcon': { marginRight: 6 },
            '.MuiButton-endIcon': { marginLeft: 6 },
            '&.shaded': {
              ...shadedButtonStyle(theme.palette.text.primary),
              ...(
                [
                  'primary',
                  'secondary',
                  'error',
                  'warning',
                  'success',
                  'info',
                ] as const
              ).reduce((accumulator: CSSObject, color) => {
                const colorCapitalized =
                  (color[0] ?? '').toUpperCase() + color.substring(1);
                return {
                  ...accumulator,
                  [`&.MuiButton-text${colorCapitalized}, &.MuiButton-outlined${colorCapitalized}`]:
                    shadedButtonStyle(theme.palette[color].main),
                };
              }, {}),
            },
          },
          sizeSmall: { fontSize: '0.875rem', lineHeight: '1.75' },
          sizeLarge: { fontSize: '1rem' },
          text: { '&.rounded': { padding: '6px 14px' } },
          textSizeSmall: { '&.rounded': { padding: '4px 8px' } },
          textSizeLarge: { '&.rounded': { padding: '8px 20px' } },
          outlined: { '&.rounded': { padding: '5px 13px' } },
          outlinedSizeSmall: { '&.rounded': { padding: '3px 9px' } },
          outlinedSizeLarge: { '&.rounded': { padding: '7px 19px' } },
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
    },
  });

  const themeWithOverrides = createTheme(themeWithComponents, overrides);

  return responsiveFontSizes(themeWithOverrides);
}

export const lightTheme = (() => {
  const primaryText = '#19202b';
  const disabledText = '#a0a1a7';
  const darkBackground = '#f5f5f5';

  return createResponsiveTheme({
    palette: {
      mode: 'light',
      primary: { main: '#038a99' },
      secondary: { main: '#61afef' },
      error: { main: '#ca1243' },
      warning: { main: '#c18401' },
      success: { main: '#50a14f' },
      info: { main: '#4078f2' },
      background: {
        default: '#fff',
        paper: '#fff',
      },
      text: {
        primary: primaryText,
        secondary: '#696c77',
        disabled: disabledText,
      },
      divider: alpha(primaryText, 0.16),
      outer: {
        background: darkBackground,
        border: '#c8c8c8',
      },
      highlight: {
        number: '#0084bc',
        parameter: '#6a3e3e',
        comment: disabledText,
        activeLine: darkBackground,
        selection: '#c8e4fb',
        foldPlaceholder: alpha(primaryText, 0.08),
        activeLintRange: alpha('#f2a60d', 0.28),
        occurences: {
          read: alpha(primaryText, 0.16),
          write: alpha(primaryText, 0.16),
        },
        search: {
          match: '#00bcd4',
          selected: '#d500f9',
          contrastText: '#fff',
        },
        typeHash: [
          { text: '#986801', box: '#e5c07b' },
          { text: '#d6493e', box: '#e06c75' },
          { text: '#50a14f', box: '#98c379' },
          { text: '#a626a4', box: '#c678dd' },
          { text: '#4078f2', box: '#80a7f4' },
          { text: '#827662', box: '#e3d1b2' },
          { text: '#904f53', box: '#e78b8f' },
          { text: '#637855', box: '#abcc94' },
          { text: '#805f89', box: '#dbb2e8' },
          { text: '#5987ae', box: '#92c0e9' },
        ],
      },
    },
  });
})();

export const darkTheme = (() => {
  const primaryText = '#ebebff';
  const secondaryText = '#abb2bf';
  const darkBackground = '#21252b';

  return createResponsiveTheme(
    {
      typography: {
        fontWeightEditorNormal: 350,
        fontWeightEditorTypeHash: 350,
        fontWeightEditorBold: 650,
      },
      palette: {
        mode: 'dark',
        primary: { main: '#56b6c2' },
        secondary: { main: '#be5046' },
        error: { main: '#e06c75' },
        warning: { main: '#d19a66' },
        success: { main: '#98c379' },
        info: { main: '#61afef' },
        background: {
          default: '#282c34',
          paper: darkBackground,
        },
        text: {
          primary: primaryText,
          secondary: secondaryText,
          disabled: '#5c6370',
        },
        divider: alpha(primaryText, 0.24),
        outer: {
          background: darkBackground,
          border: '#181a1f',
        },
        highlight: {
          number: '#6188a6',
          parameter: '#c8ae9d',
          comment: '#7f848e',
          activeLine: '#2c313c',
          selection: '#404859',
          foldPlaceholder: alpha(primaryText, 0.12),
          activeLintRange: alpha('#fbc346', 0.28),
          occurences: {
            read: alpha(primaryText, 0.14),
            write: alpha(primaryText, 0.14),
          },
          search: {
            match: '#33eaff',
            selected: '#dd33fa',
            contrastText: darkBackground,
          },
          typeHash: [
            { text: '#e5c07b', box: '#ae8003' },
            { text: '#e06c75', box: '#a23b47' },
            { text: '#98c379', box: '#428141' },
            { text: '#c678dd', box: '#854797' },
            { text: '#61afef', box: '#3982bb' },
            { text: '#e3d1b2', box: '#827662' },
            { text: '#e78b8f', box: '#904f53' },
            { text: '#abcc94', box: '#647e63' },
            { text: '#dbb2e8', box: '#805f89' },
            { text: '#92c0e9', box: '#4f7799' },
          ],
        },
      },
    },
    {
      components: {
        MuiTooltip: {
          styleOverrides: {
            tooltip: {
              color: primaryText,
            },
          },
        },
      },
    },
  );
})();

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
  const contrastTheme = useContext(ContrastThemeContext);
  if (!contrastTheme) {
    throw new Error('ContrastThemeProvider must be used within ThemeProvider');
  }
  return (
    <MaterialUiThemeProvider theme={contrastTheme}>
      {children}
    </MaterialUiThemeProvider>
  );
}

ContrastThemeProvider.defaultProps = {
  children: undefined,
};

const ThemeProvider = observer(function ThemeProvider({
  children,
}: {
  children?: ReactNode;
}): JSX.Element {
  const {
    themeStore: { darkMode },
  } = useRootStore();

  return (
    <ThemeAndContrastThemeProvider
      theme={darkMode ? darkTheme : lightTheme}
      contrastTheme={darkTheme}
    >
      {children}
    </ThemeAndContrastThemeProvider>
  );
});

ThemeProvider.defaultProps = {
  children: undefined,
};

export default ThemeProvider;
