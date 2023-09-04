/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

/* eslint-disable @typescript-eslint/no-redeclare -- Declare types with their companion objects */

import { z } from 'zod';

export const ENDPOINT = 'config.json';

const BackendConfig = z.object({
  webSocketURL: z.string().url().optional(),
});

type BackendConfig = z.infer<typeof BackendConfig>;

export default BackendConfig;
