/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import path from 'node:path';
import { fileURLToPath } from 'node:url';

import { parseShareURI } from '@tools.refinery/frontend/persistence/shareURI';

import type { OpenRequest } from './OpenRequestHandler';

export function resolveOpenArgument(
  argument: string,
  workingDirectory: string,
): OpenRequest | undefined {
  if (argument === '') {
    return undefined;
  }
  if (/^file:/i.test(argument)) {
    try {
      return { filePath: path.resolve(fileURLToPath(argument)) };
    } catch {
      return undefined;
    }
  }
  if (/^[a-z][a-z\d+.-]*:\/\//i.test(argument)) {
    const hash = parseShareURI(argument);
    return hash === undefined ? undefined : { hash };
  }
  return { filePath: path.resolve(workingDirectory, argument) };
}

export default function resolveOpenArguments(
  args: string[],
  workingDirectory: string,
): OpenRequest[] {
  const requests: OpenRequest[] = [];
  let positionalOnly = false;
  for (const argument of args) {
    if (!positionalOnly && argument === '--') {
      positionalOnly = true;
      continue;
    }
    if (!positionalOnly && argument.startsWith('-')) {
      continue;
    }
    const request = resolveOpenArgument(argument, workingDirectory);
    if (request !== undefined) {
      requests.push(request);
    }
  }
  return requests;
}
