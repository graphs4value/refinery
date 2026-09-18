/*
 * SPDX-FileCopyrightText: 2021-2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

export default function isLocalBackend(webSocketURL: string): boolean {
  const { hostname } = new URL(webSocketURL);
  return (
    hostname === 'localhost' ||
    hostname === '::1' ||
    hostname === '[::1]' ||
    /^127(\.\d{1,3}){3}$/.test(hostname)
  );
}
