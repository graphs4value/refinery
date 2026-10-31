/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { readFile } from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';

import {
  ServerSettings as ServerSettingsSchema,
  type ServerSettings,
} from '@tools.refinery/frontend/RefineryContextBridge';
import {
  MAX_SEMANTICS_TIMEOUT_MS,
  MIN_MODEL_GENERATION_TIMEOUT_SEC,
  MIN_SEMANTICS_TIMEOUT_MS,
  UNLIMITED_MODEL_GENERATION_TIMEOUT_SEC,
} from '@tools.refinery/frontend/settings/serverLimits';
import {
  getDefaultMaxMemoryBytes,
  MIN_MAX_MEMORY_BYTES,
} from '@tools.refinery/frontend/settings/serverMemory';
import z from 'zod/v4';

import { isMac, isWindows } from './utils/platform';

// This module is also imported by the plain Node CLI entry point. Keep the
// Electron-dependent settings store separate so ELECTRON_RUN_AS_NODE can use
// the same persisted server settings without loading the Electron runtime.

export {
  getDefaultMaxMemoryBytes,
  JVM_COMPRESSED_OOPS_THRESHOLD_BYTES,
  MEBIBYTE,
  MIN_MAX_MEMORY_BYTES,
} from '@tools.refinery/frontend/settings/serverMemory';

export function getSystemMemoryBytes(): number {
  return os.totalmem();
}

const DEFAULT_SERVER_SETTINGS: ServerSettings = {
  semanticsTimeoutMs: 10_000,
  modelGenerationTimeoutSec: 600,
  maxMemoryBytes: getDefaultMaxMemoryBytes(getSystemMemoryBytes()),
  libraryPaths: [],
  classpathJars: [],
};

export { DEFAULT_SERVER_SETTINGS };

export function getLibraryPathEnv(
  libraryPaths: readonly string[],
): string | undefined {
  return libraryPaths.length === 0
    ? undefined
    : libraryPaths.join(path.delimiter);
}

const MAX_MAX_MEMORY_BYTES = Math.max(
  getSystemMemoryBytes(),
  MIN_MAX_MEMORY_BYTES,
);

const SemanticsTimeoutSchema = z
  .int()
  .min(MIN_SEMANTICS_TIMEOUT_MS)
  .max(MAX_SEMANTICS_TIMEOUT_MS);
const ModelGenerationTimeoutSchema = z
  .int()
  .min(MIN_MODEL_GENERATION_TIMEOUT_SEC)
  .max(UNLIMITED_MODEL_GENERATION_TIMEOUT_SEC);
const MaxMemorySchema = z
  .int()
  .min(MIN_MAX_MEMORY_BYTES)
  .max(MAX_MAX_MEMORY_BYTES);
const LibraryPathSchema = z
  .string()
  .min(1)
  .refine((libraryPath) => !libraryPath.includes(path.delimiter));
const ClasspathJarSchema = z
  .string()
  .min(1)
  .refine((jarPath) => jarPath.toLowerCase().endsWith('.jar'))
  .refine((jarPath) => !jarPath.includes(path.delimiter));

export const ServerSettingsSchemaWithLimits = ServerSettingsSchema.extend({
  semanticsTimeoutMs: SemanticsTimeoutSchema,
  modelGenerationTimeoutSec: ModelGenerationTimeoutSchema,
  maxMemoryBytes: MaxMemorySchema,
  libraryPaths: z.array(LibraryPathSchema),
  classpathJars: z.array(ClasspathJarSchema),
});

/**
 * Parses settings from disk, repairing individual values that are no longer
 * valid instead of discarding unrelated settings from the file.
 */
export const ServerSettingsFromFile = ServerSettingsSchema.extend({
  semanticsTimeoutMs: SemanticsTimeoutSchema.default(
    DEFAULT_SERVER_SETTINGS.semanticsTimeoutMs,
  ).catch(DEFAULT_SERVER_SETTINGS.semanticsTimeoutMs),
  modelGenerationTimeoutSec: ModelGenerationTimeoutSchema.default(
    DEFAULT_SERVER_SETTINGS.modelGenerationTimeoutSec,
  ).catch(DEFAULT_SERVER_SETTINGS.modelGenerationTimeoutSec),
  maxMemoryBytes: MaxMemorySchema.default(
    DEFAULT_SERVER_SETTINGS.maxMemoryBytes,
  ).catch(DEFAULT_SERVER_SETTINGS.maxMemoryBytes),
  libraryPaths: z
    .array(LibraryPathSchema)
    .default(DEFAULT_SERVER_SETTINGS.libraryPaths)
    .catch(DEFAULT_SERVER_SETTINGS.libraryPaths),
  classpathJars: z
    .array(ClasspathJarSchema)
    .default(DEFAULT_SERVER_SETTINGS.classpathJars)
    .catch(DEFAULT_SERVER_SETTINGS.classpathJars),
}).default(DEFAULT_SERVER_SETTINGS);

const SettingsFileSchema = z.object({
  serverSettings: ServerSettingsFromFile,
});

export async function readServerSettingsFile(
  settingsFile: string,
): Promise<ServerSettings> {
  const contents: unknown = JSON.parse(await readFile(settingsFile, 'utf-8'));
  return SettingsFileSchema.parse(contents).serverSettings;
}

export function getSettingsFilePath(appName: string): string {
  let userDataDirectory: string;
  if (isWindows) {
    userDataDirectory =
      process.env['APPDATA'] ?? path.join(os.homedir(), 'AppData', 'Roaming');
  } else if (isMac) {
    userDataDirectory = path.join(
      os.homedir(),
      'Library',
      'Application Support',
    );
  } else {
    userDataDirectory =
      process.env['XDG_CONFIG_HOME'] ?? path.join(os.homedir(), '.config');
  }
  return path.join(userDataDirectory, appName, 'settings.json');
}
