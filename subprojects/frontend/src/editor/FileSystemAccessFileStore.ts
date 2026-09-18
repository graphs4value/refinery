/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { action, makeObservable } from 'mobx';

import {
  REFINERY_CONTENT_TYPE,
  FILE_TYPE_OPTIONS,
  type OpenResult,
  type OpenTextFileResult,
  FileIOError,
  openTextFile,
  saveTextFile,
  saveBlob,
} from '../utils/fileIO';
import getLogger from '../utils/getLogger';

import FileStore, { type SaveCompletion } from './FileStore';

const log = getLogger('editor.FileSystemAccessFileStore');

const FILE_PICKER_OPTIONS: FilePickerOptions = {
  id: 'problem',
  ...FILE_TYPE_OPTIONS,
};

export default class FileSystemAccessFileStore extends FileStore {
  private fileHandle: FileSystemFileHandle | undefined;

  constructor(
    initialFileName: string | undefined,
    private readonly onFileOpened: (text: string) => void,
    private readonly onFileSaved: () => void,
    onError: (title: string, body: string) => void,
  ) {
    super(initialFileName, onError);
    makeObservable<
      FileSystemAccessFileStore,
      'fileHandle' | 'fileOpened' | 'fileSavedAs' | 'setFile'
    >(this, {
      fileHandle: false,
      clearFile: action,
      openFile: action,
      openShare: false,
      setFile: action,
      fileOpened: action,
      saveFile: action,
      saveFileAs: action,
      fileSavedAs: action,
    });
  }

  openFile(): boolean {
    openTextFile(FILE_PICKER_OPTIONS)
      .then((result) => this.fileOpened(result))
      .catch((err: unknown) => {
        log.error({ err }, 'Failed to open file');
        const fileName = err instanceof FileIOError ? err.fileName : undefined;
        this.openFileFailed(fileName, err);
      });
    return true;
  }

  clearFile(): void {
    this.fileName = undefined;
    this.fileHandle = undefined;
  }

  openShare(fragment: string): void {
    const url = new URL(window.location.pathname, window.location.origin);
    url.hash = fragment;
    window.open(url, '_blank', 'noopener');
  }

  private setFile({ name, handle }: OpenResult): void {
    log.info('Opened file: %s', name);
    this.fileName = name;
    this.fileHandle = handle;
  }

  private fileOpened(result: OpenTextFileResult): void {
    this.onFileOpened(result.text);
    this.setFile(result);
  }

  saveFile(text: string, onComplete?: SaveCompletion): boolean {
    if (this.fileHandle === undefined) {
      return this.saveFileAs(text, onComplete);
    }
    saveTextFile(this.fileHandle, text)
      .then(() => {
        this.onFileSaved();
        onComplete?.(true);
      })
      .catch((err: unknown) => {
        log.error({ err }, 'Failed to save file');
        this.saveFileFailed(
          err instanceof FileIOError ? err.fileName : undefined,
          err,
        );
        onComplete?.(false);
      });
    return true;
  }

  saveFileAs(text: string, onComplete?: SaveCompletion): boolean {
    const blob = new Blob([text], {
      type: REFINERY_CONTENT_TYPE,
    });
    saveBlob(
      blob,
      this.fileName ?? `${this.simpleNameOrFallback}.problem`,
      FILE_PICKER_OPTIONS,
    )
      .then((result) => this.fileSavedAs(result, onComplete))
      .catch((err: unknown) => {
        log.error({ err }, 'Failed to save file as');
        this.saveFileFailed(
          err instanceof FileIOError ? err.fileName : undefined,
          err,
        );
        onComplete?.(false);
      });
    return true;
  }

  private fileSavedAs(result: OpenResult, onComplete?: SaveCompletion): void {
    this.setFile(result);
    this.onFileSaved();
    onComplete?.(true);
  }
}
