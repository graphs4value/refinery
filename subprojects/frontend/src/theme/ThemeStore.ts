/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { makeAutoObservable, runInAction } from 'mobx';

import {
  ThemeSource as ThemeSourceSchema,
  type ThemeSource,
} from '../RefineryContextBridge';
import getLogger from '../utils/getLogger';
import isElectron from '../utils/isElectron';

const ThemePreference = ThemeSourceSchema;

export type ThemePreference = ThemeSource;

export type SelectedPane = 'code' | 'graph' | 'table' | 'chat';

const THEME_PREFERENCE_KEY = 'refinery:themePreference';

const SHOW_LINE_NUMBERS_KEY = 'refinery:showLineNumbers';

const COLOR_IDENTIFIERS_KEY = 'refinery:colorIdentifiers';

const log = getLogger('theme.ThemeStore');

export default class ThemeStore {
  preference: ThemePreference = 'system';

  systemDarkMode: boolean;

  showCode = true;

  showGraph = true;

  showTable = false;

  showChat = false;

  showLineNumbers: boolean;

  colorIdentifiers: boolean;

  constructor() {
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
    this.systemDarkMode = mediaQuery.matches;
    mediaQuery.addEventListener('change', (event) => {
      runInAction(() => {
        this.systemDarkMode = event.matches;
      });
    });
    this.showLineNumbers =
      window.localStorage.getItem(SHOW_LINE_NUMBERS_KEY) === 'true';
    this.colorIdentifiers =
      window.localStorage.getItem(COLOR_IDENTIFIERS_KEY) !== 'false';
    if (window.refinery) {
      window.refinery.onThemeSourceChange((nextPreference) => {
        const result = ThemePreference.safeParse(nextPreference);
        if (!result.success) {
          log.error(
            { err: result.error },
            'Received invalid theme source from Electron',
          );
        } else if (this.preference !== result.data) {
          runInAction(() => {
            this.preference = result.data;
          });
        }
      });
    } else {
      this.preference =
        ThemePreference.safeParse(
          window.localStorage.getItem(THEME_PREFERENCE_KEY),
        ).data ?? 'system';
    }
    window.addEventListener('storage', ({ key, newValue }) => {
      switch (key) {
        case THEME_PREFERENCE_KEY:
          {
            const parsedValue = ThemePreference.safeParse(newValue);
            if (!isElectron && parsedValue.success) {
              runInAction(() => (this.preference = parsedValue.data));
            }
          }
          break;
        case SHOW_LINE_NUMBERS_KEY:
          runInAction(() => (this.showLineNumbers = newValue === 'true'));
          break;
        case COLOR_IDENTIFIERS_KEY:
          runInAction(() => (this.colorIdentifiers = newValue !== 'false'));
          break;
      }
    });
    makeAutoObservable(this, {
      isShowing: false,
    });
  }

  get darkMode(): boolean {
    switch (this.preference) {
      case 'light':
        return false;
      case 'dark':
        return true;
      default:
        return this.systemDarkMode;
    }
  }

  toggleDarkMode(): void {
    let nextPreference: ThemePreference;
    if (isElectron) {
      // In Electron, `systemDarkMode` comes from `nativeTheme` and already takes the
      // theme source override we set into account, so we can't rely on it to restore
      // 'system' theme source preference.
      nextPreference = this.darkMode ? 'light' : 'dark';
    }
    if (this.darkMode) {
      nextPreference = this.systemDarkMode ? 'light' : 'system';
    } else {
      nextPreference = this.systemDarkMode ? 'system' : 'dark';
    }
    this.preference = nextPreference;
    this.saveThemePreference();
  }

  setPreference(preference: ThemePreference) {
    this.preference = preference;
    this.saveThemePreference();
  }

  private saveThemePreference() {
    if (window.refinery) {
      window.refinery.setThemeSource(this.preference);
    } else {
      window.localStorage.setItem(THEME_PREFERENCE_KEY, this.preference);
    }
  }

  togglePane(pane: SelectedPane) {
    switch (pane) {
      case 'code':
        this.toggleCode();
        break;
      case 'graph':
        this.toggleGraph();
        break;
      case 'table':
        this.toggleTable();
        break;
      case 'chat':
        this.toggleChat();
        break;
      default:
        throw new Error(`Unknown pane: ${String(pane)}`);
    }
  }

  isShowing(pane: SelectedPane): boolean {
    switch (pane) {
      case 'code':
        return this.showCode;
      case 'graph':
        return this.showGraph;
      case 'table':
        return this.showTable;
      case 'chat':
        return this.showChat;
      default:
        throw new Error(`Unknown pane: ${String(pane)}`);
    }
  }

  toggleCode(): void {
    if (!this.showGraph && !this.showTable) {
      return;
    }
    this.showCode = !this.showCode;
  }

  toggleGraph(): void {
    if (!this.showCode && !this.showTable) {
      return;
    }
    this.showGraph = !this.showGraph;
  }

  toggleTable(): void {
    if (!this.showCode && !this.showGraph) {
      return;
    }
    this.showTable = !this.showTable;
  }

  toggleChat(): void {
    this.showChat = !this.showChat;
  }

  get selectedPane(): SelectedPane {
    if (this.showCode) {
      return 'code';
    }
    if (this.showGraph) {
      return 'graph';
    }
    if (this.showTable) {
      return 'table';
    }
    return 'code';
  }

  setSelectedPane(pane: SelectedPane, keepCode = true): void {
    if (pane === 'chat') {
      this.showChat = true;
      return;
    }
    this.showCode = pane === 'code' || (keepCode && this.showCode);
    this.showGraph = pane === 'graph';
    this.showTable = pane === 'table';
  }

  toggleLineNumbers(): void {
    this.showLineNumbers = !this.showLineNumbers;
    window.localStorage.setItem(
      SHOW_LINE_NUMBERS_KEY,
      String(this.showLineNumbers),
    );
    log.debug('Show line numbers: %s', String(this.showLineNumbers));
  }

  toggleColorIdentifiers(): void {
    this.colorIdentifiers = !this.colorIdentifiers;
    window.localStorage.setItem(
      COLOR_IDENTIFIERS_KEY,
      String(this.colorIdentifiers),
    );
    log.debug('Color identifiers: %s', String(this.colorIdentifiers));
  }
}
