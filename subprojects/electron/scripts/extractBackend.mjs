/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { createWriteStream } from 'node:fs';
import { rm, mkdir, unlink, utimes } from 'node:fs/promises';
import path from 'node:path';
import { pipeline } from 'stream/promises';

import { extract } from 'tar';
import yauzl from 'yauzl';

import version from './version.mjs';
import writeManifestJar from './writeManifestJar.mjs';

const targetDir = path.join(import.meta.dirname, '../build/backend');

const platforms = /** @type {const} */ (['darwin', 'linux', 'win32']);
const arches = /** @type {const} */ (['aarch64', 'x86-64']);
/** @type {`${platforms[number]}-${arches[number]}`[]} */
const targets = [];
for (const platform of platforms) {
  for (const arch of arches) {
    targets.push(`${platform}-${arch}`);
  }
}

/** @type {platforms[number]} */
let currentPlatform;
switch (process.platform) {
  case 'darwin':
  case 'linux':
  case 'win32':
    currentPlatform = process.platform;
    break;
  default:
    throw new Error(`Unsupported platform: ${process.platform}`);
}
/** @type {arches[number]} */
let currentArch;
switch (process.arch) {
  case 'arm64':
    currentArch = 'aarch64';
    break;
  case 'x64':
    currentArch = 'x86-64';
    break;
  default:
    throw new Error(`Unsupported architecture: ${process.arch}`);
}
/** @type {targets[number]} */
const currentTarget = `${currentPlatform}-${currentArch}`;
const currentTargetSuffix = `-${currentTarget}`;

const otherTargets = targets.filter((target) => target !== currentTarget);
const otherTargetsRegExp = new RegExp(
  otherTargets.map((target) => RegExp.escape(target)).join('|'),
);

/**
 * Extract jars from a Gradle distribution tar.
 *
 * @param {string} name
 * @returns {Promise<[string[], string[]]>}
 */
async function extractDistTar(name) {
  const distTar = path.join(
    import.meta.dirname,
    `../../${name}/build/distributions/refinery-${name}-${version}.tar`,
  );
  /** @type {string[]} */
  const libs = [];
  /** @type {string[]} */
  const natives = [];
  await extract({
    file: distTar,
    'keep-existing': true,
    strip: 2,
    cwd: targetDir,
    filter(filePath) {
      const dirName = path.basename(path.dirname(filePath));
      const fileName = path.basename(filePath);
      const matches = dirName === 'lib' && !otherTargetsRegExp.test(fileName);
      if (matches && /\.jar$/i.test(fileName)) {
        if (fileName.indexOf(currentArch) > 0) {
          natives.push(fileName);
        } else {
          libs.push(fileName);
        }
      }
      return matches;
    },
  });
  return [libs.toSorted(), natives];
}

/**
 * @param {string} name
 * @param {Iterable<string>} entries
 * @returns {Promise<void>}
 */
function writePathingJar(name, entries) {
  return writeManifestJar(path.join(targetDir, `${name}.jar`), entries, {
    'Bundle-SymbolicName': `tools.refinery.${name}`,
    'Bundle-Version': version,
  });
}

/**
 * Extracts native dependencies from a `com.google.ortools.Loader` style per-platform jar.
 *
 * In such a jar, usually called `<library>-<platform>-<arch>-<version>.jar`,
 * the `<library>-<platform>-<arch>/` directory in the root of the jar contains the native
 * libraries used by `<library>-<version>.jar`. We extract the libraries into the `natives/`
 * directory, which will be set as the value of the
 *
 *   + the `java.library.path` Java property to ensure that `System.loadLibrary` can find them;
 *   + the `LD_LIBRARY_PATH`, so that the dynamic linker can find them when looking for
 *     dynamically linked dependencies; and
 *   + an element of `PATH`, so that the Windows dynamic linker can also find them.
 *
 * This mechanism is adopted from ORTools by the native libraries packaged by Refinery,
 * so we should end up with all the natives in `native/` and can safely remove the jars
 * thar originally contained them. This lets us avoid extracting the depndencies from the
 * jars into a temporary directory every time they are accessed.
 *
 * @param {string} nativesDir}
 * @param {string} jar
 * @returns {Promise<void>}
 */
async function extractNativeJar(nativesDir, jar) {
  const jarPath = path.join(targetDir, jar);
  const zipFile = await yauzl.openPromise(jarPath);
  for await (const entry of zipFile.eachEntry()) {
    const { fileName } = entry;
    if (fileName.endsWith('/')) {
      continue;
    }
    const components = fileName.split('/');
    if (components.length !== 2) {
      continue;
    }
    const [dirName, baseName] = components;
    if (
      !dirName ||
      !dirName.endsWith(currentTargetSuffix) ||
      !baseName ||
      baseName === ''
    ) {
      continue;
    }
    const extractPath = path.join(nativesDir, baseName);
    const readStream = await zipFile.openReadStreamPromise(entry);
    await pipeline(
      readStream,
      createWriteStream(extractPath, {
        mode: 0o755,
      }),
    );
    const lastModDate = entry.getLastModDate();
    await utimes(extractPath, lastModDate, lastModDate);
  }
  await unlink(jarPath);
}

await rm(targetDir, { recursive: true, force: true });
await mkdir(targetDir, { recursive: true });

// Extract sequentially so that shared files in the tars don't conflic with each other.
const [webLibs, webNatives] = await extractDistTar('language-web');
const [cliLibs, cliNatives] = await extractDistTar('generator-cli');
const natives = Array.from(new Set([...webNatives, ...cliNatives])).toSorted();
const nativesDir = path.join(targetDir, 'native');
await mkdir(nativesDir);

await Promise.all([
  writePathingJar('refinery-language-web-all', webLibs),
  writePathingJar('refinery-generator-cli-all', cliLibs),
  ...natives.map((jar) => extractNativeJar(nativesDir, jar)),
]);
