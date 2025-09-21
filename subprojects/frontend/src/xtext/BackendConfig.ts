/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

/* eslint-disable @typescript-eslint/no-redeclare -- Declare types with their companion objects */

import { z } from 'zod/v4';

export const ENDPOINT = 'config.json';

const BackendConfig = z.object({
  apiBase: z.url().optional(),
  webSocketURL: z.url().optional(),
  chatURL: z.url().optional(),
});

type BackendConfig = z.infer<typeof BackendConfig>;

export default BackendConfig;
