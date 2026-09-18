/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
import { app, protocol } from 'electron';

import appName from './appName';
import runGUI from './gui/runGUI';
import runHeadless from './headless/runHeadless';
import logger from './logger';
import cleanup from './utils/cleanup';
import { isLinux, isWindows } from './utils/platform';

app.setName(appName);
if (isLinux) {
  app.setDesktopName(appName);
}
if (isWindows) {
  app.setAppUserModelId(appName);
}

let quitting = false;
app.on('will-quit', (event) => {
  if (quitting) {
    return;
  }
  // Wait for cleanup handlers to settle before quitting.
  event.preventDefault();
  cleanup()
    .finally(() => {
      quitting = true;
      app.quit();
    })
    .catch((error) =>
      logger.error({ err: error }, 'Fatal error during shutdown'),
    );
});

protocol.registerSchemesAsPrivileged([
  {
    scheme: 'app',
    privileges: {
      standard: true,
      secure: true,
      allowServiceWorkers: true,
      supportFetchAPI: true,
      corsEnabled: true,
      codeCache: true,
      allowExtensions: true,
    },
  },
]);

async function run() {
  const ipcEndpoint = process.env['REFINERY_IPC_ENDPOINT'];
  if (ipcEndpoint) {
    await runHeadless(ipcEndpoint);
  } else {
    await runGUI();
  }
}

run().catch((error) => {
  logger.error({ err: error }, 'Fatal error during startup');
  cleanup()
    .finally(() => process.exit(-1))
    .catch((error) =>
      logger.error({ err: error }, 'Fatal error during process exit'),
    );
});
