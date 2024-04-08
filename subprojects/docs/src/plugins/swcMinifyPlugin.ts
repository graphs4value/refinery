/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import type { Plugin } from '@docusaurus/types';
import TerserPlugin from 'terser-webpack-plugin';

/**
 * A Docusarus plugin that replaces the built-in Javascript minifier with swc.
 *
 * See
 * https://github.com/facebook/docusaurus/issues/4765#issuecomment-1679863984
 * but we use swc instead of esbuild.
 *
 * @returns The Docusarus plugin.
 */
export default function swcMinifyPlugin(): Plugin {
  return {
    name: 'refinery-swc-minify-plugin',
    configureWebpack: (config) => ({
      mergeStrategy: {
        'optimization.minimizer': 'replace',
      },
      optimization: {
        minimizer:
          config.optimization?.minimizer?.map((plugin) => {
            // `instanceof` seems to be broken, because a different version of
            // `TerserPlguin` is coming from Docusaurus than the one we import.
            if (plugin?.constructor.name === TerserPlugin.name) {
              return new TerserPlugin({
                minify: TerserPlugin.swcMinify,
              });
            }
            return plugin;
          }) ?? [],
      },
    }),
  };
}
