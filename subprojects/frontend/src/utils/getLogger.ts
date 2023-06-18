/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import styles, { type CSPair } from 'ansi-styles';
import log from 'loglevel';
import prefix from 'loglevel-plugin-prefix';

const colors: Partial<Record<string, CSPair>> = {
  TRACE: styles.magenta,
  DEBUG: styles.cyan,
  INFO: styles.blue,
  WARN: styles.yellow,
  ERROR: styles.red,
};

prefix.reg(log);

if (import.meta.env.DEV) {
  log.setLevel(log.levels.DEBUG);
} else {
  log.setLevel(log.levels.WARN);
}

if ('chrome' in window) {
  // Only Chromium supports console ANSI escape sequences.
  prefix.apply(log, {
    format(level, name, timestamp) {
      const formattedTimestamp = `${styles.gray.open}[${timestamp.toString()}]${
        styles.gray.close
      }`;
      const levelColor = colors[level.toUpperCase()] || styles.red;
      const formattedLevel = `${levelColor.open}${level}${levelColor.close}`;
      const formattedName = `${styles.green.open}(${name || 'root'})${
        styles.green.close
      }`;
      return `${formattedTimestamp} ${formattedLevel} ${formattedName}`;
    },
  });
} else {
  prefix.apply(log, {
    template: '[%t] %l (%n)',
  });
}

const appLogger = log.getLogger(import.meta.env.VITE_PACKAGE_NAME);

appLogger.info(
  'Version:',
  import.meta.env.VITE_PACKAGE_NAME,
  import.meta.env.VITE_PACKAGE_VERSION,
);
appLogger.info('Debug mode:', import.meta.env.DEV);

export default function getLogger(name: string | symbol): log.Logger {
  return log.getLogger(
    `${import.meta.env.VITE_PACKAGE_NAME}.${name.toString()}`,
  );
}
