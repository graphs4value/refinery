/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { readFile } from 'fs/promises';

/**
 * Splits at the first whitespace character, keeping the remainder intact.
 *
 * This corresponds to `.trim("\\s", 2)` in Java, but does not match the JavaScript behavior,
 * which truncates the resulting array instead.
 *
 * @param line The line to split.
 * @returns The split line.
 */
function splitFirst(line: string): string[] {
  const i = line.search(/\s/);
  return i === -1 ? [line] : [line.slice(0, i), line.slice(i + 1)];
}

/**
 * Implements JCommander parameter name trimming.
 *
 * Note that JCommander doesn't apply this trimming to option values by default,
 * leaving the conversion up to the individual value parsers. However, we ru it
 * indiscriminately on anything that starts with `-` to avoid keeping a parameter table.
 *
 * @param arg The original argument.
 * @returns The trimmed argument it it starts with `-`, otherwise the original `arg`.
 * @see https://github.com/boost-mw-poc/cbeust_jcommander/blob/e9599fed58fdf5251abb8ad08226e96ae951d302/src/main/java/com/beust/jcommander/JCommander.java#L594-L600
 */
function trimQuotes(arg: string): string {
  let trimmed = arg.trim();
  if (trimmed.startsWith('"') && trimmed.endsWith('"') && trimmed.length > 1) {
    trimmed = trimmed.slice(1, -1);
  }
  if (trimmed.startsWith('-')) {
    return trimmed;
  }
  return arg;
}

/**
 * Implements JCommander's argfile expansion strategy.
 *
 * This expansion allows empty lines and `#`-comments in an argfile,
 * but there is no argument quoting. Instead, lines are only split at the first whitespace,
 * allowing to pass arguments like `-o some file name with spaces`,
 * which splits into `['-o', 'some file name with spaces']`.
 *
 * @param args The list of arguments to expand.
 * @returns An async iterator over the expanded argument list.
 * @see https://github.com/boost-mw-poc/cbeust_jcommander/blob/e9599fed58fdf5251abb8ad08226e96ae951d302/src/main/java/com/beust/jcommander/JCommander.java#L572-L589
 */
export default async function* expandArgs(
  args: string[],
): AsyncGenerator<string, void> {
  for (const arg of args) {
    if (arg.startsWith('@')) {
      let contents;
      try {
        contents = await readFile(arg.substring(1), 'utf-8');
      } catch {
        // Ignore error and leave this argument unexpanded.
        yield arg;
        continue;
      }
      for (const line of contents.split(/\r?\n/)) {
        if (line !== '' && !line.trim().startsWith('#')) {
          yield* expandArgs(splitFirst(line));
        }
      }
    } else {
      yield trimQuotes(arg);
    }
  }
}
