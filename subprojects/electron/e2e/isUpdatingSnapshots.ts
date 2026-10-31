/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

export default function isUpdatingSnapshots(): boolean {
  return process.env['REFINERY_UPDATE_SNAPSHOTS'] === '1';
}
