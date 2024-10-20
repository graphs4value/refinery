/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import type { RelationMetadata } from '../xtext/xtextServiceResults';

export default function isBuiltIn(metadata: RelationMetadata): boolean {
  return metadata.name.startsWith('builtin::');
}
