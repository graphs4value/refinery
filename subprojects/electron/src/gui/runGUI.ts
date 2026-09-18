/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import path from 'node:path';

import { isShareFragment } from '@tools.refinery/frontend/persistence/shareURI';
import { app, BrowserWindow, ipcMain, nativeTheme, screen } from 'electron';
import { comparer, reaction } from 'mobx';
import z from 'zod/v4';

import enableWebContentsLogger from '../logger/enableWebContentsLogger';
import getLogger from '../logger/getLogger';
import startServer from '../server/startServer';
import settings from '../settings';
import { onCleanup } from '../utils/cleanup';
import hardenWebContents from '../utils/hardenWebContents';
import { isMac, isWindows } from '../utils/platform';

import OpenRequestHandler, { type OpenRequest } from './OpenRequestHandler';
import { openWindowsStore } from './OpenWindowsStore';
import attachApplicationMenu from './applicationMenu';
import attachCLISymlinkHandlers from './cliSymlink';
import attachFileIOHandlers, { focusWindowForFile } from './fileIO';
import getTheme, {
  attachNativeThemeHandler,
  attachWindowThemeHandler,
} from './getTheme';
import attachQuitHandler from './quit';
import resolveOpenArguments, {
  resolveOpenArgument,
} from './resolveOpenArguments';
import getWindowSize, {
  MIN_WINDOW_HEIGHT,
  MIN_WINDOW_WIDTH,
} from './windowSize';

const logger = getLogger('gui.runGUI');

const OpenRequestData = z.union([
  z.object({ filePath: z.string() }),
  z.object({ hash: z.string() }),
]);

const AdditionalData = z.object({
  requests: z.array(OpenRequestData),
});

const Hash = z.string().refine(isShareFragment);

const openRequests = new OpenRequestHandler();

function createWindow(
  pageURL: string,
  allowedOrigins: string[],
  request?: OpenRequest,
): BrowserWindow {
  const { backgroundColor, accentColor, titleBarOverlay } = getTheme(
    nativeTheme.shouldUseDarkColors,
  );
  const windowState =
    openWindowsStore.lastActiveWindowState ?? settings.windowState;
  const { width, height } = getWindowSize(
    windowState,
    screen.getAllDisplays().map(({ workAreaSize }) => workAreaSize),
  );
  const restoreMaximized = !isMac && windowState.maximized;

  const window = new BrowserWindow({
    width,
    height,
    minWidth: MIN_WINDOW_WIDTH,
    minHeight: MIN_WINDOW_HEIGHT,
    webPreferences: {
      preload: path.join(__dirname, 'gui', 'preload.js'),
      devTools: process.isDev,
    },
    show: false,
    autoHideMenuBar: true,
    title: 'Refinery',
    backgroundColor,
    ...(isWindows ? {} : { accentColor }),
    titleBarStyle: isMac ? 'hiddenInset' : 'hidden',
    ...(isMac ? {} : { titleBarOverlay }),
  });
  const windowStore = openWindowsStore.createWindowStore(window);
  if (request !== undefined) {
    if ('filePath' in request) {
      windowStore.setFilePath(request.filePath);
    } else {
      windowStore.setHash(request.hash);
    }
  }
  attachWindowThemeHandler(window);

  window.once('ready-to-show', () => {
    if (restoreMaximized) {
      window.maximize();
    }
    window.show();
    if (process.isDev) {
      window.webContents.openDevTools();
    }
  });

  const { webContents } = window;

  hardenWebContents(webContents, allowedOrigins, [
    'clipboard-read',
    'clipboard-sanitized-write',
    'fileSystem',
  ]);
  enableWebContentsLogger(webContents);

  window
    .loadURL(pageURL)
    .catch((error) => logger.error({ err: error }, 'Failed to load webpage'));

  return window;
}

export default async function runGUI() {
  if (process.defaultApp) {
    const entryPoint = process.argv[1];
    if (entryPoint !== undefined) {
      app.setAsDefaultProtocolClient('refinery', process.execPath, [
        path.resolve(entryPoint),
      ]);
    }
  } else {
    app.setAsDefaultProtocolClient('refinery');
  }

  if (isMac) {
    app.on('open-file', (event, filePath) => {
      const request = resolveOpenArgument(filePath, process.cwd());
      if (request !== undefined) {
        event.preventDefault();
        openRequests.openInitial(request);
      }
    });
    app.on('open-url', (event, url) => {
      const request = resolveOpenArgument(url, process.cwd());
      if (request !== undefined) {
        event.preventDefault();
        openRequests.openInitial(request);
      }
    });
  }

  const args = process.argv.slice(process.defaultApp ? 2 : 1);
  const requests = resolveOpenArguments(args, process.cwd());
  if (!app.requestSingleInstanceLock({ requests })) {
    app.quit();
    return;
  }
  for (const request of requests) {
    openRequests.openInitial(request);
  }
  app.on('second-instance', (_event, _argv, _workingDirectory, rawData) => {
    const data = AdditionalData.safeParse(rawData);
    if (!data.success) {
      logger.error({ err: data.error }, 'Failed to parse second instance data');
      return;
    }
    if (data.data.requests.length === 0) {
      openRequests.open();
    } else {
      for (const request of data.data.requests) {
        openRequests.open(request);
      }
    }
  });

  app.on('window-all-closed', () => {
    if (!isMac) {
      app.quit();
    }
  });

  const [{ root: pageURL, backend, allowedOrigins }] = await Promise.all([
    startServer(),
    settings.readSettings(),
  ]);

  const openWindow = (request: OpenRequest | undefined) => {
    if (request !== undefined && 'filePath' in request) {
      const existingWindow = focusWindowForFile(request.filePath);
      if (existingWindow !== undefined) {
        return existingWindow;
      }
    }
    return createWindow(pageURL, allowedOrigins, request);
  };

  // `startServer()` waits for `app.whenReady()` internally, so this is safe to do here.
  // Register the quit guard before any cleanup handlers can run from `will-quit`.
  attachQuitHandler();
  attachApplicationMenu(openRequests);
  attachCLISymlinkHandlers();
  attachFileIOHandlers(openWindow);
  attachNativeThemeHandler();
  const disposeWindowStateReaction = reaction(
    () => {
      const windowState = openWindowsStore.lastActiveWindowState;
      return windowState === undefined
        ? undefined
        : {
            ...windowState,
            maximized: !isMac && windowState.maximized,
          };
    },
    (windowState) => {
      if (windowState !== undefined) {
        settings.setWindowState(windowState);
      }
    },
    { equals: comparer.structural },
  );
  onCleanup(disposeWindowStateReaction);

  ipcMain.handle('refinery:openHash', (_event, rawHash: unknown) => {
    try {
      const hash = Hash.parse(rawHash);
      openRequests.open({ hash });
    } catch (error) {
      logger.error(
        { err: error, hash: rawHash },
        'Failed to open shared model',
      );
      return { error: true };
    }
    return undefined;
  });

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      openRequests.open();
    }
  });

  const [window] = openRequests.initialize(openWindow);

  // Only start the backend once the UI is ready to show to avoid CPU contention
  // slowing down time to UI interactivity.
  await new Promise<void>((resolve, reject) => {
    window.once('ready-to-show', () => {
      resolve();
    });
    window.webContents.on(
      'did-fail-load',
      (_event, _errorCode, errorDescription) => {
        reject(new Error(errorDescription));
      },
    );
  });

  await backend.start();
}
