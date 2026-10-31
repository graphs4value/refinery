/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

/*
 * Provisions the Zig toolchain into `build/zig`. Split from the launcher build
 * so the (large, network-bound) toolchain download is a separate, cacheable
 * Gradle step from the (fast, source-bound) cross-compile.
 */

import { ensureZig, zigExe } from './zig.mjs';

await ensureZig();
console.log(`Zig ready: ${zigExe}`);
