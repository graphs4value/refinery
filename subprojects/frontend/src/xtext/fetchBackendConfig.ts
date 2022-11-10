/* eslint-disable @typescript-eslint/no-redeclare -- Declare types with their companion objects */

import { z } from 'zod';

export const BackendConfig = z.object({
  webSocketURL: z.string().url(),
});

export type BackendConfig = z.infer<typeof BackendConfig>;

export default async function fetchBackendConfig(): Promise<BackendConfig> {
  const configURL = `${import.meta.env.BASE_URL}config.json`;
  const response = await fetch(configURL);
  const rawConfig = (await response.json()) as unknown;
  return BackendConfig.parse(rawConfig);
}
