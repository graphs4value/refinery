/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { chmod, mkdir, readFile, writeFile } from 'node:fs/promises';
import path from 'node:path';

import rawVersion from './scripts/version.mjs';

/** @type {unknown} */
const packageJSON = JSON.parse(
  await readFile(path.join(import.meta.dirname, 'app/package.json'), 'utf-8'),
);
if (
  typeof packageJSON !== 'object' ||
  packageJSON === null ||
  !('version' in packageJSON)
) {
  throw new Error('Missing "version" field in app/package.json');
} else if (
  typeof packageJSON.version !== 'string' ||
  packageJSON.version !== rawVersion.toLowerCase()
) {
  throw new Error(
    `Version in app/package.json ${JSON.stringify(packageJSON.version)} ` +
      `does not match gradle.properties ${JSON.stringify(rawVersion)}`,
  );
}

/** @type {import('electron-builder').Configuration} */
const config = {
  appId: 'tools.refinery.Refinery',
  productName: 'Refinery',
  protocols: [
    {
      name: 'Refinery link',
      schemes: ['refinery'],
    },
  ],
  fileAssociations: [
    {
      ext: ['problem', 'refinery'],
      name: 'Refinery file',
      description: 'Refinery problem file',
      mimeType: 'application/x-refinery',
      role: 'Editor',
    },
  ],
  directories: {
    app: 'app',
    output: 'build/dist',
    buildResources: 'build-resources',
  },
  linux: {
    target: ['AppImage'],
    category: 'Development',
  },
  win: {
    target: ['nsis'],
    extraFiles: [
      {
        from: 'build/launcher',
        to: 'bin',
        filter: ['refinery.exe'],
      },
    ],
  },
  nsis: {
    include: 'build-resources/installer.nsh',
  },
  mac: {
    target: ['dmg'],
    category: 'public.app-category.developer-tools',
    darkModeSupport: true,
  },
  appImage: {
    artifactName: '${productName}-${version}-${arch}.${ext}',
  },
  deb: {
    afterInstall: 'build-resources/after-install.tpl',
    afterRemove: 'build-resources/after-remove.tpl',
  },
  pacman: {
    afterInstall: 'build-resources/after-install.tpl',
    afterRemove: 'build-resources/after-remove.tpl',
    artifactName: '${name}-${version}-${arch}.pkg.tar.xz',
  },
  rpm: {
    afterInstall: 'build-resources/after-install.tpl',
    afterRemove: 'build-resources/after-remove.tpl',
  },
  npmRebuild: false,
  publish: null,
  toolsets: { appimage: '1.0.3' },
  files: [
    'package.json',
    {
      from: '../build/esbuild/production',
      to: '.',
      filter: '**/*',
    },
    {
      from: '../../frontend/build/vite/production',
      to: 'frontend',
      filter: ['**/*', '!**/*.br', '!**/*.gz'],
    },
    // For the workaround in `beforeBuild` to work, we explicitly need to exclude `node_modules`.
    '!node_modules/**/*',
  ],
  extraResources: [
    {
      from: 'build/backend',
      to: 'lib',
      filter: '**/*',
    },
    {
      from: 'build/jre',
      to: 'jre',
      filter: '**/*',
    },
  ],
  beforeBuild() {
    // Disable dependency installation. See
    // https://github.com/electron-userland/electron-builder/issues/10033#issuecomment-5078613647
    return false;
  },
  async afterPack(context) {
    const { appOutDir, electronPlatformName, packager, targets } = context;
    const app = packager.appInfo;
    const tpl = await readFile('build-resources/refinery-cli.sh.in', 'utf-8');

    const emit = async (
      /** @type {string} **/ dest,
      /** @type {Record<string, string>} */ vars,
    ) => {
      const out = tpl.replace(/@(\w+)@/g, (_, /** @type {string} */ k) => {
        if (k in vars) {
          return String(vars[k]);
        }
        throw new Error(`${dest}: unknown placeholder @${k}@`);
      });
      await mkdir(path.dirname(dest), { recursive: true });
      await writeFile(dest, out);
      await chmod(dest, 0o755);
    };

    if (electronPlatformName === 'darwin') {
      const contents = path.join(
        appOutDir,
        `${app.productFilename}.app`,
        'Contents',
      );
      await emit(path.join(contents, 'Resources/bin/refinery'), {
        REL_ROOT: '../..',
        REL_EXE: `MacOS/${app.productFilename}`,
        REL_CLI: 'Resources/app.asar/cli/index.cjs',
        APP_DIR: `/Applications/${app.productFilename}.app/Contents`,
      });
    }

    if (electronPlatformName === 'linux') {
      await emit(path.join(appOutDir, 'bin/refinery'), {
        REL_ROOT: '..',
        REL_EXE: app.name,
        REL_CLI: 'resources/app.asar/cli/index.cjs',
        APP_DIR: `/opt/${app.sanitizedProductName}`,
      });

      // electron-builder creates AppRun in its temporary AppImage staging
      // directory before copying the packed application into it. Providing an
      // AppRun in appOutDir therefore replaces the default launcher in the
      // AppImage while leaving the other Linux targets unchanged in behavior.
      if (targets.some(({ name }) => name === 'appImage')) {
        const appRun = path.join(appOutDir, 'AppRun');
        await writeFile(
          appRun,
          await readFile('build-resources/refinery-app-run.sh', 'utf-8'),
        );
        await chmod(appRun, 0o755);
      }
    }
  },
};

export default config;
