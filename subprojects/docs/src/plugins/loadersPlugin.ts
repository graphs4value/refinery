/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import type { Plugin } from '@docusaurus/types';

export default function loadersPlugin(): Plugin {
  return {
    name: 'refinery-loaders-plugin',
    configureWebpack(config) {
      let svgoDisabled = false;
      const rules = [...(config.module?.rules ?? [])];
      rules.forEach((rule) => {
        // Compare with
        // https://github.com/facebook/docusaurus/blob/73016d4936164ba38d4b86ec2aa8c168b5904a21/packages/docusaurus-utils/src/webpackUtils.ts#L128-L166
        if (
          typeof rule !== 'object' ||
          rule === null ||
          !('test' in rule) ||
          !(rule.test instanceof RegExp) ||
          !rule.test.test('.svg') ||
          !('oneOf' in rule)
        ) {
          return;
        }
        const {
          oneOf: [svgLoader],
        } = rule;
        if (
          typeof svgLoader !== 'object' ||
          svgLoader === null ||
          !('use' in svgLoader) ||
          typeof svgLoader.use !== 'object' ||
          svgLoader.use === null ||
          !(0 in svgLoader.use)
        ) {
          return;
        }
        const {
          use: [loader],
        } = svgLoader;
        if (
          typeof loader !== 'object' ||
          loader === null ||
          !('options' in loader)
        ) {
          return;
        }
        loader.options = {
          ...(typeof loader.options === 'object' ? loader.options : {}),
          // Disable SVGO, because it interferes styling figures exported from Refinery with CSS.
          svgo: false,
          svgoConfig: undefined,
        };
        svgoDisabled = true;
      });
      if (!svgoDisabled) {
        throw new Error('Failed to disable SVGO.');
      }
      return {
        mergeStrategy: {
          'module.rules': 'replace',
        },
        module: {
          rules,
        },
      };
    },
  };
}
