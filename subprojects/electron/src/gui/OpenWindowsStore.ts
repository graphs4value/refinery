/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import type { BrowserWindow } from 'electron';
import { makeAutoObservable, observable, runInAction } from 'mobx';

import type { WindowState } from '../settings';

import WindowStore from './WindowStore';

export default class OpenWindowsStore {
  private readonly windowStores = new Map<BrowserWindow, WindowStore>();

  private lastActiveWindow: BrowserWindow | undefined;

  lastActiveWindowState: WindowState | undefined;

  constructor() {
    makeAutoObservable<OpenWindowsStore, 'lastActiveWindow' | 'windowStores'>(
      this,
      {
        lastActiveWindow: observable.ref,
        windowStores: false,
      },
    );
  }

  createWindowStore(browserWindow: BrowserWindow): WindowStore {
    if (this.windowStores.has(browserWindow)) {
      throw new Error('WindowStore already exists for BrowserWindow');
    }
    const updateWindowState = () => {
      const windowStore = this.getWindowStore(browserWindow);
      const { width, height } = browserWindow.getNormalBounds();
      windowStore.setWindowState({
        width,
        height,
        // Fullscreen is transient and should not replace the underlying
        // maximized state that will be restored when fullscreen is left.
        maximized: browserWindow.isFullScreen()
          ? windowStore.maximized
          : browserWindow.isMaximized(),
      });
      if (this.lastActiveWindow === browserWindow) {
        runInAction(() => {
          this.lastActiveWindowState = windowStore.windowState;
        });
      }
    };
    const activate = () => {
      this.setLastActiveWindow(browserWindow);
      updateWindowState();
    };
    const { width, height } = browserWindow.getNormalBounds();
    const windowStore = new WindowStore({
      width,
      height,
      maximized: !browserWindow.isFullScreen() && browserWindow.isMaximized(),
    });
    this.windowStores.set(browserWindow, windowStore);
    browserWindow.on('focus', activate);
    browserWindow.on('resize', updateWindowState);
    browserWindow.on('maximize', updateWindowState);
    browserWindow.on('unmaximize', updateWindowState);
    browserWindow.once('closed', () => {
      browserWindow.off('focus', activate);
      browserWindow.off('resize', updateWindowState);
      browserWindow.off('maximize', updateWindowState);
      browserWindow.off('unmaximize', updateWindowState);
      if (this.lastActiveWindow === browserWindow) {
        this.setLastActiveWindow(undefined);
      }
      if (this.windowStores.get(browserWindow) === windowStore) {
        this.windowStores.delete(browserWindow);
        windowStore.dispose();
      }
    });
    return windowStore;
  }

  getWindowStore(browserWindow: BrowserWindow): WindowStore {
    const windowStore = this.windowStores.get(browserWindow);
    if (windowStore === undefined) {
      throw new Error('No WindowStore found for BrowserWindow');
    }
    return windowStore;
  }

  get lastActiveWindowStore(): WindowStore | undefined {
    return this.lastActiveWindow === undefined
      ? undefined
      : this.windowStores.get(this.lastActiveWindow);
  }

  private setLastActiveWindow(browserWindow: BrowserWindow | undefined): void {
    this.lastActiveWindow = browserWindow;
  }

  findWindowByFilePath(filePath: string): BrowserWindow | undefined {
    for (const [browserWindow, windowStore] of this.windowStores) {
      if (windowStore.filePath === filePath) {
        return browserWindow;
      }
    }
    return undefined;
  }

  findWindowsWithUnsavedChanges(): BrowserWindow[] {
    const unsavedWindows: BrowserWindow[] = [];
    for (const [browserWindow, windowStore] of this.windowStores) {
      if (windowStore.unsavedChanges) {
        unsavedWindows.push(browserWindow);
      }
    }
    return unsavedWindows;
  }
}

export const openWindowsStore = new OpenWindowsStore();
