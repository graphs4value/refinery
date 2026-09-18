/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { action, makeObservable } from 'mobx';

import RefineryContextBridge, {
  FileErrorResult,
  FileResultOrError,
  OpenFileResultOrError,
  type FileResult,
} from '../RefineryContextBridge';
import getLogger from '../utils/getLogger';

import FileStore, { type SaveCompletion } from './FileStore';

const log = getLogger('editor.ElectronFileStore');

export default class ElectronFileStore extends FileStore {
  constructor(
    private readonly refinery: RefineryContextBridge,
    initialFileName: string | undefined,
    private readonly onFileOpened: (text: string) => void,
    private readonly onFileSaved: () => void,
    onError: (title: string, body: string) => void,
  ) {
    super(initialFileName, onError);
    makeObservable<
      ElectronFileStore,
      | 'clearFileFailed'
      | 'fileOpened'
      | 'fileAlreadyOpen'
      | 'fileSaved'
      | 'openFileWithOption'
      | 'openShareFailed'
      | 'setFile'
    >(this, {
      clearFile: action,
      clearFileFailed: false,
      openFile: action,
      openFileInNewWindow: action,
      openShare: false,
      openShareFailed: false,
      fileOpened: action,
      fileAlreadyOpen: false,
      saveFile: action,
      saveFileAs: action,
      fileSaved: action,
      openFileWithOption: false,
      setFile: action,
    });
  }

  openFile(): boolean {
    return this.openFileWithOption(false);
  }

  override openFileInNewWindow(): boolean {
    return this.openFileWithOption(true);
  }

  private openFileWithOption(newWindow: boolean): boolean {
    this.refinery
      .openFile(newWindow)
      .then((result) => OpenFileResultOrError.parse(result))
      .then((result) => this.fileOpened(result))
      .catch((err: unknown) => {
        log.error({ err }, 'Failed to open file');
        this.openFileFailed(undefined, err);
      });
    return true;
  }

  clearFile(): void {
    const { fileName } = this;
    this.fileName = undefined;
    this.refinery
      .clearFile()
      .then((result) => FileErrorResult.parse(result))
      .then((result) => {
        if (result !== undefined) {
          this.clearFileFailed(result.name ?? fileName);
        }
      })
      .catch((err: unknown) => {
        log.error({ err }, 'Failed to clear open file');
        this.clearFileFailed(fileName);
      });
  }

  private clearFileFailed(fileName: string | undefined): void {
    this.reportError(
      'Failed to open shared link safely',
      fileName === undefined
        ? 'Do not save this window, because it may overwrite the previously opened file. Close the window and try opening the shared link again.'
        : `Do not save this window, because it may overwrite the file “${fileName}”. Close the window and try opening the shared link again.`,
    );
  }

  openShare(fragment: string): void {
    this.refinery
      .openHash(fragment)
      .then((result) => FileErrorResult.parse(result))
      .then((result) => {
        if (result !== undefined) {
          this.openShareFailed();
        }
      })
      .catch((err: unknown) => {
        log.error({ err }, 'Failed to open shared model');
        this.openShareFailed(err);
      });
  }

  private fileOpened(result: OpenFileResultOrError): void {
    if (result === undefined) {
      return;
    }
    if ('error' in result) {
      if (result.reason === 'invalidUtf8') {
        this.reportError(
          'Failed to open file',
          result.name === undefined
            ? 'The selected file is not valid UTF-8 and could not be opened.'
            : `The selected file “${result.name}” is not valid UTF-8 and could not be opened.`,
        );
        return;
      }
      this.openFileFailed(result.name);
      return;
    }
    this.onFileOpened(result.text);
    this.setFile(result);
  }

  saveFile(text: string, onComplete?: SaveCompletion): boolean {
    this.refinery
      .saveFile(text)
      .then((result) => FileResultOrError.parse(result))
      .then((result) => this.fileSaved(result, onComplete))
      .catch((err: unknown) => {
        log.error({ err }, 'Failed to save file');
        this.saveFileFailed(this.fileName, err);
        onComplete?.(false);
      });
    return true;
  }

  saveFileAs(text: string, onComplete?: SaveCompletion): boolean {
    this.refinery
      .saveFileAs(text)
      .then((result) => FileResultOrError.parse(result))
      .then((result) => this.fileSaved(result, onComplete))
      .catch((err: unknown) => {
        log.error({ err }, 'Failed to save file as');
        this.saveFileFailed(undefined, err);
        onComplete?.(false);
      });
    return true;
  }

  private fileSaved(
    result: FileResultOrError,
    onComplete?: SaveCompletion,
  ): void {
    if (result === undefined) {
      onComplete?.(false);
      return;
    }
    if ('error' in result) {
      if (result.reason === 'alreadyOpen') {
        this.fileAlreadyOpen(result.name);
        onComplete?.(false);
        return;
      }
      this.saveFileFailed(result.name);
      onComplete?.(false);
      return;
    }
    this.setFile(result);
    this.onFileSaved();
    onComplete?.(true);
  }

  private setFile({ name }: FileResult): void {
    log.info('Opened file: %s', name);
    this.fileName = name;
  }

  private openShareFailed(error?: unknown): void {
    this.reportError(
      'Failed to open shared link',
      'The shared link could not be opened.',
      error,
    );
  }

  private fileAlreadyOpen(fileName: string | undefined): void {
    this.reportError(
      'Failed to save file',
      fileName === undefined
        ? 'The selected file is already open in another window.'
        : `The file “${fileName}” is already open in another window.`,
    );
  }
}
