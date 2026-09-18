/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import type { ChildProcess } from 'node:child_process';
import { EOL } from 'node:os';
import { createInterface } from 'node:readline';
import type Stream from 'node:stream';

import { destination } from '.';

export function pipeToCallback(
  input: Stream.Readable,
  child: ChildProcess,
  callback: (line: string) => void,
): void {
  const readline = createInterface({
    input,
    crlfDelay: Infinity,
  });
  readline.on('line', callback);
  for (const event of ['error', 'exit'] as const) {
    child.on(event, () => readline.close());
  }
}

export default function pipeToLogger(
  input: Stream.Readable,
  child: ChildProcess,
): void {
  pipeToCallback(input, child, (line) => destination.write(`${line}${EOL}`));
}
