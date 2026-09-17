/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import child_process from 'node:child_process';
import { once } from 'node:events';

import getLogger from '../logger/getLogger';

const log = getLogger('cli.launchGUI');

/**
 * Launches the GUI and waits for it to spawn successfully, since a failure is
 * otherwise reported asynchronously, after we'd already have exited. For an
 * AppImage, also wait for the GUI to exit so its temporary mount stays alive.
 */
export default async function launchGUI(args: string[]): Promise<boolean> {
  const newEnv = { ...process.env };
  delete newEnv['ELECTRON_RUN_AS_NODE'];
  // An AppImage runs from a temporary mount. Keep its launcher process alive
  // until the GUI exits, otherwise the AppImage runtime can unmount APPDIR
  // immediately after this CLI process returns.
  const isAppImage = process.env['REFINERY_APPIMAGE'] === '1';
  // Chromium's SUID sandbox needs `chrome-sandbox` to be owned by root with
  // mode 4755 (or user namespaces to be available and permitted), which is
  // only set up by the installers for packaged builds (see
  // `after-install.tpl`), not by an unpacked `electron-builder` output.
  const electronArgs =
    process.env['REFINERY_NO_SANDBOX'] === '1' ? ['--no-sandbox'] : [];
  const child = child_process.spawn(process.argv0, [...electronArgs, ...args], {
    env: newEnv,
    stdio: ['ignore', 'ignore', 'ignore'],
    detached: !isAppImage,
  });
  try {
    await once(child, 'spawn');
    if (isAppImage) {
      await once(child, 'exit');
    }
    return true;
  } catch (error) {
    log.error({ err: error }, 'Failed to launch Electron');
    return false;
  } finally {
    child.unref();
  }
}
