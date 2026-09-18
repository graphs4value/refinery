/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { execFile } from 'node:child_process';
import { lstat, readlink } from 'node:fs/promises';
import path from 'node:path';
import { promisify } from 'node:util';

import type {
  CLISymlinkResult,
  CLISymlinkState,
  CLISymlinkStatus,
} from '@tools.refinery/frontend/RefineryContextBridge';
import { app, BrowserWindow, ipcMain } from 'electron';

import getLogger from '../logger/getLogger';
import settings from '../settings';
import { isMac } from '../utils/platform';

const logger = getLogger('gui.cliSymlink');

const execFileAsync = promisify(execFile);

export const CLI_SYMLINK_PATH = '/usr/local/bin/refinery';

let cliSymlinkPromptClaimed = false;

let cliSymlinkActionInFlight = false;

type CLISymlinkTargetStatus =
  'missing' | 'occupied' | 'wrongTarget' | 'correct';

function getCLIWrapperPath(): string {
  const resourcesPath =
    process.env['REFINERY_ELECTRON_RESOURCES_PATH'] ?? process.resourcesPath;
  return path.join(resourcesPath, 'bin', 'refinery');
}

/**
 * Checks whether a given app install location should support adding
 * a symlink to `/usr/local/bin`.
 *
 * We exclude translocated paths (quarantined DMG not yet moved in finder,
 * with the app launched through LaunchServices), because they are
 * not stable across launches.
 *
 * This prompts the user to create a symlink from a DMG without translocation,
 * but allows storing the app in install locations other than `/Applications`.
 *
 * @param wrapperPath The path to check.
 * @returns `true` if this install location supports symlinks.
 */
function isSupportedInstallPath(wrapperPath: string): boolean {
  const resolvedPath = path.resolve(wrapperPath);
  return !resolvedPath.split(path.sep).includes('AppTranslocation');
}

function isCLISymlinkSupported(): boolean {
  return isMac && !process.mas && isSupportedInstallPath(getCLIWrapperPath());
}

function shellQuote(value: string): string {
  return `'${value.replaceAll("'", "'\\''")}'`;
}

function appleScriptQuote(value: string): string {
  return `"${value.replaceAll('\\', '\\\\').replaceAll('"', '\\"')}"`;
}

async function runAsAdministrator(command: string): Promise<void> {
  const script = `do shell script ${appleScriptQuote(
    command,
  )} with administrator privileges`;
  await execFileAsync('/usr/bin/osascript', ['-e', script]);
}

async function getLinkStatus(): Promise<CLISymlinkStatus> {
  const wrapperPath = getCLIWrapperPath();
  try {
    const wrapperStat = await lstat(wrapperPath);
    if (!wrapperStat.isFile()) {
      return 'unsupported';
    }
  } catch (error) {
    if (
      error === null ||
      typeof error !== 'object' ||
      !('code' in error) ||
      error.code !== 'ENOENT'
    ) {
      throw error;
    }
    return 'unsupported';
  }

  if (settings.createCLISymlink === undefined) {
    return 'notConfigured';
  }
  if (!settings.createCLISymlink) {
    return 'disabled';
  }

  switch (await getCLISymlinkTargetStatus(wrapperPath)) {
    case 'missing':
      return 'missing';
    case 'correct':
      return 'correct';
    case 'occupied':
    case 'wrongTarget':
      return 'incorrect';
  }
}

async function getCLISymlinkTargetStatus(
  wrapperPath: string,
): Promise<CLISymlinkTargetStatus> {
  try {
    const linkStat = await lstat(CLI_SYMLINK_PATH);
    if (!linkStat.isSymbolicLink()) {
      return 'occupied';
    }
    const target = await readlink(CLI_SYMLINK_PATH);
    const resolvedTarget = path.resolve(path.dirname(CLI_SYMLINK_PATH), target);
    return resolvedTarget === path.resolve(wrapperPath)
      ? 'correct'
      : 'wrongTarget';
  } catch (error) {
    if (
      error !== null &&
      typeof error === 'object' &&
      'code' in error &&
      error.code === 'ENOENT'
    ) {
      return 'missing';
    }
    throw error;
  }
}

async function getCLISymlinkStatusValue(): Promise<CLISymlinkStatus> {
  if (!isCLISymlinkSupported()) {
    return 'unsupported';
  }
  return getLinkStatus();
}

async function getCLISymlinkStatus(): Promise<CLISymlinkState> {
  return {
    status: await getCLISymlinkStatusValue(),
    actionInFlight: cliSymlinkActionInFlight,
  };
}

async function broadcastCLISymlinkStatus(): Promise<void> {
  try {
    const state = await getCLISymlinkStatus();
    for (const window of BrowserWindow.getAllWindows()) {
      window.webContents.send('refinery:cliSymlinkStatusChanged', state);
    }
  } catch (error) {
    logger.error({ err: error }, 'Failed to broadcast CLI symlink status');
  }
}

