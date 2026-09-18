/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { describe, expect, test } from 'vitest';

import commands from './commands';
import shouldLaunchGUI from './shouldLaunchGUI';

describe('no arguments', () => {
  test('launches the GUI when no arguments are given', () => {
    expect(shouldLaunchGUI([])).toBe(true);
  });

  test('launches the GUI when the first argument is empty', () => {
    expect(shouldLaunchGUI([''])).toBe(true);
  });

  test('launches the GUI when the first argument is `--`', () => {
    expect(shouldLaunchGUI(['--'])).toBe(true);
  });
});

describe('known commands', () => {
  test.each(commands)('does not launch the GUI for `%s`', (command) => {
    expect(shouldLaunchGUI([command])).toBe(false);
  });

  test('does not launch the GUI for a known command regardless of its other arguments', () => {
    expect(shouldLaunchGUI(['check', 'model.problem'])).toBe(false);
  });
});

describe('bare file arguments', () => {
  test('launches the GUI for a single file path', () => {
    expect(shouldLaunchGUI(['model.problem'])).toBe(true);
  });

  test('launches the GUI for multiple file paths', () => {
    expect(shouldLaunchGUI(['model.problem', 'other.problem'])).toBe(true);
  });
});

describe('option-like arguments', () => {
  test('does not launch the GUI when an argument starts with `-`', () => {
    expect(shouldLaunchGUI(['-format', 'svg'])).toBe(false);
  });

  test('does not launch the GUI when an argument starts with `@`', () => {
    expect(shouldLaunchGUI(['@args.txt'])).toBe(false);
  });

  test('does not launch the GUI when a later argument looks like an option', () => {
    expect(shouldLaunchGUI(['model.problem', '-format', 'svg'])).toBe(false);
  });
});
