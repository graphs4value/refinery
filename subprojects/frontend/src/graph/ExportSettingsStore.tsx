/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { makeAutoObservable } from 'mobx';

export type ExportFormat = 'svg' | 'png';
export type ExportTheme = 'light' | 'dark';

export default class ExportSettingsStore {
  format: ExportFormat = 'svg';

  theme: ExportTheme = 'light';

  transparent = true;

  embedFonts = false;

  scale = 100;

  constructor() {
    makeAutoObservable(this);
  }

  setFormat(format: ExportFormat): void {
    this.format = format;
  }

  setTheme(theme: ExportTheme): void {
    this.theme = theme;
  }

  toggleTransparent(): void {
    this.transparent = !this.transparent;
  }

  toggleEmbedFonts(): void {
    this.embedFonts = !this.embedFonts;
  }

  setScale(scale: number): void {
    this.scale = scale;
  }
}
