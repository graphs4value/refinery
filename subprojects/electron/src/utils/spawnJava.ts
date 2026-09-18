/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import child_process, {
  ChildProcess,
  type SpawnOptionsWithoutStdio,
} from 'node:child_process';
import path from 'node:path';

import { logLevel } from '../logger';
import pipeToLogger from '../logger/pipeToLogger';

import { isWindows } from './platform';

export function formatMaxMemory(maxMemoryBytes: number): string {
  return `-Xmx${Math.max(1, Math.floor(maxMemoryBytes / (1024 * 1024)))}m`;
}

export default function spawnJava(
  packageName: string,
  mainClass: string,
  args: string[],
  {
    interactive = false,
    maxMemoryBytes,
    classpathJars = [],
    ...options
  }: SpawnOptionsWithoutStdio & {
    interactive?: boolean;
    maxMemoryBytes?: number;
    classpathJars?: readonly string[];
  },
): ChildProcess {
  // Sometimes we spawn the stock `electron` binary instead of our own
  // packaged executable (e.g. to run the packaged app under Playwright,
  // which only reliably detects readiness when Electron is launched this
  // way). In that case `process.resourcesPath` points at the stock
  // binary's own resources, not our packaged ones, so this lets
  // the caller override it explicitly.
  const resourcesPath =
    process.env['REFINERY_ELECTRON_RESOURCES_PATH'] ?? process.resourcesPath;
  const libDir = path.resolve(resourcesPath, 'lib');
  const javaDir = path.resolve(resourcesPath, 'jre');
  const javaBinDir = path.join(javaDir, 'bin');
  const javaBinary = path.join(javaBinDir, isWindows ? 'java.exe' : 'java');
  const pathingJar = path.join(libDir, `${packageName}-all.jar`);
  const nativesDir = path.join(libDir, 'native');

  const pathEnv = process.env['PATH'];
  // For Windows: make sure the native libraries are present of `PATH`.
  const extraPaths = [nativesDir, javaDir].join(path.delimiter);
  const newPathEnv =
    pathEnv === undefined || pathEnv === ''
      ? extraPaths
      : `${extraPaths}${path.delimiter}${pathEnv}`;

  const newEnv: NodeJS.ProcessEnv = {
    ...(options.env ?? {}),
    REFINERY_LOG_DESTINATION: 'stderr',
    REFINERY_LOG_FORMAT: 'json',
    REFINERY_LOG_LEVEL: logLevel,
    PATH: newPathEnv,
    JAVA_HOME: javaDir,
    CLASSPATH: [pathingJar, ...classpathJars].join(path.delimiter),
    // For Linux: dynamic linking for native libraries.
    LD_LIBRARY_PATH: nativesDir,
    // For macOS: dynamic linking for native libraries.
    DYLD_LIBRARY_PATH: nativesDir,
  };
  delete newEnv['ELECTRON_RUN_AS_NODE'];
  // Do not inherit Java options from the environment to ensure portability.
  delete newEnv['_JAVA_OPTIONS'];
  delete newEnv['JDK_JAVA_OPTIONS'];
  delete newEnv['JAVA_TOOL_OPTIONS'];

  const child = child_process.spawn(
    javaBinary,
    [
      '--enable-native-access=ALL-UNNAMED',
      '--sun-misc-unsafe-memory-access=allow',
      ...(maxMemoryBytes === undefined
        ? []
        : [formatMaxMemory(maxMemoryBytes)]),
      `-Djava.library.path=${nativesDir}`,
      mainClass,
      ...args,
    ],
    {
      ...options,
      env: newEnv,
      stdio: [interactive ? 'inherit' : 'ignore', 'inherit', 'pipe'],
      detached: false,
      ...(isWindows ? { windowsHide: true } : {}),
    },
  );

  pipeToLogger(child.stderr, child);

  return child;
}
