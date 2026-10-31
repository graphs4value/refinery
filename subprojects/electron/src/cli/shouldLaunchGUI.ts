/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import commands from './commands';

export default function shouldLaunchGUI(args: string[]): boolean {
  const firstArg = args[0];
  return (
    !firstArg ||
    firstArg === '--' ||
    (!(commands as readonly string[]).includes(firstArg) &&
      !args.some((arg) => arg.startsWith('-') || arg.startsWith('@')))
  );
}
