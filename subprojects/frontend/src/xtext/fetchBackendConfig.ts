/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import BackendConfig, { ENDPOINT } from './BackendConfig';

export default async function fetchBackendConfig(): Promise<BackendConfig> {
  const configURL = `${import.meta.env.BASE_URL}${ENDPOINT}`;
  const response = await fetch(configURL);
  const rawConfig = (await response.json()) as unknown;
  return BackendConfig.parse(rawConfig);
}
