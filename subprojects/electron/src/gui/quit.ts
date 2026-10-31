/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import path from 'node:path';

import { app, BrowserWindow, dialog } from 'electron';

import getLogger from '../logger/getLogger';

import { openWindowsStore } from './OpenWindowsStore';

const logger = getLogger('gui.quit');

let quitAllowed = false;
let quitDialogOpen = false;

function focusWindow(browserWindow: BrowserWindow): void {
  if (browserWindow.isDestroyed()) {
    return;
  }
  if (browserWindow.isMinimized()) {
    browserWindow.restore();
  }
  browserWindow.focus();
}

function closeAllWindowsAndQuit(): void {
  quitAllowed = true;
  // Destroying the windows skips their renderer beforeunload handlers, which
  // would otherwise ask about the same unsaved changes again.
  for (const browserWindow of BrowserWindow.getAllWindows()) {
    browserWindow.destroy();
  }
  app.quit();
}

function getUnsavedChangesDetail(
  unsavedWindows: readonly BrowserWindow[],
): string {
  const fileNames: string[] = [];
  let untitledCount = 0;
  for (const browserWindow of unsavedWindows) {
    const { filePath } = openWindowsStore.getWindowStore(browserWindow);
    if (filePath === undefined) {
      untitledCount++;
    } else {
      fileNames.push(path.basename(filePath));
    }
  }
  const details = [
    'Files with unsaved changes:',
    ...fileNames.map((name) => `• ${name}`),
    ...(untitledCount === 0
      ? []
      : [
          `• ${untitledCount} untitled ${
            untitledCount === 1 ? 'document' : 'documents'
          }`,
        ]),
  ];
  return details.join('\n');
}

export default function attachQuitHandler(): void {
  app.on('before-quit', (event) => {
    // This runs before `will-quit`, where the main entrypoint starts cleanup.
    if (quitAllowed) {
      return;
    }
    event.preventDefault();
    const unsavedWindows = openWindowsStore.findWindowsWithUnsavedChanges();
    const unsavedWindow = unsavedWindows[0];
    if (unsavedWindow === undefined) {
      closeAllWindowsAndQuit();
      return;
    }
    if (quitDialogOpen) {
      return;
    }
    quitDialogOpen = true;
    dialog
      .showMessageBox({
        type: 'warning',
        buttons: ['Quit anyway', 'Cancel'],
        defaultId: 1,
        cancelId: 1,
        noLink: true,
        title: 'Quit Refinery?',
        message: 'There are unsaved changes.',
        detail: getUnsavedChangesDetail(unsavedWindows),
      })
      .then(({ response }) => {
        if (response === 0) {
          closeAllWindowsAndQuit();
        } else {
          focusWindow(unsavedWindow);
        }
      })
      .catch((error: unknown) => {
        logger.error({ err: error }, 'Failed to show quit confirmation');
        focusWindow(unsavedWindow);
      })
      .finally(() => {
        quitDialogOpen = false;
      });
  });
}
