/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import type { EditorCommand } from '@tools.refinery/frontend/RefineryContextBridge';
import {
  BrowserWindow,
  clipboard,
  dialog,
  Menu,
  type BaseWindow,
  type MenuItem,
  type MenuItemConstructorOptions,
} from 'electron';
import { comparer, reaction } from 'mobx';

import getLogger from '../logger/getLogger';
import { onCleanup } from '../utils/cleanup';
import { isMac } from '../utils/platform';

import type OpenRequestHandler from './OpenRequestHandler';
import type { OpenRequest } from './OpenRequestHandler';
import { openWindowsStore } from './OpenWindowsStore';
import { selectFilePath } from './fileIO';
import { resolveOpenArgument } from './resolveOpenArguments';

const logger = getLogger('gui.applicationMenu');

interface ApplicationMenuItems {
  save: MenuItem;
  saveAs: MenuItem;
  copyLink: MenuItem;
  edit: MenuItem;
}

function openClipboardSharedLink(openRequests: OpenRequestHandler): void {
  let request: OpenRequest | undefined;
  try {
    request = resolveOpenArgument(clipboard.readText(), process.cwd());
  } catch (error) {
    logger.error({ err: error }, 'Failed to read shared link from clipboard');
  }
  if (request === undefined || !('hash' in request)) {
    dialog
      .showMessageBox({
        type: 'error',
        title: 'Failed to paste shared link',
        message: 'The clipboard does not contain a valid shared link.',
      })
      .catch((error: unknown) => {
        logger.error({ err: error }, 'Failed to show clipboard error');
      });
    return;
  }
  openRequests.open(request);
}

function sendEditorCommand(
  openRequests: OpenRequestHandler,
  browserWindow: BaseWindow | undefined,
  command: EditorCommand,
): void {
  const focusedWindow =
    browserWindow instanceof BrowserWindow
      ? browserWindow
      : BrowserWindow.getFocusedWindow();
  if (focusedWindow === null || focusedWindow === undefined) {
    if (command === 'openFile') {
      selectFilePath()
        .then((filePath) => {
          if (filePath !== undefined) {
            openRequests.open({ filePath });
          }
        })
        .catch((error: unknown) => {
          logger.error({ err: error }, 'Failed to select file to open');
        });
    } else if (command === 'pasteLink') {
      openClipboardSharedLink(openRequests);
    }
    return;
  }
  focusedWindow.webContents.send('refinery:editorCommand', command);
}

function createApplicationMenu(
  openRequests: OpenRequestHandler,
): ApplicationMenuItems {
  const template: MenuItemConstructorOptions[] = [
    ...(isMac ? [{ role: 'appMenu' as const }] : []),
    {
      label: 'File',
      submenu: [
        {
          label: 'Open…',
          accelerator: 'CommandOrControl+O',
          click: (_menuItem, browserWindow) => {
            sendEditorCommand(openRequests, browserWindow, 'openFile');
          },
        },
        {
          label: 'Save',
          id: 'save-file',
          accelerator: 'CommandOrControl+S',
          click: (_menuItem, browserWindow) => {
            sendEditorCommand(openRequests, browserWindow, 'saveFile');
          },
        },
        {
          label: 'Save As…',
          id: 'save-file-as',
          accelerator: 'CommandOrControl+Shift+S',
          click: (_menuItem, browserWindow) => {
            sendEditorCommand(openRequests, browserWindow, 'saveFileAs');
          },
        },
        { type: 'separator' },
        {
          label: 'Share…',
          id: 'copy-link',
          accelerator: 'CommandOrControl+Shift+X',
          click: (_menuItem, browserWindow) => {
            sendEditorCommand(openRequests, browserWindow, 'copyLink');
          },
        },
        {
          label: 'Paste shared link',
          accelerator: 'CommandOrControl+Shift+V',
          click: (_menuItem, browserWindow) => {
            sendEditorCommand(openRequests, browserWindow, 'pasteLink');
          },
        },
      ],
    },
    { id: 'edit-menu', role: 'editMenu' },
    {
      label: 'View',
      submenu: [
        { role: 'resetZoom' },
        { role: 'zoomIn' },
        { role: 'zoomOut' },
        { type: 'separator' },
        { role: 'togglefullscreen' },
        ...(process.isDev
          ? ([{ type: 'separator' }, { role: 'toggleDevTools' }] as const)
          : []),
      ],
    },
    { role: 'windowMenu' },
  ];
  const menu = Menu.buildFromTemplate(template);
  Menu.setApplicationMenu(menu);
  const getMenuItemByID = (id: string) => {
    const item = menu.getMenuItemById(id);
    if (item === null) {
      throw new Error(`Unknown menu item ${id}`);
    }
    return item;
  };
  return {
    save: getMenuItemByID('save-file'),
    saveAs: getMenuItemByID('save-file-as'),
    copyLink: getMenuItemByID('copy-link'),
    edit: getMenuItemByID('edit-menu'),
  };
}

export default function attachApplicationMenu(
  openRequests: OpenRequestHandler,
): void {
  const applicationMenuItems = createApplicationMenu(openRequests);
  const disposer = reaction(
    () => {
      const windowStore = openWindowsStore.lastActiveWindowStore;
      if (windowStore === undefined) {
        return {
          windowActive: false,
          canSave: false,
        };
      }
      return {
        windowActive: true,
        canSave:
          windowStore.filePath === undefined || windowStore.unsavedChanges,
      };
    },
    (state) => {
      applicationMenuItems.edit.enabled = state.windowActive;
      applicationMenuItems.save.enabled = state.canSave;
      applicationMenuItems.saveAs.enabled = state.windowActive;
      applicationMenuItems.copyLink.enabled = state.windowActive;
    },
    { equals: comparer.structural, fireImmediately: true },
  );
  onCleanup(disposer);
}
