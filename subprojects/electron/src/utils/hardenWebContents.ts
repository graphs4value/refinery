/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { shell, type Session, type WebContents } from 'electron';

import getLogger from '../logger/getLogger';

const log = getLogger('utils.hardenWebContents');

export type Permission = Session['setPermissionCheckHandler'] extends (
  handler: ((webContents: unknown, permission: infer P) => boolean) | null,
) => void
  ? P
  : never;

const hardenedSessions = new WeakSet<Session>();

// There permission checks are automatically triggered by Chromium
// and our application manifest, but we can safely deny them.
const silentPermissions: Permission[] = [
  'media',
  'geolocation',
  // This is checked by Chromium, but not part of Electron's type signature.
  'web-app-installation' as Permission,
];

const allowedExternalHosts = ['refinery.tools', 'github.com'];

function hardenSession(
  sessionToHarden: Session,
  allowedOrigins: string[],
  allowedPermissions: Permission[],
): void {
  if (hardenedSessions.has(sessionToHarden)) {
    return;
  }
  hardenedSessions.add(sessionToHarden);
  sessionToHarden.setPermissionCheckHandler(
    (webContents, permission, requestingOrigin, details) => {
      requestingOrigin =
        details.securityOrigin ?? details.embeddingOrigin ?? requestingOrigin;
      if (requestingOrigin.endsWith('/')) {
        requestingOrigin = requestingOrigin.slice(0, -1);
      }
      const allowed =
        (allowedOrigins.includes(requestingOrigin) &&
          allowedPermissions.includes(permission)) ||
        // Let devtools have access to all required permissions in debug mode.
        (process.isDev && requestingOrigin.startsWith('devtools://'));
      if (allowed) {
        log.debug(
          { webContentsID: webContents?.id, permission, requestingOrigin },
          'Allowing browser permission',
        );
      } else if (silentPermissions.includes(permission)) {
        log.debug(
          { webContentsID: webContents?.id, permission, requestingOrigin },
          'Denying browser permission',
        );
      } else {
        log.warn(
          { webContentsID: webContents?.id, permission, requestingOrigin },
          'Denying browser permission',
        );
      }
      return allowed;
    },
  );
}

export default function hardenWebContents(
  webContentsToHarden: WebContents,
  allowedOrigins: string[],
  allowedPermissions: Permission[],
): void {
  hardenSession(
    webContentsToHarden.session,
    allowedOrigins,
    allowedPermissions,
  );
  webContentsToHarden.on('will-frame-navigate', (event) => {
    let url: URL | undefined;
    try {
      url = new URL(event.url);
    } catch (error) {
      log.error(
        { webContentsID: webContentsToHarden.id, url: event.url, err: error },
        'Trying to navigate to invalid URL',
      );
    }
    if (url === undefined || !allowedOrigins.includes(url.origin)) {
      log.warn(
        { webContentsID: webContentsToHarden.id, url: event.url },
        'Preventing navigation to forbidden origin',
      );
      event.preventDefault();
    }
  });
  const sanitizeURL = (url: string) => {
    let parsedURL: URL;
    try {
      parsedURL = new URL(url);
    } catch (error) {
      log.error(
        { webContentsID: webContentsToHarden.id, url, err: error },
        'Preventing new window with invalid URL',
      );
      return undefined;
    }
    if (allowedOrigins.includes(parsedURL.origin)) {
      log.warn(
        { webContentsID: webContentsToHarden.id, url },
        'Preventing new window with internal URL',
      );
      return undefined;
    }
    if (parsedURL.protocol !== 'https:') {
      log.warn(
        { webContentsID: webContentsToHarden.id, url },
        'Preventing new window with unsafe protocol',
      );
      return undefined;
    }
    if (!allowedExternalHosts.includes(parsedURL.host)) {
      log.warn(
        { webContentsID: webContentsToHarden.id, url },
        'Preventing new window with untrusted host',
      );
      return undefined;
    }
    return parsedURL.href;
  };
  webContentsToHarden.setWindowOpenHandler(({ url }) => {
    const safeURL = sanitizeURL(url);
    if (safeURL !== undefined) {
      log.debug(
        { webContentsID: webContentsToHarden.id, url: safeURL },
        'Opening URL in system browser',
      );
      shell
        .openExternal(safeURL)
        .catch((error) =>
          log.error(
            { webContentsID: webContentsToHarden.id, url: safeURL, err: error },
            'Failed to open URL in system browser',
          ),
        );
    }
    return { action: 'deny' };
  });
}
