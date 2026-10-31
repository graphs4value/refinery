/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { EventEmitter } from 'node:events';

import type { BrowserWindow } from 'electron';
import { isObservableProp, reaction } from 'mobx';
import { beforeEach, describe, expect, test, vi } from 'vitest';

import OpenWindowsStore from './OpenWindowsStore';

function createBrowserWindow(
  width = 1024,
  height = 768,
  maximized = false,
): BrowserWindow {
  return Object.assign(new EventEmitter(), {
    getNormalBounds: vi.fn(() => ({ x: 0, y: 0, width, height })),
    isFullScreen: vi.fn(() => false),
    isMaximized: vi.fn(() => maximized),
  }) as unknown as BrowserWindow;
}

describe('OpenWindowsStore', () => {
  let openWindowsStore: OpenWindowsStore;

  beforeEach(() => {
    openWindowsStore = new OpenWindowsStore();
  });

  test('creates a store for a BrowserWindow', () => {
    const browserWindow = createBrowserWindow();
    const windowStore = openWindowsStore.createWindowStore(browserWindow);

    expect(openWindowsStore.getWindowStore(browserWindow)).toBe(windowStore);
    expect(isObservableProp(openWindowsStore, 'lastActiveWindow')).toBe(true);
    expect(isObservableProp(openWindowsStore, 'lastActiveWindowState')).toBe(
      true,
    );

    browserWindow.emit('closed');
  });

  test('rejects duplicate stores', () => {
    const browserWindow = createBrowserWindow();
    openWindowsStore.createWindowStore(browserWindow);

    expect(() => openWindowsStore.createWindowStore(browserWindow)).toThrow(
      'WindowStore already exists for BrowserWindow',
    );

    browserWindow.emit('closed');
  });

  test('rejects unregistered windows', () => {
    const browserWindow = createBrowserWindow();

    expect(() => openWindowsStore.getWindowStore(browserWindow)).toThrow(
      'No WindowStore found for BrowserWindow',
    );
  });

  test('finds the window associated with a file path', () => {
    const browserWindow = createBrowserWindow();
    const windowStore = openWindowsStore.createWindowStore(browserWindow);
    const filePath = '/example/model.problem';
    windowStore.setFilePath(filePath);

    expect(openWindowsStore.findWindowByFilePath(filePath)).toBe(browserWindow);
    expect(
      openWindowsStore.findWindowByFilePath('/example/other.problem'),
    ).toBeUndefined();

    browserWindow.emit('closed');
    expect(openWindowsStore.findWindowByFilePath(filePath)).toBeUndefined();
  });

  test('finds windows with unsaved changes', () => {
    const firstWindow = createBrowserWindow();
    const secondWindow = createBrowserWindow();
    openWindowsStore.createWindowStore(firstWindow);
    const secondWindowStore = openWindowsStore.createWindowStore(secondWindow);
    secondWindowStore.setUnsavedChanges(true);

    expect(openWindowsStore.findWindowsWithUnsavedChanges()).toEqual([
      secondWindow,
    ]);

    secondWindowStore.setUnsavedChanges(false);
    expect(openWindowsStore.findWindowsWithUnsavedChanges()).toEqual([]);

    firstWindow.emit('closed');
    secondWindow.emit('closed');
  });

  test('removes and disposes the store when the window is closed', () => {
    const browserWindow = createBrowserWindow();
    const windowStore = openWindowsStore.createWindowStore(browserWindow);
    const disposer = vi.fn();
    windowStore.addReactionDisposer(disposer);

    browserWindow.emit('closed');

    expect(disposer).toHaveBeenCalledOnce();
    expect(() => openWindowsStore.getWindowStore(browserWindow)).toThrow(
      'No WindowStore found for BrowserWindow',
    );

    windowStore.dispose();
    expect(disposer).toHaveBeenCalledOnce();
  });

  test('tracks and reports the last active window state', () => {
    const firstWindow = createBrowserWindow(800, 600);
    const secondWindow = createBrowserWindow(1200, 900, true);
    const stateChanged = vi.fn();
    const disposeReaction = reaction(
      () => openWindowsStore.lastActiveWindowState,
      (windowState) => {
        stateChanged(windowState);
      },
    );
    openWindowsStore.createWindowStore(firstWindow);
    openWindowsStore.createWindowStore(secondWindow);

    firstWindow.emit('focus');
    expect(openWindowsStore.lastActiveWindowState).toEqual({
      width: 800,
      height: 600,
      maximized: false,
    });
    secondWindow.emit('focus');
    expect(openWindowsStore.lastActiveWindowState).toEqual({
      width: 1200,
      height: 900,
      maximized: true,
    });

    vi.spyOn(secondWindow, 'getNormalBounds').mockReturnValue({
      x: 0,
      y: 0,
      width: 1400,
      height: 1000,
    });
    vi.spyOn(secondWindow, 'isMaximized').mockReturnValue(false);
    secondWindow.emit('resize');
    expect(stateChanged).toHaveBeenLastCalledWith({
      width: 1400,
      height: 1000,
      maximized: false,
    });

    firstWindow.emit('resize');
    expect(stateChanged).toHaveBeenCalledTimes(3);

    secondWindow.emit('closed');
    expect(openWindowsStore.lastActiveWindowState).toEqual({
      width: 1400,
      height: 1000,
      maximized: false,
    });
    firstWindow.emit('focus');
    expect(openWindowsStore.lastActiveWindowState).toEqual({
      width: 800,
      height: 600,
      maximized: false,
    });
    firstWindow.emit('closed');
    expect(openWindowsStore.lastActiveWindowState).toEqual({
      width: 800,
      height: 600,
      maximized: false,
    });
    disposeReaction();
  });

  test('does not persist fullscreen as an unmaximized state', () => {
    const browserWindow = createBrowserWindow(1200, 900, true);
    const stateChanged = vi.fn();
    const disposeReaction = reaction(
      () => openWindowsStore.lastActiveWindowState,
      (windowState) => {
        stateChanged(windowState);
      },
    );
    openWindowsStore.createWindowStore(browserWindow);
    browserWindow.emit('focus');

    vi.spyOn(browserWindow, 'isFullScreen').mockReturnValue(true);
    vi.spyOn(browserWindow, 'isMaximized').mockReturnValue(false);
    browserWindow.emit('resize');

    expect(stateChanged).toHaveBeenLastCalledWith({
      width: 1200,
      height: 900,
      maximized: true,
    });
    browserWindow.emit('closed');
    disposeReaction();
  });
});
