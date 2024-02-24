/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { readFile } from 'node:fs/promises';
import path from 'node:path';

import pnpapi from 'pnpapi';
import type { PluginOption, ResolvedConfig } from 'vite';

// Use a CJS file as the PnP resolution issuer to force resolution to a non-ESM export.
const issuerFileName = 'worker.cjs';

export default function graphvizUMDVitePlugin(): PluginOption {
  let command: ResolvedConfig['command'] = 'build';
  let root: string | undefined;
  let url: string | undefined;

  return {
    name: 'graphviz-umd',
    enforce: 'post',
    configResolved(config) {
      ({ command, root } = config);
    },
    async buildStart() {
      const issuer =
        root === undefined ? issuerFileName : path.join(root, issuerFileName);
      // Since https://github.com/hpcc-systems/hpcc-js-wasm/commit/15e1ace5edae7f94714e547a3ac20e0e17cd6b0c,
      // hpcc-js has both a `.cjs` and a `.umd.js` build. PnPAPI will find the former, but we need the latter.
      const resolvedPath = pnpapi
        .resolveRequest('@hpcc-js/wasm/graphviz', issuer)
        ?.replace(/\.cjs$/, '.umd.js');
      if (resolvedPath === undefined) {
        return;
      }
      if (command === 'serve') {
        url = `/@fs/${resolvedPath}`;
      } else {
        const content = await readFile(resolvedPath, null);
        url = this.emitFile({
          name: path.basename(resolvedPath),
          type: 'asset',
          source: content,
        });
      }
    },
    renderStart() {
      if (url !== undefined && command !== 'serve') {
        url = this.getFileName(url);
      }
    },
    transformIndexHtml() {
      if (url === undefined) {
        return undefined;
      }
      return [
        {
          tag: 'script',
          attrs: {
            src: url,
            type: 'javascript/worker',
          },
          injectTo: 'head',
        },
      ];
    },
  };
}
