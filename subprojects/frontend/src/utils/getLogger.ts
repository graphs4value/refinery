/*
 * SPDX-FileCopyrightText: 2021-2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import type { Logger } from 'pino';

import logger from './logger';

export default function getLogger(name: string): Logger {
  return logger.child({ name });
}
