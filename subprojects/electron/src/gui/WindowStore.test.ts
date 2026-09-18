/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { isObservableProp } from 'mobx';
import { describe, expect, test, vi } from 'vitest';

import WindowStore from './WindowStore';

describe('WindowStore', () => {
  test('tracks observable per-window state', () => {
    const windowStore = new WindowStore({
      width: 1024,
      height: 768,
      maximized: false,
    });

    expect(isObservableProp(windowStore, 'modalDialogCount')).toBe(true);
    expect(isObservableProp(windowStore, 'unsavedChanges')).toBe(true);
    expect(isObservableProp(windowStore, 'filePath')).toBe(true);
    expect(isObservableProp(windowStore, 'hash')).toBe(true);
    expect(isObservableProp(windowStore, 'width')).toBe(true);
    expect(isObservableProp(windowStore, 'height')).toBe(true);
    expect(isObservableProp(windowStore, 'maximized')).toBe(true);
    expect(windowStore.filePath).toBeUndefined();
    expect(windowStore.hash).toBeUndefined();
    expect(windowStore.unsavedChanges).toBe(false);
    expect(windowStore.windowState).toEqual({
      width: 1024,
      height: 768,
      maximized: false,
    });

    windowStore.setModalDialogCount(2);
    expect(windowStore.modalDialogCount).toBe(2);
    windowStore.setUnsavedChanges(true);
    expect(windowStore.unsavedChanges).toBe(true);
    windowStore.setFilePath('/example/model.problem');
    expect(windowStore.filePath).toBe('/example/model.problem');
    windowStore.setHash('#/2/shared-model');
    expect(windowStore.filePath).toBeUndefined();
    expect(windowStore.hash).toBe('#/2/shared-model');
    windowStore.setFilePath('/example/saved.problem');
    expect(windowStore.filePath).toBe('/example/saved.problem');
    expect(windowStore.hash).toBeUndefined();
    windowStore.setWindowState({
      width: 1200,
      height: 900,
      maximized: true,
    });
    expect(windowStore.windowState).toEqual({
      width: 1200,
      height: 900,
      maximized: true,
    });
  });

  test('disposes reactions once', () => {
    const windowStore = new WindowStore({
      width: 1024,
      height: 768,
      maximized: false,
    });
    const disposer = vi.fn();
    windowStore.addReactionDisposer(disposer);

    windowStore.dispose();
    windowStore.dispose();

    expect(disposer).toHaveBeenCalledOnce();
  });
});
