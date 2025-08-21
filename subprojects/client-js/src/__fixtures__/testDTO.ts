/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import z from 'zod/v4';

export const Ping = z.object({
  ping: z.string(),
});

export type Ping = z.infer<typeof Ping>;

export const Pong = z.object({
  pong: z.string(),
});

export type Pong = z.infer<typeof Pong>;
