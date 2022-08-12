import { makeAutoObservable } from 'mobx';

import EditorTheme from './EditorTheme';

export default class ThemeStore {
  currentTheme: EditorTheme = EditorTheme.Default;

  constructor() {
    makeAutoObservable(this);
  }

  toggleDarkMode(): void {
    switch (this.currentTheme) {
      case EditorTheme.Light:
        this.currentTheme = EditorTheme.Dark;
        break;
      case EditorTheme.Dark:
        this.currentTheme = EditorTheme.Light;
        break;
      default:
        throw new Error(`Unknown theme: ${this.currentTheme}`);
    }
  }

  get darkMode(): boolean {
    return this.currentTheme === EditorTheme.Dark;
  }
}
