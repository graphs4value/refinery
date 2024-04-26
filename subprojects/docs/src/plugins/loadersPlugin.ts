/*
 * Copyright (c) 2016, Jeremy Stucki
 * Copyright (c) Facebook, Inc. and its affiliates.
 * Copyright (c) 2024 The Refinery Authors
 *
 * SPDX-License-Identifier: BSD-3-Clause AND MIT AND EPL-2.0
 */

import type { Plugin } from '@docusaurus/types';
// @ts-expect-error No typings available for `responsive-loader`.
import sharp from 'responsive-loader/sharp';

export default function loadersPlugin(): Plugin {
  return {
    name: 'refinery-loaders-plugin',
    configureWebpack(config, isServer) {
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
        // Skip SVGR when importing SVG files with ?url.
        svgLoader.resourceQuery = { not: /[?&]url$/ };
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
          svgo: true,
          svgoConfig: {
            plugins: [
              {
                name: 'preset-default',
                params: {
                  overrides: {
                    removeTitle: false,
                    removeViewBox: false,
                    // Disable SVGO, because it interferes styling figures exported from Refinery with CSS.
                    inlineStyles: false,
                    cleanupIds: false,
                  },
                },
              },
            ],
          },
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
          rules: [
            // Configuration based on
            // https://github.com/dazuaz/responsive-loader/blob/ef2c806fcd36f06f6be8a0b97e09f40c3d86d3ac/README.md
            {
              test: /\.(png|jpe?g)$/,
              resourceQuery: /[?&]rl$/,
              use: [
                {
                  loader: 'responsive-loader',
                  options: {
                    /* eslint-disable-next-line @typescript-eslint/no-unsafe-assignment --
                     * No typings available for `responsive-loader`.
                     */
                    adapter: sharp,
                    format: 'webp',
                    // See
                    // https://github.com/facebook/docusaurus/blob/c745021b01a8b88d34e1d772278d7171ad8acdf5/packages/docusaurus-plugin-ideal-image/src/index.ts#L62-L66
                    emitFile: !isServer,
                    name: 'assets/images/[name].[hash:hex:7].[width].[ext]',
                  },
                },
              ],
            },
            ...rules,
          ],
        },
      };
    },
  };
}
