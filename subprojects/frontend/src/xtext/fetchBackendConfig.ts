/*
 * SPDX-FileCopyrightText: 2021-2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import BackendConfig, { ENDPOINT } from './BackendConfig';

export type BackendConfigWithDefaults = BackendConfig & {
  apiBase: NonNullable<BackendConfig['apiBase']>;
  webSocketURL: NonNullable<BackendConfig['webSocketURL']>;
};

async function fetchRawConfig() {
  if ('refinery' in window) {
    // We're running inside Electron, ask the main process directly for configuration.
    return window.refinery.getBackendConfig();
  }
  const configURL = `${import.meta.env.BASE_URL}${ENDPOINT}`;
  const response = await fetch(configURL);
  return response.json() as Promise<unknown>;
}

export default async function fetchBackendConfig(): Promise<BackendConfigWithDefaults> {
  const rawConfig = await fetchRawConfig();
  const parsedConfig = BackendConfig.parse(rawConfig);
  return {
    ...parsedConfig,
    apiBase: parsedConfig.apiBase ?? `${window.origin}/api/v1`,
    webSocketURL:
      parsedConfig.webSocketURL ??
      `${window.origin.replace(/^http/, 'ws')}/xtext-service`,
  };
}
