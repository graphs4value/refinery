import styles, { CSPair } from 'ansi-styles';
import log from 'loglevel';
import * as prefix from 'loglevel-plugin-prefix';

const colors: Record<string, CSPair> = {
  TRACE: styles.magenta,
  DEBUG: styles.cyan,
  INFO: styles.blue,
  WARN: styles.yellow,
  ERROR: styles.red,
};

prefix.reg(log);

if (DEBUG) {
  log.setLevel(log.levels.DEBUG);
} else {
  log.setLevel(log.levels.WARN);
}

if ('chrome' in window) {
  // Only Chromium supports console ANSI escape sequences.
  prefix.apply(log, {
    format(level, name, timestamp) {
      const formattedTimestamp = `${styles.gray.open}[${timestamp.toString()}]${styles.gray.close}`;
      const levelColor = colors[level.toUpperCase()] || styles.red;
      const formattedLevel = `${levelColor.open}${level}${levelColor.close}`;
      const formattedName = `${styles.green.open}(${name || 'root'})${styles.green.close}`;
      return `${formattedTimestamp} ${formattedLevel} ${formattedName}`;
    },
  });
} else {
  prefix.apply(log, {
    template: '[%t] %l (%n)',
  });
}

const appLogger = log.getLogger(PACKAGE_NAME);

appLogger.info('Version:', PACKAGE_NAME, PACKAGE_VERSION);
appLogger.info('Debug mode:', DEBUG);

export function getLoggerFromRoot(name: string | symbol): log.Logger {
  return log.getLogger(name);
}

export function getLogger(name: string | symbol): log.Logger {
  return getLoggerFromRoot(`${PACKAGE_NAME}.${name.toString()}`);
}
