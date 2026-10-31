/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

export const MEBIBYTE = 1024 * 1024;
export const GIBIBYTE = 1024 * MEBIBYTE;
export const MIN_MAX_MEMORY_BYTES = 128 * MEBIBYTE;
/** JVM compressed references become less efficient above this threshold. */
export const JVM_COMPRESSED_OOPS_THRESHOLD_BYTES = 25 * GIBIBYTE;

export function getDefaultMaxMemoryBytes(systemMemoryBytes: number): number {
  return Math.min(
    Math.max(Math.floor(systemMemoryBytes / 4), MIN_MAX_MEMORY_BYTES),
    JVM_COMPRESSED_OOPS_THRESHOLD_BYTES,
  );
}
