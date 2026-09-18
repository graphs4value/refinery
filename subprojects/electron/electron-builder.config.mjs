/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { chmod, mkdir, readFile, writeFile } from 'node:fs/promises';
import path from 'node:path';

import version from './scripts/gitVersion.mjs';

const isCI = process.env['CI'] === 'true';
const packageRevision = [
  process.env['BUILD_NUMBER'],
  process.env['TRAVIS_BUILD_NUMBER'],
  process.env['APPVEYOR_BUILD_NUMBER'],
  process.env['CIRCLE_BUILD_NUM'],
  process.env['BUILD_BUILDNUMBER'],
  process.env['CI_PIPELINE_IID'],
].find((value) => value !== undefined && value !== '');

const packageIteration = packageRevision?.replaceAll('-', '_');
const fpmArtifactVersion = version.replaceAll('-', '~');
const pacmanArtifactVersion = version.replaceAll('-', '_');
const fpmArtifactRevision = packageIteration ?? '1';

/** @type {Record<string, string>} */
const linuxArchitectureNames = {
  x64: 'x86_64',
  arm64: 'aarch64',
};
const linuxArtifactArchitecture =
  linuxArchitectureNames[process.arch] ?? process.arch;
const debArtifactArchitecture = process.arch === 'x64' ? 'amd64' : process.arch;

const appImageArtifactName =
  '${productName}-${version}-' + linuxArtifactArchitecture + '.${ext}';
const debArtifactName =
  '${name}_' +
  fpmArtifactVersion +
  (packageIteration === undefined ? '' : `-${packageIteration}`) +
  `_${debArtifactArchitecture}.deb`;
const pacmanArtifactName =
  '${name}-' +
  pacmanArtifactVersion +
  '-' +
  fpmArtifactRevision +
  `-${linuxArtifactArchitecture}.pkg.tar.xz`;
const rpmArtifactName =
  '${name}-' +
  fpmArtifactVersion +
  `-${fpmArtifactRevision}.${linuxArtifactArchitecture}.rpm`;
const windowsArtifactName =
  '${productName} Setup ${version}-' + process.arch + '.${ext}';
const macArtifactName = '${productName}-${version}-' + process.arch + '.${ext}';

/** @type {import('electron-builder').Configuration} */
const config = {
  extraMetadata: {
    version,
  },
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
    target: ['AppImage', ...(isCI ? ['deb', 'rpm', 'pacman'] : [])],
    category: 'Development',
    icon: 'icons',
  },
  win: {
    target: ['nsis'],
    icon: 'icons/icon.ico',
    extraFiles: [
      {
        from: 'build/launcher',
        to: 'bin',
        filter: ['refinery.exe'],
      },
    ],
  },
  nsis: {
    artifactName: windowsArtifactName,
    include: 'build-resources/installer.nsh',
  },
  mac: {
    target: ['dmg'],
    category: 'public.app-category.developer-tools',
    darkModeSupport: true,
    icon: 'icons/icon.icns',
  },
  dmg: {
    artifactName: macArtifactName,
  },
  appImage: {
    artifactName: appImageArtifactName,
  },
  deb: {
    afterInstall: 'build-resources/after-install.tpl',
    afterRemove: 'build-resources/after-remove.tpl',
    artifactName: debArtifactName,
  },
  pacman: {
    afterInstall: 'build-resources/after-install.tpl',
    afterRemove: 'build-resources/after-remove.tpl',
    artifactName: pacmanArtifactName,
  },
  rpm: {
    afterInstall: 'build-resources/after-install.tpl',
    afterRemove: 'build-resources/after-remove.tpl',
    artifactName: rpmArtifactName,
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
