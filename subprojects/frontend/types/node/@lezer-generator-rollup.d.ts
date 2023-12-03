/*
 * Copyright (C) 2018 by Marijn Haverbeke <marijn@haverbeke.berlin> and others
 * Copyright (C) 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: MIT
 */

// We have to explicitly redeclare the type of the `./rollup` ESM export of `@lezer/generator`,
// because TypeScript can't find it on its own even with `"moduleResolution": "Node16"`.
declare module '@lezer/generator/rollup' {
  import type { PluginOption } from 'vite';

  export function lezer(): PluginOption;
}
