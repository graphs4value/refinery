/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 * Copyright (c) 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: MIT AND EPL-2.0
 */

import type { Options as PagesOptions } from '@docusaurus/plugin-content-pages';
import type { Options as ClassicThemeOptions } from '@docusaurus/theme-classic';
import type { UserThemeConfig } from '@docusaurus/theme-common';
import type { Config } from '@docusaurus/types';
import { Config as SwcConfig } from '@swc/core';
import smartypants from 'remark-smartypants';

const markdownOptions = {
  remarkPlugins: [[smartypants, { dashes: 'oldschool' }]],
};

export default {
  title: 'Refinery',
  url: 'https://refinery.tools',
  baseUrl: '/',
  baseUrlIssueBanner: false,
  trailingSlash: true,
  staticDirectories: ['static'],
  plugins: [
    [
      '@docusaurus/plugin-content-pages',
      markdownOptions satisfies PagesOptions,
    ],
    '@docusaurus/plugin-sitemap',
    './src/plugins/swcMinifyPlugin.ts',
  ],
  themes: [
    [
      '@docusaurus/theme-classic',
      {
        customCss: [require.resolve('./src/css/custom.css')],
      } satisfies ClassicThemeOptions,
    ],
  ],
  themeConfig: {
    colorMode: {
      respectPrefersColorScheme: true,
    },
    navbar: {
      title: 'Refinery',
      hideOnScroll: true,
    },
    footer: {
      copyright: `
        Copyright &copy; 2021-2024
        <a href="https://github.com/graphs4value/refinery/blob/main/CONTRIBUTORS.md" target="_blank">The Refinery Authors</a>.
        Available under the
        <a href="https://www.eclipse.org/legal/epl-2.0/" target="_blank">Eclipse Public License - v 2.0</a>.
      `,
    },
  } satisfies UserThemeConfig,
  webpack: {
    // Speed up builds by using a native Javascript loader.
    // See: https://github.com/facebook/docusaurus/issues/4765#issuecomment-841135926
    // But we follow the Docusaurus upstream from
    // https://github.com/facebook/docusaurus/blob/791da2e4a1a53aa6309887059e3f112fcb35bec4/website/docusaurus.config.ts#L152-L171
    // and use swc instead of esbuild.
    jsLoader: (isServer) => ({
      loader: require.resolve('swc-loader'),
      options: {
        jsc: {
          parser: {
            syntax: 'typescript',
            tsx: true,
          },
          transform: {
            react: {
              runtime: 'automatic',
            },
          },
          target: 'es2022',
        },
        module: {
          type: isServer ? 'commonjs' : 'es6',
        },
      } satisfies SwcConfig,
    }),
  },
} satisfies Config;
