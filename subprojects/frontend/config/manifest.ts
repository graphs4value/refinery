/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import type { ManifestOptions } from 'vite-plugin-pwa';

const manifest: Partial<ManifestOptions> = {
  lang: 'en-US',
  name: 'Refinery',
  short_name: 'Refinery',
  description: 'An efficient graph solver for generating well-formed models',
  theme_color: '#f5f5f5',
  display_override: ['window-controls-overlay'],
  display: 'standalone',
  background_color: '#21252b',
  icons: [
    {
      src: 'icon-192x192.png',
      sizes: '192x192',
      type: 'image/png',
      purpose: 'any maskable',
    },
    {
      src: 'icon-512x512.png',
      sizes: '512x512',
      type: 'image/png',
      purpose: 'any maskable',
    },
    {
      src: 'icon-any.svg',
      sizes: 'any',
      type: 'image/svg+xml',
      purpose: 'any maskable',
    },
    {
      src: 'mask-icon.svg',
      sizes: 'any',
      type: 'image/svg+xml',
      purpose: 'monochrome',
    },
  ],
};

export default manifest;
