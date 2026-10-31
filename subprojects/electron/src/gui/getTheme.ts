/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { ThemeSource } from '@tools.refinery/frontend/RefineryContextBridge';
import { color } from 'd3-color';
import {
  BrowserWindow,
  ipcMain,
  nativeTheme,
  type TitleBarOverlay,
} from 'electron';
import { reaction } from 'mobx';
import z from 'zod';

import getLogger from '../logger/getLogger';
import settings from '../settings';
import { isMac, isWindows } from '../utils/platform';

import { openWindowsStore } from './OpenWindowsStore';

const logger = getLogger('gui.getTheme');

interface Theme {
  backgroundColor: string;
  accentColor: string;
  titleBarOverlay: TitleBarOverlay;
}

const ModalDialogCount = z.int().nonnegative();

const UnsavedChanges = z.boolean();

function blendModalOverlay(inputColor: string, depth: number, opacity = 1) {
  const colorCoords = color(inputColor)?.rgb();
  if (!colorCoords) {
    return inputColor;
  }
  if (depth === 0) {
    return colorCoords.copy({ opacity }).formatRgb();
  }
  const a = Math.pow(0.5, depth);
  return colorCoords
    .copy({
      r: Math.floor(a * colorCoords.r),
      g: Math.floor(a * colorCoords.g),
      b: Math.floor(a * colorCoords.b),
      opacity,
    })
    .formatRgb();
}

export default function getTheme(
  shouldUseDarkColors: boolean,
  browserWindow?: BrowserWindow,
): Theme {
  let depth = 0;
  if (browserWindow) {
    depth = openWindowsStore.getWindowStore(browserWindow).modalDialogCount;
  }
  if (shouldUseDarkColors) {
    const backgroundColor = '#21252b';
    return {
      backgroundColor,
      accentColor: '#56b6c2',
      titleBarOverlay: {
        // We determine what the background color behind the WCO would be
        // so we set the correct RGB coordinates even on platforms which don't
        // support transparency or perform hover contrast calculations
        // based on the RGB coordinates. We set the alpha coordinate to 0
        // to correctly composite the WCO over animated background color.
        color: blendModalOverlay(backgroundColor, depth, 0),
        symbolColor: blendModalOverlay('#ebebff', depth),
      },
    };
  }
  const backgroundColor = '#f5f5f5';
  return {
    backgroundColor,
    accentColor: '#038a99',
    titleBarOverlay: {
      color: blendModalOverlay(backgroundColor, depth, 0),
      symbolColor: blendModalOverlay('#19202b', depth),
    },
  };
}

function updateWindow(window: BrowserWindow): void {
  const { backgroundColor, accentColor, titleBarOverlay } = getTheme(
    nativeTheme.shouldUseDarkColors,
    window,
  );
  window.setBackgroundColor(backgroundColor);
  if (isWindows) {
    window.setAccentColor(accentColor);
  }
  if (!isMac) {
    window.setTitleBarOverlay(titleBarOverlay);
  }
}

export function attachWindowThemeHandler(window: BrowserWindow): void {
  const windowStore = openWindowsStore.getWindowStore(window);
  windowStore.addReactionDisposer(
    reaction(
      () => windowStore.modalDialogCount,
      () => updateWindow(window),
      { fireImmediately: false },
    ),
  );
}

export function attachNativeThemeHandler(): void {
  reaction(
    () => settings.theme,
    (theme) => (nativeTheme.themeSource = theme),
    { fireImmediately: true },
  );

  nativeTheme.on('updated', () => {
    const { themeSource } = nativeTheme;
    for (const window of BrowserWindow.getAllWindows()) {
      window.webContents.send('refinery:themeSourceChanged', themeSource);
      updateWindow(window);
    }
  });

  ipcMain.handle(
    'refinery:getThemeSource',
    (): ThemeSource => nativeTheme.themeSource,
  );

  ipcMain.on('refinery:setThemeSource', (_event, rawThemeSource: unknown) => {
    const themeSource = ThemeSource.safeParse(rawThemeSource);
    if (themeSource.success) {
      settings.setTheme(themeSource.data);
    } else {
      logger.error({ err: themeSource.error }, 'Failed to parse ThemeSource');
    }
  });

  ipcMain.on('refinery:setModalDialogCount', (event, rawCount: unknown) => {
    const browserWindow = BrowserWindow.fromWebContents(event.sender);
    if (browserWindow === null) {
      logger.error(
        { processId: event.processId },
        'Invalid setModalDialogCount from WebContents without a BrowserWindow',
      );
      return;
    }
    const count = ModalDialogCount.safeParse(rawCount);
    if (count.success) {
      openWindowsStore
        .getWindowStore(browserWindow)
        .setModalDialogCount(count.data);
    } else {
      logger.error({ err: count.error }, 'Failed to parse ModalDialogCount');
    }
  });

  ipcMain.on(
    'refinery:setUnsavedChanges',
    (event, rawUnsavedChanges: unknown) => {
      const browserWindow = BrowserWindow.fromWebContents(event.sender);
      if (browserWindow === null) {
        logger.error(
          { processId: event.processId },
          'Invalid setUnsavedChanges from WebContents without a BrowserWindow',
        );
        return;
      }
      const unsavedChanges = UnsavedChanges.safeParse(rawUnsavedChanges);
      if (unsavedChanges.success) {
        openWindowsStore
          .getWindowStore(browserWindow)
          .setUnsavedChanges(unsavedChanges.data);
      } else {
        logger.error(
          { err: unsavedChanges.error },
          'Failed to parse UnsavedChanges',
        );
      }
    },
  );
}
