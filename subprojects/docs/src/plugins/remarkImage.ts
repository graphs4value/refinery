/*
 * Copyright (c) 2024 The Refinery Authors
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import type { Image } from 'mdast';
import type {
  MdxjsEsm,
  MdxJsxAttribute,
  MdxJsxFlowElement,
} from 'mdast-util-mdx';
import type { Transformer } from 'unified';
import type { Node } from 'unist';
import { visit } from 'unist-util-visit';

import {
  isImport,
  isLiteral,
  isParent,
  replaceChildrenRecursively,
} from './remarkPluginUtils';

function isImage(node: Node): node is Image {
  return node.type === 'image';
}

function createImport(componentName: string, url: string): MdxjsEsm {
  const urlString = JSON.stringify(url);
  return {
    type: 'mdxjsEsm',
    value: `import ${componentName} from ${urlString}`,
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
              value: url,
              raw: urlString,
            },
          },
        ],
        sourceType: 'module',
      },
    },
  };
}

function addCommonAttributes(
  image: Image,
  attributes: MdxJsxAttribute[],
): void {
  const { alt, title } = image;
  if (alt) {
    attributes.push({
      type: 'mdxJsxAttribute',
      name: 'alt',
      value: alt,
    });
  }
  if (title) {
    attributes.push({
      type: 'mdxJsxAttribute',
      name: 'title',
      value: title,
    });
  }
}

function getOriginalURL(url: string) {
  const index = url.indexOf('?');
  if (index < 0) {
    return url;
  }
  return url.substring(0, index);
}

function getResponsiveURL(url: string) {
  const separator = url.includes('?') ? '&' : '?';
  let format: string;
  let quality: number;
  if (url.endsWith('.png')) {
    format = 'png';
    quality = 100;
  } else {
    format = 'webp';
    quality = 85;
  }
  return `${url}${separator}format=${format}&quality=${quality}&sizes[]=2640,sizes[]=1928,sizes[]=1320,sizes[]=964,sizes[]=640,sizes[]=320&placeholder=true&rl`;
}

function getExpressionAttribute(name: string, value: string): MdxJsxAttribute {
  return {
    type: 'mdxJsxAttribute',
    name,
    value: {
      type: 'mdxJsxAttributeValueExpression',
      value,
      data: {
        estree: {
          type: 'Program',
          body: [
            {
              type: 'ExpressionStatement',
              expression: {
                type: 'Identifier',
                name: value,
              },
            },
          ],
          sourceType: 'module',
        },
      },
    },
  };
}

class ImageTransformer {
  requiredImports = new Map<string, MdxjsEsm>();

  transformImage(image: Image, index: number): Node[] {
    const { url } = image;
    if (url.endsWith('.svg')) {
      return this.transformSVGImage(image, index);
    }
    if (url.includes('|')) {
      return this.transformThemedImage(image, index);
    }
    return this.transformResponsiveImage(image, index);
  }

  private transformSVGImage(image: Image, index: number): Node[] {
    const { url } = image;
    const componentName = `Image__${index}`;
    const attributes = [getExpressionAttribute('Component', componentName)];
    addCommonAttributes(image, attributes);
    this.requireImport('SVGImage', '@site/src/components/ResponsiveImage/SVG');

    return [
      createImport(componentName, url),
      {
        type: 'mdxJsxFlowElement',
        name: 'SVGImage',
        attributes,
      } as MdxJsxFlowElement,
    ];
  }

  private transformThemedImage(image: Image, index: number): Node[] {
    const { url } = image;
    const [urlLight, urlDark] = url.split('|');
    if (urlLight === undefined || urlDark === undefined) {
      throw new Error(`Invalid themed image: ${url}`);
    }
    const componentNameLight = `ImageLight__${index}`;
    const componentNameDark = `ImageDark__${index}`;
    const originalLight = `originalImageLight__${index}`;
    const originalDark = `originalImageDark${index}`;
    this.requireImport(
      'ThemedImage',
      '@site/src/components/ResponsiveImage/Themed',
    );

    const attributes: MdxJsxAttribute[] = [
      getExpressionAttribute('light', componentNameLight),
      getExpressionAttribute('dark', componentNameDark),
      getExpressionAttribute('originalLight', originalLight),
      getExpressionAttribute('originalDark', originalDark),
    ];
    addCommonAttributes(image, attributes);

    return [
      createImport(componentNameLight, getResponsiveURL(urlLight)),
      createImport(componentNameDark, getResponsiveURL(urlDark)),
      createImport(originalLight, getOriginalURL(urlLight)),
      createImport(originalDark, getOriginalURL(urlDark)),
      {
        type: 'mdxJsxFlowElement',
        name: 'ThemedImage',
        attributes,
      } as MdxJsxFlowElement,
    ];
  }

  private transformResponsiveImage(image: Image, index: number): Node[] {
    const { url } = image;
    const componentName = `Image__${index}`;
    const original = `originalImage__${index}`;
    this.requireImport(
      'ResponsiveImage',
      '@site/src/components/ResponsiveImage',
    );

    const attributes: MdxJsxAttribute[] = [
      getExpressionAttribute('image', componentName),
      getExpressionAttribute('original', original),
    ];
    addCommonAttributes(image, attributes);

    return [
      createImport(componentName, getResponsiveURL(url)),
      createImport(original, getOriginalURL(url)),
      {
        type: 'mdxJsxFlowElement',
        name: 'ResponsiveImage',
        attributes,
      } as MdxJsxFlowElement,
    ];
  }

  private requireImport(componentName: string, url: string) {
    if (!this.requiredImports.has(url)) {
      this.requiredImports.set(url, createImport(componentName, url));
    }
  }
}

export default function remarkImage(): Transformer {
  return (root) => {
    let counter = 0;
    const transformer = new ImageTransformer();
    replaceChildrenRecursively(root, isImage, (image) => {
      const result = transformer.transformImage(image, counter);
      counter += 1;
      return result;
    });
    const { requiredImports } = transformer;
    const imported = new Set<string>();
    visit(root, (node) => {
      if (isLiteral(node)) {
        requiredImports.forEach((_, key) => {
          if (isImport(node, key)) {
            imported.add(key);
          }
        });
      }
    });
    const imports: MdxjsEsm[] = [];
    requiredImports.forEach((node, key) => {
      if (!imported.has(key)) {
        imports.push(node);
      }
    });
    if (isParent(root)) {
      root.children.unshift(...imports);
    }
  };
}
