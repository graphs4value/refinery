/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { Visibility } from '@tools.refinery/client';
import { getLogger } from 'loglevel';
import {
  IReactionDisposer,
  autorun,
  makeAutoObservable,
  runInAction,
} from 'mobx';

import PWAStore from './PWAStore';
import { EditorCommand, ReadFileResult } from './RefineryContextBridge';
import DialogStore from './dialog/DialogStore';
import type EditorStore from './editor/EditorStore';
import ExportSettingsStore from './graph/export/ExportSettingsStore';
import Compressor, {
  type DecompressFailure,
  type DecompressSource,
} from './persistence/Compressor';
import defaultInitialValue from './persistence/initialValue';
import CLISymlinkStore from './settings/CLISymlinkStore';
import ThemeStore from './theme/ThemeStore';
import isElectron from './utils/isElectron';
import fetchBackendConfig, {
  type BackendConfigWithDefaults,
} from './xtext/fetchBackendConfig';

const log = getLogger('RootStore');

export default class RootStore {
  private readonly compressor = new Compressor(
    this.setDecompressedValue.bind(this),
    this.decompressionFailed.bind(this),
  );

  private initialValue: string | undefined;

  private initialVisibility: Record<string, Visibility> | undefined;

  private initialFileName: string | undefined;

  private editorStoreClass: typeof EditorStore | undefined;

  backendConfig: BackendConfigWithDefaults | undefined;

  editorStore: EditorStore | undefined;

  readonly pwaStore: PWAStore;

  readonly dialogStore: DialogStore;

  readonly cliSymlinkStore: CLISymlinkStore;

  readonly themeStore: ThemeStore;

  readonly exportSettingsStore: ExportSettingsStore;

  disposed = false;

  private titleReaction: IReactionDisposer | undefined;

  private closeAllowed = false;

  private readonly beforeUnloadHandler = (event: BeforeUnloadEvent): void => {
    const { editorStore } = this;
    if (
      this.closeAllowed ||
      !editorStore?.unsavedChanges ||
      this.dialogStore.hasConfirmation('close')
    ) {
      return;
    }
    event.preventDefault();
    event.returnValue = true;
    if (!isElectron) {
      // Browsers cannot defer unload for an application-rendered dialog. Let
      // the platform show its native unsaved-changes prompt instead.
      return;
    }
    editorStore.closeRequested();
  };

  constructor() {
    this.pwaStore = new PWAStore();
    this.dialogStore = new DialogStore();
    this.cliSymlinkStore = new CLISymlinkStore(
      this.dialogStore,
      window.refinery,
    );
    this.themeStore = new ThemeStore();
    this.exportSettingsStore = new ExportSettingsStore();
    makeAutoObservable<
      RootStore,
      | 'compressor'
      | 'dialogStore'
      | 'cliSymlinkStore'
      | 'editorStoreClass'
      | 'titleReaction'
      | 'beforeUnloadHandler'
      | 'closeAllowed'
    >(this, {
      compressor: false,
      dialogStore: false,
      cliSymlinkStore: false,
      editorStoreClass: false,
      pwaStore: false,
      themeStore: false,
      exportSettingsStore: false,
      titleReaction: false,
      beforeUnloadHandler: false,
      closeAllowed: false,
    });
    window.addEventListener('beforeunload', this.beforeUnloadHandler);
    (async () => {
      const [backendConfig, { default: EditorStore }] = await Promise.all([
        fetchBackendConfig(),
        import('./editor/EditorStore'),
      ]);
      runInAction(() => {
        if (this.disposed) {
          return;
        }
        this.backendConfig = backendConfig;
        this.editorStoreClass = EditorStore;
        if (this.initialValue !== undefined) {
          this.setInitialValue(
            this.initialValue,
            this.initialVisibility,
            this.initialFileName,
          );
        }
      });
    })().catch((err: unknown) => {
      log.error({ err }, 'Failed to load EditorStore');
    });
    const { refinery } = window;
    if (refinery) {
      refinery.onEditorCommand((rawCommand) => {
        const result = EditorCommand.safeParse(rawCommand);
        if (!result.success) {
          log.error({ err: result.error }, 'Invalid editor command');
          return;
        }
        const { editorStore } = this;
        if (editorStore === undefined) {
          return;
        }
        switch (result.data) {
          case 'openFile':
            editorStore.openFile();
            break;
          case 'saveFile':
            editorStore.saveFile();
            break;
          case 'saveFileAs':
            editorStore.saveFileAs();
            break;
          case 'copyLink':
            editorStore.openShareDialog('copyLink');
            break;
          case 'pasteLink':
            editorStore.openShareDialog('pasteLink');
            break;
        }
      });
      refinery
        .readFile()
        .then((result) => ReadFileResult.parse(result))
        .then((result) => {
          if (result !== undefined && 'error' in result) {
            this.initialFileFailed(
              result.name,
              result.reason === 'invalidUtf8',
            );
            return;
          }
          if (result !== undefined && 'hash' in result) {
            this.compressor.decompressInitial(result.hash);
            return;
          }
          this.setInitialValue(
            result?.text ?? defaultInitialValue,
            undefined,
            result?.name,
          );
        })
        .catch((err: unknown) => {
          log.error({ err }, 'Failed to read initial file');
          this.initialFileFailed();
        });
    } else {
      this.compressor.decompressInitial();
    }
  }

