import { action, makeObservable, observable } from 'mobx';

export default class ThemeStore {
  darkMode = true;

  constructor() {
    makeObservable(this, {
      darkMode: observable,
      toggleDarkMode: action,
    });
  }

  toggleDarkMode(): void {
    this.darkMode = !this.darkMode;
  }
}
