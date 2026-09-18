/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

export const isMac = process.platform === 'darwin';
export const isWindows = process.platform === 'win32';
export const isLinux = !isMac && !isWindows;
