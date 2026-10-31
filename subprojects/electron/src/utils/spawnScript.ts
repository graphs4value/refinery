/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import child_process, {
  type ChildProcess,
  type SpawnOptionsWithoutStdio,
  type SpawnOptionsWithStdioTuple,
  type StdioNull,
  type StdioPipe,
} from 'child_process';

import { logLevel } from '../logger';
import pipeToLogger from '../logger/pipeToLogger';

import { isWindows } from './platform';

export default function spawnScript(
  command: string,
  args: string[],
  options: SpawnOptionsWithoutStdio,
): ChildProcess {
  const commonOptions: SpawnOptionsWithStdioTuple<
    StdioNull,
    StdioNull,
    StdioPipe
  > = {
    ...options,
    env: {
      ...(options.env ?? {}),
      REFINERY_LOG_DESTINATION: 'stderr',
      REFINERY_LOG_FORMAT: 'json',
      REFINERY_LOG_LEVEL: logLevel,
    },
    stdio: ['ignore', 'inherit', 'pipe'],
    detached: false,
  };

  let child;
  if (isWindows) {
    child = child_process.spawn(
      'cmd.exe',
      ['/q', '/c', `${command}.bat`, ...args],
      {
        ...commonOptions,
        windowsHide: true,
      },
    );
  } else {
    child = child_process.spawn(command, args, commonOptions);
  }

  pipeToLogger(child.stderr, child);

  return child;
}
