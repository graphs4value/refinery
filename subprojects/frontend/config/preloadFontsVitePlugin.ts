/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import micromatch from 'micromatch';
import type { PluginOption } from 'vite';

export default function preloadFontsVitePlugin(
  fontsGlob: string | string[],
): PluginOption {
  return {
    name: 'refinery-preload-fonts',
    apply: 'build',
    enforce: 'post',
    transformIndexHtml(_html, { bundle }) {
      return micromatch(Object.keys(bundle ?? {}), fontsGlob).map((href) => ({
        tag: 'link',
        attrs: {
          href,
          rel: 'preload',
          type: 'font/woff2',
          as: 'font',
          crossorigin: 'anonymous',
        },
      }));
    },
  };
}
