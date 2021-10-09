import { makeAutoObservable } from 'mobx';
import {
  Theme,
  createTheme,
  responsiveFontSizes,
} from '@mui/material/styles';

import {
  EditorTheme,
  EditorThemeData,
  DEFAULT_THEME,
  EDITOR_THEMES,
} from './EditorTheme';

export class ThemeStore {
  currentTheme: EditorTheme = DEFAULT_THEME;

  constructor() {
    makeAutoObservable(this);
  }

  toggleDarkMode(): void {
    this.currentTheme = this.currentThemeData.toggleDarkMode;
  }

  private get currentThemeData(): EditorThemeData {
    return EDITOR_THEMES[this.currentTheme];
  }

  get materialUiTheme(): Theme {
    const themeData = this.currentThemeData;
    const materialUiTheme = createTheme({
      palette: {
        mode: themeData.paletteMode,
        background: {
          default: themeData.background,
          paper: themeData.background,
        },
        primary: {
          main: themeData.primary,
        },
        secondary: {
          main: themeData.secondary,
        },
        text: {
          primary: themeData.foregroundHighlight,
          secondary: themeData.foreground,
        },
      },
    });
    return responsiveFontSizes(materialUiTheme);
  }

  get darkMode(): boolean {
    return this.currentThemeData.paletteMode === 'dark';
  }

  get className(): string {
    return this.currentThemeData.className;
  }
}
