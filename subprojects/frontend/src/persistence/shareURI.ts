/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

export type ShareVersion = 1 | 2;

const FRAGMENT_PREFIX_V1 = '#/1/';

const FRAGMENT_PREFIX_V2 = '#/2/';

export function createShareFragment(
  version: ShareVersion,
  value: string,
): string {
  switch (version) {
    case 1:
      return `${FRAGMENT_PREFIX_V1}${value}`;
    case 2:
      return `${FRAGMENT_PREFIX_V2}${value}`;
    default:
      throw new Error(`Unsupported compressor version: ${String(version)}`);
  }
}

export function parseShareFragment(
  fragment: string,
): { version: ShareVersion; text: string } | undefined {
  if (fragment.startsWith(FRAGMENT_PREFIX_V1)) {
    return { version: 1, text: fragment.slice(FRAGMENT_PREFIX_V1.length) };
  }
  if (fragment.startsWith(FRAGMENT_PREFIX_V2)) {
    return { version: 2, text: fragment.slice(FRAGMENT_PREFIX_V2.length) };
  }
  return undefined;
}

export function isShareFragment(fragment: string): boolean {
  return parseShareFragment(fragment) !== undefined;
}

export function parseShareURI(uri: string): string | undefined {
  let url: URL;
  try {
    url = new URL(uri.trim());
  } catch {
    return undefined;
  }
  const isWebURI = url.protocol === 'http:' || url.protocol === 'https:';
  const isDesktopURI =
    url.protocol === 'refinery:' &&
    url.hostname.toLowerCase() === 'open' &&
    (url.pathname === '' || url.pathname === '/');
  if (!isWebURI && !isDesktopURI) {
    return undefined;
  }
  return isShareFragment(url.hash) ? url.hash : undefined;
}
