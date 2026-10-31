/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { computed, makeObservable, observable } from 'mobx';

export type SaveCompletion = (saved: boolean) => void;

export default abstract class FileStore {
  fileName: string | undefined;

  constructor(
    initialFileName: string | undefined,
    private readonly onError: (title: string, body: string) => void,
  ) {
    this.fileName = initialFileName;
    makeObservable<
      FileStore,
      'openFileFailed' | 'reportError' | 'saveFileFailed'
    >(this, {
      fileName: observable,
      simpleName: computed,
      simpleNameOrFallback: computed,
      clearFile: false,
      reportError: false,
      openFile: false,
      openFileInNewWindow: false,
      openFileFailed: false,
      openShare: false,
      saveFileFailed: false,
      saveFile: false,
      saveFileAs: false,
    });
  }

  protected reportError(title: string, body: string, error?: unknown): void {
    if (error instanceof DOMException && error.name === 'AbortError') {
      return;
    }
    this.onError(title, body);
  }

  protected openFileFailed(fileName?: string, error?: unknown): void {
    this.reportError(
      'Failed to open file',
      fileName === undefined
        ? 'The selected file could not be opened.'
        : `The selected file “${fileName}” could not be opened.`,
      error,
    );
  }

  protected saveFileFailed(fileName?: string, error?: unknown): void {
    this.reportError(
      'Failed to save file',
      fileName === undefined
        ? 'The file could not be saved.'
        : `The file “${fileName}” could not be saved.`,
      error,
    );
  }

  get simpleName(): string | undefined {
    const { fileName } = this;
    if (fileName === undefined) {
      return undefined;
    }
    const index = fileName.lastIndexOf('.');
    if (index < 0) {
      return fileName;
    }
    return fileName.substring(0, index);
  }

  get simpleNameOrFallback(): string {
    return this.simpleName ?? 'graph';
  }

  abstract openFile(): boolean;

  openFileInNewWindow(): boolean {
    return false;
  }

  abstract clearFile(): void;

  abstract openShare(fragment: string): void;

  abstract saveFile(text: string, onComplete?: SaveCompletion): boolean;

  abstract saveFileAs(text: string, onComplete?: SaveCompletion): boolean;
}
