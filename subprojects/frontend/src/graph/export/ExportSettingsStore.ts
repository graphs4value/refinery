/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { makeAutoObservable } from 'mobx';

export type ExportFormat = 'svg' | 'pdf' | 'png';
export type StaticTheme = 'light' | 'dark';
export type ExportTheme = StaticTheme | 'dynamic';

export default class ExportSettingsStore {
  format: ExportFormat = 'svg';

  private staticTheme: StaticTheme = 'light';

  private _theme: ExportTheme = 'light';

  private _transparent = true;

  private embedSVGFonts = false;

  private embedPDFFonts = true;

  scale = 100;

  constructor() {
    makeAutoObservable(this);
  }

  setFormat(format: ExportFormat): void {
    this.format = format;
  }

  setTheme(theme: ExportTheme): void {
    this._theme = theme;
    if (theme !== 'dynamic') {
      this.staticTheme = theme;
    }
  }

  toggleTransparent(): void {
    this._transparent = !this._transparent;
  }

  toggleEmbedFonts(): void {
    this.embedFonts = !this.embedFonts;
  }

  setScale(scale: number): void {
    this.scale = scale;
  }

  get theme(): ExportTheme {
    return this.format === 'svg' ? this._theme : this.staticTheme;
  }

  get transparent(): boolean {
    return this.theme === 'dynamic' ? true : this._transparent;
  }

  get embedFonts(): boolean {
    if (this.theme === 'dynamic') {
      return false;
    }
    return this.format === 'pdf' ? this.embedPDFFonts : this.embedSVGFonts;
  }

  private set embedFonts(embedFonts: boolean) {
    if (this.format === 'pdf') {
      this.embedPDFFonts = embedFonts;
    }
    this.embedSVGFonts = embedFonts;
  }

  get canSetDynamicTheme(): boolean {
    return this.format === 'svg';
  }

  get canChangeTransparency(): boolean {
    return this.theme !== 'dynamic';
  }

  get canEmbedFonts(): boolean {
    return (
      (this.format === 'svg' || this.format === 'pdf') &&
      this.theme !== 'dynamic'
    );
  }

  get canScale(): boolean {
    return this.format === 'png';
  }
}
