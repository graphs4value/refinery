/*
 * Copyright (C) 2018 by Marijn Haverbeke <marijn@haverbeke.berlin> and others
 * Copyright (C) 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: MIT
 */

declare module '*.grammar' {
  import type { LRParser } from '@lezer/lr';

  export const parser: LRParser;
}

declare module '*.terms' {
  export const QualifiedNameSeparator: number;
}
