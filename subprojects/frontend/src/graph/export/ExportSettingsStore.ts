/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { makeAutoObservable } from 'mobx';

export type ExportFormat = 'svg' | 'pdf' | 'png';
export type ExportTheme = 'light' | 'dark' | 'dynamic';

export default class ExportSettingsStore {
  format: ExportFormat = 'svg';

  theme: ExportTheme = 'light';

  transparent = true;

  embedSVGFonts = false;

  embedPDFFonts = true;

  scale = 100;

  constructor() {
    makeAutoObservable(this);
  }

  setFormat(format: ExportFormat): void {
    this.format = format;
    if (this.theme === 'dynamic' && this.format !== 'svg') {
      this.theme = 'light';
    }
  }

  setTheme(theme: ExportTheme): void {
    this.theme = theme;
    if (this.theme === 'dynamic') {
      this.format = 'svg';
      this.transparent = true;
    }
  }

  toggleTransparent(): void {
    this.transparent = !this.transparent;
    if (!this.transparent && this.theme === 'dynamic') {
      this.theme = 'light';
    }
  }

  toggleEmbedFonts(): void {
    this.embedFonts = !this.embedFonts;
  }

  setScale(scale: number): void {
    this.scale = scale;
  }

  get embedFonts(): boolean {
    return this.format === 'pdf' ? this.embedPDFFonts : this.embedSVGFonts;
  }

  private set embedFonts(embedFonts: boolean) {
    if (this.format === 'pdf') {
      this.embedPDFFonts = embedFonts;
    }
    this.embedSVGFonts = embedFonts;
    if (this.embedSVGFonts && this.theme === 'dynamic') {
      this.theme = 'light';
    }
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
