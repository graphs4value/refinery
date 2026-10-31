/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { Command } from './commands';
import expandArgs from './expandArgs';

const COMMANDS: Command[] = ['render', 'r'];

const FORMAT_OPTIONS = ['-format', '-f'];

const FORMATS = ['svg', 'pdf', 'png'];

const OUTPUT_OPTIONS = ['-output', '-o'];

const OUTPUTS = /\.(?:svg|pdf|png)$/i;

type State = 'command' | 'default' | 'format' | 'output';

/**
 * Determines whether a Refinery CLI invocation requires the headless worker.
 *
 * + Invocations of the `render` command always require a headless worker.
 * + Output formats in `FORMATS` always require a headless worker.
 * + Output file paths matching `OUTPUTS` always require a headless worker,
 *   unless a different output format has been explicitly set.
 *
 * @param args The argument list.
 * @returns `true` if the CLI invocation requires the headless worker.
 */
export default async function isHeadlessNeeded(
  args: string[],
): Promise<boolean> {
  let state: State = 'command';
  let ouputNeedsHeadless = false;
  for await (const arg of expandArgs(args)) {
    switch (state) {
      case 'command':
        if ((COMMANDS as string[]).includes(arg)) {
          return true;
        } else if (arg.startsWith('-')) {
          // The Refinery CLI has no top-level options,
          // so this situation means no subcommand is specified.
          return false;
        }
        state = 'default';
        break;
      case 'default':
        if (FORMAT_OPTIONS.includes(arg)) {
          state = 'format';
        } else if (OUTPUT_OPTIONS.includes(arg)) {
          state = 'output';
        } else if (arg === '--') {
          return ouputNeedsHeadless;
        }
        break;
      case 'format':
        return FORMATS.includes(arg.toLowerCase());
      case 'output':
        if (OUTPUTS.test(arg)) {
          ouputNeedsHeadless = true;
        }
        state = 'default';
        break;
      default:
        throw new Error(`Unknown state: ${String(state)}`);
    }
  }
  return ouputNeedsHeadless;
}
