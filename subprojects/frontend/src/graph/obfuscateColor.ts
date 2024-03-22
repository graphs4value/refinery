/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

const regExp = /\d/g;
const offset = 'g'.charCodeAt(0) - '0'.charCodeAt(0);

/*
 * The SVG animation framework we use garbles all numbers while interpolating,
 * so we mask numbers in hex color codes by replacing them with letters.
 *
 * @param color The hex code.
 * @return The hex code with no number characters.
 */
export default function obfuscateColor(color: string): string {
  return color.replaceAll(regExp, (match) =>
    String.fromCharCode(match.charCodeAt(0) + offset),
  );
}
