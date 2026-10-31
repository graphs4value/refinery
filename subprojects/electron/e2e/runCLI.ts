/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { spawn } from 'node:child_process';

import { getArchSuffix, resolvePackagedPath } from './packagedPaths';

export interface CLIResult {
  readonly exitCode: number | null;
  readonly stdout: string;
  readonly stderr: string;
}

/**
 * Path to the `electron-builder` unpacked CLI shim, relative to the subproject root.
 */
function getRelativeCLIPath(): string {
  const archSuffix = getArchSuffix();
  switch (process.platform) {
    case 'linux':
      return `build/dist/linux${archSuffix}-unpacked/bin/refinery`;
    case 'darwin':
      return `build/dist/mac${archSuffix}/Refinery.app/Contents/Resources/bin/refinery`;
    case 'win32':
      return `build/dist/win${archSuffix}-unpacked/bin/refinery.exe`;
    default:
      throw new Error(`Unsupported platform: ${process.platform}`);
  }
}

export function getPackagedCLIPath(): string {
  return resolvePackagedPath(getRelativeCLIPath(), 'Packaged CLI');
}

/**
 * Spawns the packaged CLI shim and collects its output.
 *
 * `signal` should be the running test's abort signal (aborted on timeout or
 * cancellation), so we don't leave the CLI (and the JVM/Electron it spawns)
 * running once the test that started it has given up on it.
 */
export default function runCLI(
  cliPath: string,
  args: string[],
  signal?: AbortSignal,
): Promise<CLIResult> {
  return new Promise((resolve, reject) => {
    const child = spawn(cliPath, args, { stdio: ['ignore', 'pipe', 'pipe'] });
    let stdout = '';
    let stderr = '';
    child.stdout.on('data', (chunk: Buffer) => {
      stdout += chunk.toString('utf-8');
    });
    child.stderr.on('data', (chunk: Buffer) => {
      stderr += chunk.toString('utf-8');
    });

    const onAbort = () => child.kill();
    signal?.addEventListener('abort', onAbort, { once: true });

    child.once('error', (error) => {
      signal?.removeEventListener('abort', onAbort);
      reject(error);
    });
    child.once('exit', (exitCode) => {
      signal?.removeEventListener('abort', onAbort);
      resolve({ exitCode, stdout, stderr });
    });
  });
}
