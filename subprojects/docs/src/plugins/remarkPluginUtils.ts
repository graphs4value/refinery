/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 * Copyright (c) 2024 The Refinery Authors
 *
 * SPDX-License-Identifier: MIT AND EPL-2.0
 *
 * This file is based on
 * https://github.com/facebook/docusaurus/blob/e4ecffe41878728acff55a8370bd7440706c02f7/packages/docusaurus-remark-plugin-npm2yarn/src/index.ts
 */

import type { Code, Literal } from 'mdast';
import type { Node, Parent } from 'unist';

export function isParent(node: Node): node is Parent {
  return 'children' in node && Array.isArray(node.children);
}

export function replaceChildrenRecursively<T extends Node>(
  node: Node,
  shoulReplace: (node: Node) => node is T,
  replacement: (node: T) => Node[],
): boolean {
  if (!isParent(node)) {
    return false;
  }
  let didReplace = false;
  let index = 0;
  while (index < node.children.length) {
    const child = node.children[index];
    if (child !== undefined && shoulReplace(child)) {
      const result = replacement(child);
      node.children.splice(index, 1, ...result);
      index += result.length;
      didReplace = true;
    } else {
      if (
        child !== undefined &&
        replaceChildrenRecursively(child, shoulReplace, replacement)
      ) {
        didReplace = true;
      }
      index += 1;
    }
  }
  return didReplace;
}

export function isCode(node: Node): node is Code {
  return node.type === 'code';
}

export function isLiteral(node: Node): node is Literal {
  return node.type === 'mdxjsEsm';
}

export function isImport(node: Node, importedPath: string): boolean {
  return isLiteral(node) && node.value.includes(importedPath);
}
