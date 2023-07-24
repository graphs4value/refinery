/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import type { PluginOption } from 'vite';

import type BackendConfig from '../src/xtext/BackendConfig';
import { ENDPOINT } from '../src/xtext/BackendConfig';

export default function backendConfigVitePlugin(
  backendConfig: BackendConfig,
): PluginOption {
  return {
    name: 'backend-config',
    apply: 'serve',
    configureServer(server) {
      const config = JSON.stringify(backendConfig);
      server.middlewares.use((req, res, next) => {
        if (req.url === `/${ENDPOINT}`) {
          res.setHeader('Content-Type', 'application/json');
          res.end(config);
        } else {
          next();
        }
      });
    },
  };
}

export type { default as BackendConfig } from '../src/xtext/BackendConfig';
export { ENDPOINT as CONFIG_ENDPOINT } from '../src/xtext/BackendConfig';
