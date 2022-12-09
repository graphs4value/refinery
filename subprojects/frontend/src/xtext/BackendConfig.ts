/* eslint-disable @typescript-eslint/no-redeclare -- Declare types with their companion objects */

import { z } from 'zod';

export const ENDPOINT = 'config.json';

const BackendConfig = z.object({
  webSocketURL: z.string().url(),
});

type BackendConfig = z.infer<typeof BackendConfig>;

export default BackendConfig;
