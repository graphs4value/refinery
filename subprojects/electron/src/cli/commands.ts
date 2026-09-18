/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

const commands = [
  'check',
  'v',
  'concretize',
  'c',
  'generate',
  'g',
  'semantics',
  's',
  'render',
  'r',
] as const;

export default commands;

export type Command = (typeof commands)[number];
