/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { PropertiesFile } from 'java-properties';
import type { Transformer } from 'unified';
import { visit } from 'unist-util-visit';

const TEMPLATE_REGEXP = /@@@([a-zA-Z\d._-]*)@@@/g;

export default function remarkReplaceVariables({
  propertiesPath,
}: {
  propertiesPath?: string;
}): Transformer {
  if (propertiesPath === undefined) {
    throw new Error('propertiesPath is required');
  }

  const variables = new PropertiesFile(propertiesPath);

  function substitution(substring: string, name: string): string {
    if (name === '') {
      // Escape sequence.
      return '@@@';
    }
    const value = variables.get(name);
    if (value !== undefined) {
      return String(value);
    }
    return substring;
  }

  return (root) => {
    visit(root, (node) => {
      if (!('value' in node)) {
        return;
      }
      const { value } = node;
      if (typeof value !== 'string') {
        return;
      }
      /* eslint-disable-next-line no-param-reassign --
       * Here we are transforming the mdast `node` in-place.
       */
      node.value = value.replace(TEMPLATE_REGEXP, substitution);
    });
  };
}
