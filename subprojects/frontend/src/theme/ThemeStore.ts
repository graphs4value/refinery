import { action, computed, makeObservable, observable } from 'mobx';

export enum ThemePreference {
  System,
  PreferLight,
  PreferDark,
}

export default class ThemeStore {
  preference = ThemePreference.System;

  systemDarkMode: boolean;

  constructor() {
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
    this.systemDarkMode = mediaQuery.matches;
    mediaQuery.addEventListener('change', (event) => {
      this.systemDarkMode = event.matches;
    });
    makeObservable(this, {
      preference: observable,
      systemDarkMode: observable,
      darkMode: computed,
      toggleDarkMode: action,
    });
  }

  get darkMode(): boolean {
    switch (this.preference) {
      case ThemePreference.PreferLight:
        return false;
      case ThemePreference.PreferDark:
        return true;
      default:
        return this.systemDarkMode;
    }
  }

  toggleDarkMode(): void {
    if (this.darkMode) {
      this.preference = this.systemDarkMode
        ? ThemePreference.PreferLight
        : ThemePreference.System;
    } else {
      this.preference = this.systemDarkMode
        ? ThemePreference.System
        : ThemePreference.PreferDark;
    }
  }
}
