/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import type RefineryContextBridge from '@tools.refinery/frontend/RefineryContextBridge';
import type {
  FileErrorResult,
  FileResultOrError,
  OpenFileResultOrError,
  ReadFileResult,
  EditorCommand,
  EditorCommandCallback,
  CLISymlinkResult,
  CLISymlinkPromptClaimResult,
  CLISymlinkState,
  CLISymlinkStatusChangeCallback,
  PathSelectionResult,
  RestartServerResult,
  ServerSettingsResponse,
  ServerStateChangeCallback,
  ThemeSource,
  ThemeSourceChangeCallback,
} from '@tools.refinery/frontend/RefineryContextBridge';
import type BackendConfig from '@tools.refinery/frontend/xtext/BackendConfig';
import { contextBridge, ipcRenderer, webUtils } from 'electron';

import { loggerContextBridge, getLogger } from '../logger/preloadLogger';

const logger = getLogger('gui.preload');

const serverStateCallbacks: ServerStateChangeCallback[] = [];

const themeSourceCallbacks: ThemeSourceChangeCallback[] = [];

const cliSymlinkStatusCallbacks: CLISymlinkStatusChangeCallback[] = [];

const editorCommandCallbacks: EditorCommandCallback[] = [];

const openDialogs = new Set<string>();

ipcRenderer.on(
  'refinery:serverStateChanged',
  (_event, serverState: boolean) => {
    for (const callback of serverStateCallbacks) {
      callback(serverState);
    }
  },
);

ipcRenderer.on(
  'refinery:themeSourceChanged',
  (_event, themeSource: ThemeSource) => {
    for (const callback of themeSourceCallbacks) {
      callback(themeSource);
    }
  },
);

ipcRenderer.on(
  'refinery:cliSymlinkStatusChanged',
  (_event, state: CLISymlinkState) => {
    for (const callback of cliSymlinkStatusCallbacks) {
      callback(state);
    }
  },
);

ipcRenderer.on('refinery:editorCommand', (_event, command: unknown) => {
  for (const callback of editorCommandCallbacks) {
    callback(command as EditorCommand);
  }
});

function updateDialogCount() {
  ipcRenderer.send('refinery:setModalDialogCount', openDialogs.size);
}

contextBridge.exposeInMainWorld('refinery', {
  ...loggerContextBridge,
  async getBackendConfig() {
    return ipcRenderer.invoke(
      'refinery:getBackendConfig',
    ) as Promise<BackendConfig>;
  },
  async getServerSettings() {
    return ipcRenderer.invoke(
      'refinery:getServerSettings',
    ) as Promise<ServerSettingsResponse>;
  },
  onCLISymlinkStatusChange(callback) {
    if (typeof callback !== 'function') {
      return;
    }
    cliSymlinkStatusCallbacks.push(callback);
    (
      ipcRenderer.invoke(
        'refinery:getCLISymlinkStatus',
      ) as Promise<CLISymlinkState>
    )
      .then(callback)
      .catch((error) =>
        logger.error({ err: error }, 'Failed to get CLI symlink status'),
      );
  },
  async claimCLISymlinkPrompt() {
    return ipcRenderer.invoke(
      'refinery:claimCLISymlinkPrompt',
    ) as Promise<CLISymlinkPromptClaimResult>;
  },
  async setCLISymlink(enabled) {
    return ipcRenderer.invoke(
      'refinery:setCLISymlink',
      enabled,
    ) as Promise<CLISymlinkResult>;
  },
  async setCLISymlinkPreference(enabled) {
    return ipcRenderer.invoke(
      'refinery:setCLISymlinkPreference',
      enabled,
    ) as Promise<CLISymlinkResult>;
  },
  getPathForFile(file) {
    return webUtils.getPathForFile(file as File);
  },
  async selectLibraryDirectory() {
    return ipcRenderer.invoke(
      'refinery:selectLibraryDirectory',
    ) as Promise<PathSelectionResult>;
  },
  async selectClasspathJar() {
    return ipcRenderer.invoke(
      'refinery:selectClasspathJar',
    ) as Promise<PathSelectionResult>;
  },
  async restartServer(serverSettings) {
    return (
      serverSettings === undefined
        ? ipcRenderer.invoke('refinery:restartServer')
        : ipcRenderer.invoke('refinery:restartServer', serverSettings)
    ) as Promise<RestartServerResult>;
  },
  onServerStateChange(callback) {
    if (typeof callback !== 'function') {
      return;
    }
    serverStateCallbacks.push(callback);
    (ipcRenderer.invoke('refinery:getServerState') as Promise<boolean>)
      .then(callback)
      .catch((error) =>
        logger.error({ err: error }, 'Failed to get server state'),
      );
  },
  setThemeSource(themeSource) {
    logger.info({ themeSource }, 'Setting theme source');
    ipcRenderer.send('refinery:setThemeSource', themeSource);
  },
  onThemeSourceChange(callback) {
    if (typeof callback !== 'function') {
      return;
    }
    themeSourceCallbacks.push(callback);
    (ipcRenderer.invoke('refinery:getThemeSource') as Promise<ThemeSource>)
      .then(callback)
      .catch((error) =>
        logger.error({ err: error }, 'Failed to get theme source'),
      );
  },
  onEditorCommand(callback) {
    if (typeof callback === 'function') {
      editorCommandCallbacks.push(callback);
    }
  },
  setUnsavedChanges(unsavedChanges) {
    ipcRenderer.send('refinery:setUnsavedChanges', unsavedChanges);
  },
  async readFile() {
    return ipcRenderer.invoke('refinery:readFile') as Promise<ReadFileResult>;
  },
  async openFile(newWindow) {
    return ipcRenderer.invoke(
      'refinery:openFile',
      newWindow,
    ) as Promise<OpenFileResultOrError>;
  },
  async clearFile() {
    return ipcRenderer.invoke('refinery:clearFile') as Promise<FileErrorResult>;
  },
  async openHash(hash) {
    return ipcRenderer.invoke(
      'refinery:openHash',
      hash,
    ) as Promise<FileErrorResult>;
  },
  async saveFile(text) {
    return ipcRenderer.invoke(
      'refinery:saveFile',
      text,
    ) as Promise<FileResultOrError>;
  },
  async saveFileAs(text) {
    return ipcRenderer.invoke(
      'refinery:saveFileAs',
      text,
    ) as Promise<FileResultOrError>;
  },
  openDialog(id) {
    logger.info({ dialogID: id }, 'Opening dialog');
    if (openDialogs.has(id)) {
      logger.error({ dialogID: id }, 'Duplicate dialog');
    } else {
      openDialogs.add(id);
      updateDialogCount();
    }
  },
  closeDialog(id) {
    logger.info({ dialogID: id }, 'Closing dialog');
    if (openDialogs.delete(id)) {
      updateDialogCount();
    } else {
      logger.error({ dialogID: id }, 'Unknown dialog');
    }
  },
} satisfies RefineryContextBridge);

// Clear the previous dialog count on page reload.
updateDialogCount();
