/*
 * Copyright (c) 2024 The Refinery Authors
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { Zstd } from '@hpcc-js/wasm-zstd';
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

const componentName = 'TryInRefinery';
const componentPath = `@site/src/components/${componentName}`;

function isTryInRefineryImport(node: Node): boolean {
  return isImport(node, componentPath);
}

function createImportNode(): MdxjsEsm {
  const componentPathString = JSON.stringify(componentPath);
  return {
    type: 'mdxjsEsm',
    value: `import ${componentName} from ${componentPathString}`,
    data: {
      estree: {
        type: 'Program',
        body: [
          {
            type: 'ImportDeclaration',
            specifiers: [
              {
                type: 'ImportDefaultSpecifier',
                local: { type: 'Identifier', name: componentName },
              },
            ],
            source: {
              type: 'Literal',
              value: componentPath,
              raw: componentPathString,
            },
          },
        ],
        sourceType: 'module',
      },
    },
  };
}

function isRefineryCode(node: Node): node is Code {
  return isCode(node) && node.lang === 'refinery';
}

class LiterateTransformer {
  needsImport = false;

  private saved = new Map<string, string>();

  private last: string | undefined;

  constructor(private readonly zstd: Zstd) {}

  transform(code: Code): Node[] {
    const { meta } = code;
    if (meta === undefined || meta === null) {
      return [code];
    }
    let value = `${code.value.trim()}\n`;

    const continueMatch = /\bcontinue(?:=(\w+))?\b/.exec(meta);
    if (continueMatch !== null) {
      let prefix: string;
      const continueID = continueMatch[1];
      if (continueID === undefined) {
        if (this.last === undefined) {
          throw new Error('No code block to continue');
        }
        prefix = this.last;
      } else {
        const savedPrefix = this.saved.get(continueID);
        if (savedPrefix === undefined) {
          throw new Error(
            `Checkpoint to continue '${continueID}' was not found`,
          );
        }
        prefix = savedPrefix;
      }
      value = `${prefix}\n${value}`;
    }

    this.last = value;

    const checkpointMatch = /\bcheckpoint=(\w+)\b/.exec(meta);
    if (checkpointMatch?.[1] !== undefined) {
      if (this.saved.has(checkpointMatch[1])) {
        throw new Error(`Duplicate checkpoint ID ${checkpointMatch[1]}`);
      }
      this.saved.set(checkpointMatch[1], value);
    }

    const result: Node[] = [];

    if (!/\bhidden\b/.test(meta)) {
      result.push(code);
    }

    if (/\btry\b/.test(meta)) {
      this.needsImport = true;
      result.push({
        type: 'mdxJsxFlowElement',
        name: componentName,
        attributes: [
          {
            type: 'mdxJsxAttribute',
            name: 'href',
            value: this.compressURL(value),
          },
        ],
      } as MdxJsxFlowElement);
    }

    return result;
  }

  private compressURL(value: string): string {
    const encoder = new TextEncoder();
    const encodedBuffer = encoder.encode(value);
    const compressedBuffer = this.zstd.compress(encodedBuffer, 20);
    const base64 = Buffer.from(compressedBuffer).toString('base64url');
    return `https://refinery.services/#/1/${base64}`;
  }
}

export default async function loadRemarkRefineryPlugin(): Promise<
  () => Transformer
> {
  const zstd = await Zstd.load();
  return () => (root) => {
    const transformer = new LiterateTransformer(zstd);
    let alreadyImported = false;
    replaceChildrenRecursively(
      root,
      (node) => {
        if (isTryInRefineryImport(node)) {
          alreadyImported = true;
        }
        return isRefineryCode(node);
      },
      (node) => transformer.transform(node),
    );
    if (transformer.needsImport && !alreadyImported) {
      if (isParent(root)) {
        root.children.unshift(createImportNode());
      }
    }
  };
}
