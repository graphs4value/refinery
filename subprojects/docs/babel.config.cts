/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 * Copyright (c) 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: MIT AND EPL-2.0
 */

import type { TransformOptions } from '@babel/core';

module.exports = {
  presets: [require.resolve('@docusaurus/core/lib/babel/preset')],
} satisfies TransformOptions;
