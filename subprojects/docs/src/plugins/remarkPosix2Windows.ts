/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 * Copyright (c) 2024 The Refinery Authors
 *
 * SPDX-License-Identifier: MIT AND EPL-2.0
 *
 * This file is based on
 * https://github.com/facebook/docusaurus/blob/e4ecffe41878728acff55a8370bd7440706c02f7/packages/docusaurus-remark-plugin-npm2yarn/src/index.ts
 * but was changed to convert shell commands to POSIX to Windows syntax.
 */

import type { Code } from 'mdast';
import type { MdxjsEsm, MdxJsxFlowElement } from 'mdast-util-mdx';
import type { Transformer } from 'unified';
import type { Node } from 'unist';

import {
  isCode,
  isImport,
  isParent,
  replaceChildrenRecursively,
} from './remarkPluginUtils';

function isTabImport(node: Node): boolean {
  return isImport(node, '@theme/Tabs');
}

function isPosix2Windows(node: Node): node is Code {
  return isCode(node) && node.meta === 'posix2windows';
}

function createTabItem(
  code: string,
  node: Code,
  value: string,
  label: string,
): MdxJsxFlowElement {
  return {
    type: 'mdxJsxFlowElement',
    name: 'TabItem',
    attributes: [
      {
        type: 'mdxJsxAttribute',
        name: 'value',
        value,
      },
      {
        type: 'mdxJsxAttribute',
        name: 'label',
        value: label,
      },
    ],
    children: [
      {
        type: node.type,
        lang: node.lang,
        value: code,
      },
    ],
  };
}

function transformNode(node: Code): MdxJsxFlowElement[] {
  const posixCode = node.value;
  const windowsCode = posixCode.replaceAll(
    /(^\w*)\.\//gm,
    (_substring, prefix: string) => `${prefix}.\\`,
  );
  return [
    {
      type: 'mdxJsxFlowElement',
      name: 'Tabs',
      attributes: [
        {
          type: 'mdxJsxAttribute',
          name: 'groupId',
          value: 'posix2windows',
        },
      ],
      children: [
        createTabItem(posixCode, node, 'posix', 'Linux or macOS'),
        createTabItem(windowsCode, node, 'windows', 'Windows (PowerShell)'),
      ],
    },
  ];
}

function createImportNode(): MdxjsEsm {
  return {
    type: 'mdxjsEsm',
    value:
      "import Tabs from '@theme/Tabs'\nimport TabItem from '@theme/TabItem'",
    data: {
      estree: {
        type: 'Program',
        body: [
          {
            type: 'ImportDeclaration',
            specifiers: [
              {
                type: 'ImportDefaultSpecifier',
                local: { type: 'Identifier', name: 'Tabs' },
              },
            ],
            attributes: [],
            source: {
              type: 'Literal',
              value: '@theme/Tabs',
              raw: "'@theme/Tabs'",
            },
          },
          {
            type: 'ImportDeclaration',
            specifiers: [
              {
                type: 'ImportDefaultSpecifier',
                local: { type: 'Identifier', name: 'TabItem' },
              },
            ],
            attributes: [],
            source: {
              type: 'Literal',
              value: '@theme/TabItem',
              raw: "'@theme/TabItem'",
            },
          },
        ],
        sourceType: 'module',
      },
    },
  };
}

export default function remarkPosix2Windows(): Transformer {
  return (root) => {
    let alreadyImported = false;
    const transformed = replaceChildrenRecursively(
      root,
      (node) => {
        if (isTabImport(node)) {
          alreadyImported = true;
        }
        return isPosix2Windows(node);
      },
      transformNode,
    );
    if (transformed && !alreadyImported) {
      if (isParent(root)) {
        root.children.unshift(createImportNode());
      } else {
        throw new Error("Cannot import '@theme/Tabs'");
      }
    }
  };
}
