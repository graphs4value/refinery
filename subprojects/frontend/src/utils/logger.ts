/*
 * SPDX-FileCopyrightText: 2021-2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import styles, { type CSPair } from 'ansi-styles';
import { format } from 'date-fns';
import pino from 'pino';

const levelColors: Record<string, CSPair> = {
  FATAL: styles.red,
  ERROR: styles.red,
  WARN: styles.yellow,
  INFO: styles.green,
  DEBUG: styles.green,
  TRACE: styles.green,
};

const isChromium = 'chrome' in window;

const logger = pino({
  level: import.meta.env.DEV ? 'debug' : 'info',
  browser: {
    asObject: true,
    write: (logObj) => {
      const { level, msg, name, time, err, ...rest } = (logObj ?? {}) as Record<
        string,
        unknown
      >;
      const timeFormatted =
        typeof time === 'number' ? format(new Date(time), `HH:mm:ss.sss`) : '';
      const levelUppercased = String(level)?.toUpperCase() ?? 'ERROR';
      const levelColor = levelColors[levelUppercased] ?? styles.reset;
      const formattedName = typeof name === 'string' ? ` (${name})` : '';
      const formattedMessage =
        typeof msg === 'string' ? msg : JSON.stringify(msg, null, 2);
      const message = isChromium
        ? `[${timeFormatted}] ${levelColor.open}${levelUppercased}${levelColor.close}${formattedName}: ${styles.cyan.open}${formattedMessage}${styles.cyan.close}`
        : `[${timeFormatted}] ${levelUppercased}${formattedName}: ${formattedMessage}`;
      const args: unknown[] = [message];
      if (err) {
        args.push('\n', err);
      }
      if (Object.keys(rest).length > 0) {
        args.push('\n', rest);
      }
      if (levelUppercased === 'ERROR' || levelUppercased === 'FATAL') {
        // eslint-disable-next-line no-console -- This is a logger implementation.
        console.error(...args);
      } else if (levelUppercased === 'WARN') {
        // eslint-disable-next-line no-console -- This is a logger implementation.
        console.warn(...args);
      } else {
        // eslint-disable-next-line no-console -- This is a logger implementation.
        console.log(...args);
      }
    },
    formatters: {
      level: (label) => ({ level: label }),
    },
  },
});

window.addEventListener('error', (event) => {
  logger.error({ err: event.error as unknown }, 'Uncaught error');
});

window.addEventListener('unhandledrejection', (event) => {
  logger.error({ err: event.reason as unknown }, 'Unhandler promise rejection');
});

logger.info(
  'Version: %s %s',
  import.meta.env.VITE_PACKAGE_NAME,
  import.meta.env.VITE_PACKAGE_VERSION,
);

logger.info('Debug mode: %s', import.meta.env.DEV);

export default logger;
