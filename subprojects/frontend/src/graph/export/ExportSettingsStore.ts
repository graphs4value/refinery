/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { makeAutoObservable } from 'mobx';

const validFormats = ['svg', 'pdf', 'png', 'refinery'] as const;
const validStaticThemes = ['light', 'dark'] as const;
const validThemes = [...validStaticThemes, 'dynamic'] as const;

export type ExportFormat = (typeof validFormats)[number];
export type StaticTheme = (typeof validStaticThemes)[number];
export type ExportTheme = (typeof validThemes)[number];

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

  setFormat(format: string): void {
    if ((validFormats as readonly string[]).includes(format)) {
      this.format = format as ExportFormat;
    }
  }

  setTheme(theme: string): void {
    if ((validThemes as readonly string[]).includes(theme)) {
      this._theme = theme as ExportTheme;
      if (theme !== 'dynamic') {
        this.staticTheme = theme as StaticTheme;
      }
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

  get canSetTheme(): boolean {
    return !this.plainText;
  }

  get canSetDynamicTheme(): boolean {
    return this.format === 'svg';
  }

  get canChangeTransparency(): boolean {
    return !this.plainText && this.theme !== 'dynamic';
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

  get canCopy(): boolean {
    return this.format === 'png' || this.plainText;
  }

  get plainText(): boolean {
    return this.format === 'refinery';
  }
}