  private initialFileFailed(fileName?: string, invalidUtf8 = false) {
    this.showError(
      'Failed to open file',
      invalidUtf8
        ? fileName === undefined
          ? 'The requested file is not valid UTF-8 and could not be opened.'
          : `The requested file “${fileName}” is not valid UTF-8 and could not be opened.`
        : fileName === undefined
          ? 'The requested file could not be opened.'
          : `The requested file “${fileName}” could not be opened.`,
      true,
    );
  }

  private decompressionFailed(
    source: DecompressSource,
    failure: DecompressFailure,
    error: Error,
  ): void {
    log.error({ err: error }, 'Failed to decompress shared state');
    const defaultLoaded = source === 'initial' && !isElectron;
    if (defaultLoaded) {
      this.setDecompressedValue(defaultInitialValue, undefined, source);
    }
    let body: string;
    if (failure === 'workerFailed') {
      body = defaultLoaded
        ? 'Shared-link processing is unavailable. The default model was loaded instead.'
        : 'The shared link could not be opened because shared-link processing is unavailable.';
    } else {
      body = defaultLoaded
        ? 'The shared link is invalid. The default model was loaded instead.'
        : 'The shared link is invalid and could not be opened.';
    }
    this.showError(
      'Failed to open shared link',
      body,
      source === 'initial' && isElectron,
    );
  }

  showError(title: string, body: string, fatal = false): void {
    this.dialogStore.showError(title, body, fatal);
  }

  closeErrorDialog(): void {
    if (this.dialogStore.errorDialog?.fatal) {
      this.closeWindow();
      return;
    }
    this.dialogStore.dismissError();
  }

  get errorDialog() {
    return this.dialogStore.errorDialog;
  }

  private closeWindow(): void {
    this.closeAllowed = true;
    window.close();
    // Browsers ignore window.close() for tabs they did not open themselves.
    // Reset the bypass so a subsequent close still asks about unsaved changes.
    setTimeout(() => {
      this.closeAllowed = false;
    });
  }

  private setDecompressedValue(
    value: string,
    visibility: Record<string, Visibility> | undefined,
    source: DecompressSource,
  ): void {
    if (this.editorStore === undefined) {
      this.setInitialValue(value, visibility);
    } else if (source === 'openShare') {
      this.editorStore.sharedModelOpened(value, visibility ?? {});
    } else {
      this.editorStore.fileOpened(value, visibility ?? {});
      if (source === 'hashChange') {
        this.editorStore.clearFile();
      }
    }
  }

  setInitialValue(
    initialValue: string,
    visibility: Record<string, Visibility> | undefined,
    fileName?: string,
  ): void {
    runInAction(() => {
      this.initialValue = initialValue;
      this.initialVisibility = visibility;
      this.initialFileName = fileName;
    });
    if (
      this.initialValue !== undefined &&
      this.backendConfig !== undefined &&
      this.editorStoreClass !== undefined &&
      this.editorStore === undefined
    ) {
      const EditorStore = this.editorStoreClass;
      const editorStore = new EditorStore(
        this.initialValue,
        this.initialFileName,
        this.initialVisibility,
        this.pwaStore,
        this.dialogStore,
        this.themeStore,
        this.backendConfig,
        this.compressor.compress.bind(this.compressor),
        this.compressor.getShareFragment.bind(this.compressor),
        this.compressor.decompress.bind(this.compressor),
        this.showError.bind(this),
        this.closeWindow.bind(this),
      );
      this.editorStore = editorStore;
      this.cliSymlinkStore.editorInitialized();
      this.titleReaction = autorun(() => {
        const { simpleName, unsavedChanges } = editorStore;
        if (simpleName === undefined) {
          document.title = 'Refinery';
        } else {
          // Chromium web apps don't like whe the file name precedes the app name,
          // and turn `filename - Refinery` into `Refinery - filename - Refinery`.
          // We elect to use just `Refinery - filename` instead.
          // Change indicator in a style similar to VSCodium.
          document.title = `Refinery - ${unsavedChanges ? '\u25cf ' : ''}${simpleName}`;
        }
      });
    }
  }

  get hasChat(): boolean {
    return this.backendConfig?.chatURL !== undefined;
  }

  dispose(): void {
    if (this.disposed) {
      return;
    }
    this.titleReaction?.();
    this.editorStore?.dispose();
    this.compressor.dispose();
    window.removeEventListener('beforeunload', this.beforeUnloadHandler);
    this.disposed = true;
  }
}