export function claimCLISymlinkPrompt(): boolean {
  if (!isCLISymlinkSupported()) {
    return false;
  }
  // Avoid an unsolicited first-run prompt outside Applications, while still
  // offering repairs after the user has explicitly enabled the launcher.
  if (
    settings.createCLISymlink === undefined &&
    !app.isInApplicationsFolder()
  ) {
    return false;
  }
  if (cliSymlinkPromptClaimed) {
    return false;
  }
  cliSymlinkPromptClaimed = true;
  return true;
}

async function createCLISymlink(): Promise<'success' | 'occupied'> {
  const wrapperPath = getCLIWrapperPath();
  const targetStatus = await getCLISymlinkTargetStatus(wrapperPath);
  if (targetStatus === 'correct') {
    return 'success';
  }
  if (targetStatus === 'occupied') {
    return 'occupied';
  }
  const target = shellQuote(CLI_SYMLINK_PATH);
  const wrapper = shellQuote(wrapperPath);
  const directory = shellQuote(path.dirname(CLI_SYMLINK_PATH));
  await runAsAdministrator(
    `mkdir -p ${directory} && ` +
      `if [ -L ${target} ]; then rm -f ${target}; fi && ` +
      `ln -s ${wrapper} ${target}`,
  );
  return 'success';
}

async function removeCLISymlink(): Promise<void> {
  const target = shellQuote(CLI_SYMLINK_PATH);
  let targetStatus: CLISymlinkTargetStatus;
  try {
    targetStatus = await getCLISymlinkTargetStatus(getCLIWrapperPath());
  } catch {
    // Let the privileged command handle paths that cannot be inspected here.
    targetStatus = 'wrongTarget';
  }
  if (targetStatus === 'missing' || targetStatus === 'occupied') {
    return;
  }
  await runAsAdministrator(`if [ -L ${target} ]; then rm -f ${target}; fi`);
}

async function performSetCLISymlink(
  enabled: boolean,
): Promise<CLISymlinkResult> {
  try {
    if (enabled) {
      const createResult = await createCLISymlink();
      if (createResult === 'occupied') {
        return { error: true, reason: 'occupied' };
      }
    } else {
      await removeCLISymlink();
    }
    settings.setCreateCLISymlink(enabled);
    return true;
  } catch (error) {
    logger.error({ err: error }, 'Failed to update CLI symlink');
    return { error: true, reason: 'failed' };
  }
}

async function runCLISymlinkAction(
  action: () => Promise<CLISymlinkResult>,
): Promise<CLISymlinkResult> {
  if (cliSymlinkActionInFlight) {
    return { error: true, reason: 'busy' };
  }
  cliSymlinkActionInFlight = true;
  try {
    await broadcastCLISymlinkStatus();
    return await action();
  } finally {
    cliSymlinkActionInFlight = false;
    await broadcastCLISymlinkStatus();
  }
}

export function setCLISymlink(enabled: boolean): Promise<CLISymlinkResult> {
  if (!isCLISymlinkSupported()) {
    return Promise.resolve({ error: true, reason: 'failed' });
  }
  return runCLISymlinkAction(() => performSetCLISymlink(enabled));
}

export function setCLISymlinkPreference(
  enabled: boolean,
): Promise<CLISymlinkResult> {
  if (!isCLISymlinkSupported()) {
    return Promise.resolve({ error: true, reason: 'failed' });
  }
  return runCLISymlinkAction(() => {
    settings.setCreateCLISymlink(enabled);
    return Promise.resolve(true);
  });
}

export default function attachCLISymlinkHandlers(): void {
  if (isMac) {
    app.on('browser-window-focus', () => {
      broadcastCLISymlinkStatus().catch((error) => {
        logger.error({ err: error }, 'Failed to broadcast CLI symlink status');
      });
    });
  }
  ipcMain.handle('refinery:getCLISymlinkStatus', async () => {
    try {
      return await getCLISymlinkStatus();
    } catch (error) {
      logger.error({ err: error }, 'Failed to inspect CLI symlink');
      return {
        status: 'unsupported',
        actionInFlight: cliSymlinkActionInFlight,
      };
    }
  });
  ipcMain.handle('refinery:claimCLISymlinkPrompt', () =>
    claimCLISymlinkPrompt(),
  );
  ipcMain.handle('refinery:setCLISymlink', async (_event, enabled: unknown) => {
    if (typeof enabled !== 'boolean') {
      logger.error({ enabled }, 'Invalid CLI symlink setting');
      return { error: true, reason: 'failed' };
    }
    return setCLISymlink(enabled);
  });
  ipcMain.handle(
    'refinery:setCLISymlinkPreference',
    (_event, enabled: unknown) => {
      if (typeof enabled !== 'boolean') {
        logger.error({ enabled }, 'Invalid CLI symlink preference');
        return { error: true, reason: 'failed' };
      }
      return setCLISymlinkPreference(enabled);
    },
  );
}
