/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 * Copyright (c) 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: MIT AND EPL-2.0
 */

import type { MDXOptions } from '@docusaurus/mdx-loader';
import type { Options as DocsOptions } from '@docusaurus/plugin-content-docs';
import type { Options as PagesOptions } from '@docusaurus/plugin-content-pages';
import type { Options as ClassicThemeOptions } from '@docusaurus/theme-classic';
import type { UserThemeConfig } from '@docusaurus/theme-common';
import type { UserThemeConfig as AlgoliaConfig } from '@docusaurus/theme-search-algolia';
import type { Config } from '@docusaurus/types';
import { Config as SwcConfig } from '@swc/core';
import { themes } from 'prism-react-renderer';
import smartypants from 'remark-smartypants';

import remarkPosix2Windows from './src/plugins/remarkPosix2Windows';

const markdownOptions: Partial<MDXOptions> = {
  remarkPlugins: [[smartypants, { dashes: 'oldschool' }], remarkPosix2Windows],
};

const docsOptions = {
  ...markdownOptions,
  sidebarPath: undefined,
  editUrl:
    'https://github.com/graphs4value/refinery/edit/main/subprojects/docs',
} satisfies DocsOptions;

export default {
  title: 'Refinery',
  tagline: 'An efficient graph solver for generating well-formed models',
  url: 'https://refinery.tools',
  baseUrl: '/',
  baseUrlIssueBanner: false,
  trailingSlash: true,
  staticDirectories: ['static', 'build/javadocs'],
  plugins: [
    [
      '@docusaurus/plugin-content-docs',
      {
        id: 'learn',
        path: 'src/learn',
        routeBasePath: '/learn',
        ...docsOptions,
      } satisfies DocsOptions,
    ],
    [
      '@docusaurus/plugin-content-docs',
      {
        id: 'develop',
        path: 'src/develop',
        routeBasePath: '/develop',
        ...docsOptions,
      } satisfies DocsOptions,
    ],
    [
      '@docusaurus/plugin-content-pages',
      markdownOptions satisfies PagesOptions,
    ],
    '@docusaurus/plugin-sitemap',
    './src/plugins/loadersPlugin.ts',
    './src/plugins/swcMinifyPlugin.ts',
  ],
  themes: [
    [
      '@docusaurus/theme-classic',
      {
        customCss: [require.resolve('./src/css/custom.css')],
      } satisfies ClassicThemeOptions,
    ],
    '@docusaurus/theme-search-algolia',
  ],
  themeConfig: {
    colorMode: {
      respectPrefersColorScheme: true,
    },
    prism: {
      additionalLanguages: ['bash', 'java'],
      theme: themes.oneLight,
      darkTheme: themes.oneDark,
    },
    navbar: {
      title: 'Refinery',
      logo: {
        src: '/logo.svg',
        srcDark: '/logo-dark.svg',
      },
      hideOnScroll: true,
      items: [
        {
          label: 'Learn',
          to: '/learn',
        },
        {
          label: 'Develop',
          to: '/develop',
        },
        {
          label: 'GitHub',
          position: 'right',
          href: 'https://github.com/graphs4value/refinery',
        },
        {
          label: 'Try now',
          position: 'right',
          href: 'https://refinery.services/',
          className: 'navbar__link--try-now',
        },
      ],
    },
    footer: {
      links: [
        {
          title: 'Learn',
          items: [
            {
              label: 'Introduction',
              to: '/learn',
            },
            {
              label: 'Tutorials',
              to: '/learn/tutorials',
            },
            {
              label: 'Langauge reference',
              to: '/learn/language',
            },
            {
              label: 'Run in Docker',
              to: '/learn/docker',
            },
          ],
        },
        {
          title: 'Develop',
          items: [
            {
              label: 'Programming guide',
              to: '/develop',
            },
            {
              label: 'Contributing',
              to: '/develop/contributing',
            },
            {
              label: 'Javadoc',
              to: '/develop/javadoc',
            },
          ],
        },
        {
          title: 'More',
          items: [
            {
              label: 'Try now',
              href: 'https://refinery.services/',
            },
            {
              label: 'GitHub',
              href: 'https://github.com/graphs4value/refinery',
            },
            {
              label: 'License',
              to: '/license',
            },
          ],
        },
        {
          title: 'Supporters',
          items: [
            {
              label: 'BME MIT FTSRG',
              href: 'https://ftsrg.mit.bme.hu/en/',
            },
            {
              label: 'McGill ECE',
              href: 'https://www.mcgill.ca/',
            },
            {
              label: '2022 Amazon Research Awards',
              href: 'https://www.amazon.science/research-awards/recipients/daniel-varro-fall-2021',
            },
            {
              label: 'LiU Software and Systems',
              href: 'https://liu.se/en/organisation/liu/ida/sas',
            },
            {
              label: 'WASP',
              href: 'https://wasp-sweden.org/',
            },
          ],
        },
      ],
      copyright: `
        Copyright &copy; 2021-2024
        <a href="https://github.com/graphs4value/refinery/blob/main/CONTRIBUTORS.md#the-refinery-authors" target="_blank">The Refinery Authors</a>.
        Available under the
        <a href="/license/">Eclipse Public License - v 2.0</a>.
      `,
    },
    algolia: {
      appId: 'KYHOYEO80F',
      apiKey: '152acfb8d1ad9e10f29f083a6b017a69',
      indexName: 'refinery',
    },
  } satisfies UserThemeConfig & AlgoliaConfig,
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
  headTags: [
    {
      tagName: 'link',
      attributes: {
        rel: 'icon',
        href: '/favicon.svg',
        type: 'image/svg+xml',
      },
    },
    {
      tagName: 'link',
      attributes: {
        rel: 'icon',
        href: '/favicon.png',
        type: 'image/png',
        sizes: '32x32',
      },
    },
    {
      tagName: 'link',
      attributes: {
        rel: 'icon',
        href: '/favicon-96x96.png',
        type: 'image/png',
        sizes: '96x96',
      },
    },
    {
      tagName: 'link',
      attributes: {
        rel: 'apple-touch-icon',
        href: '/apple-touch-icon.png',
        type: 'image/png',
        sizes: '180x180',
      },
    },
    {
      tagName: 'link',
      attributes: {
        rel: 'manifest',
        href: '/manifest.webmanifest',
      },
    },
    {
      tagName: 'meta',
      attributes: {
        name: 'theme-color',
        media: '(prefers-color-scheme:light)',
        content: '#f5f5f5',
      },
    },
    {
      tagName: 'meta',
      attributes: {
        name: 'theme-color',
        media: '(prefers-color-scheme:dark)',
        content: '#282c34',
      },
    },
  ],
} satisfies Config;
