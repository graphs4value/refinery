/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { minify, type Options as TerserOptions } from 'html-minifier-terser';
import type { PluginOption } from 'vite';

export default function minifyHTMLVitePlugin(
  options?: TerserOptions | undefined,
): PluginOption {
  return {
    name: 'minify-html',
    apply: 'build',
    enforce: 'post',
    transformIndexHtml(html) {
      return minify(html, {
        collapseWhitespace: true,
        collapseBooleanAttributes: true,
        minifyCSS: true,
        removeComments: true,
        removeAttributeQuotes: true,
        removeRedundantAttributes: true,
        sortAttributes: true,
        ...(options ?? {}),
      });
    },
  };
}
