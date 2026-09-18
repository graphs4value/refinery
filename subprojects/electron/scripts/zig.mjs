/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

/*
 * Provisions a Zig toolchain into `build/zig` on demand and exposes helpers for
 * driving it. Zig bundles the MinGW-w64 headers and import libraries, so
 * `zig cc` targeting the `-gnu` ABI can build Win32 code with no Visual Studio
 * or Windows SDK on the machine. This keeps the "only a JDK is required; the
 * build downloads the rest" property intact, and it cross-compiles from
 * Linux/macOS/Windows so CI can emit the Windows binary from any host.
 */

import { spawn } from 'node:child_process';
import { createHash } from 'node:crypto';
import { createWriteStream } from 'node:fs';
import { chmod, mkdir, rm, stat } from 'node:fs/promises';
import path from 'node:path';
import { Readable, Transform } from 'node:stream';
import { pipeline } from 'node:stream/promises';

import yauzl from 'yauzl';

const root = path.join(import.meta.dirname, '..');
const zigCacheRoot = path.join(root, 'build', 'zig');

/** Zig toolchain used to cross-compile the launcher. */
export const ZIG_VERSION = '0.16.0';

/**
 * SHA-256 of the official Zig release archives, keyed by `${arch}-${os}`.
 * Copy the values for the hosts your builds run on from the signed release
 * index at https://ziglang.org/download/. Only the entry for
 * the current build host is required.
 *
 * @type {Record<string, string>}
 */
const ZIG_SHA256 = {
  'aarch64-linux':
    'ea4b09bfb22ec6f6c6ceac57ab63efb6b46e17ab08d21f69f3a48b38e1534f17',
  'aarch64-macos':
    'b23d70deaa879b5c2d486ed3316f7eaa53e84acf6fc9cc747de152450d401489',
  'x86_64-linux':
    '70e49664a74374b48b51e6f3fdfbf437f6395d42509050588bd49abe52ba3d00',
  'x86_64-macos':
    '0387557ed1877bc6a2e1802c8391953baddba76081876301c522f52977b52ba7',
  'x86_64-windows':
    '68659eb5f1e4eb1437a722f1dd889c5a322c9954607f5edcf337bc3684a75a7e',
};

/** @type {Record<string, string>} */
const ARCHES = { x64: 'x86_64', arm64: 'aarch64' };
/** @type {Record<string, string>} */
const OSES = { linux: 'linux', darwin: 'macos', win32: 'windows' };

const hostArch = ARCHES[process.arch];
const hostOs = OSES[process.platform];
if (!hostArch || !hostOs) {
  throw new Error(
    `Unsupported build host: ${process.platform}/${process.arch}`,
  );
}

const hostKey = `${hostArch}-${hostOs}`;
const archiveBase = `zig-${hostArch}-${hostOs}-${ZIG_VERSION}`;
const archiveExt = hostOs === 'windows' ? 'zip' : 'tar.xz';
const archiveName = `${archiveBase}.${archiveExt}`;
const downloadUrl = `https://ziglang.org/download/${ZIG_VERSION}/${archiveName}`;

const zigHome = path.join(zigCacheRoot, archiveBase);

/** Absolute path to the provisioned `zig` executable. */
export const zigExe = path.join(
  zigHome,
  hostOs === 'windows' ? 'zig.exe' : 'zig',
);

/**
 * Spawn a command, inheriting stdio, and resolve when it exits successfully.
 *
 * @param {string} command
 * @param {string[]} args
 * @returns {Promise<void>}
 */
export function run(command, args) {
  return new Promise((resolve, reject) => {
    const child = spawn(command, args, { stdio: 'inherit' });
    child.on('error', reject);
    child.on('exit', (code, signal) => {
      if (signal) {
        reject(new Error(`${command} killed by ${signal}`));
      } else if (code !== 0) {
        reject(new Error(`${command} exited with code ${code}`));
      } else {
        resolve();
      }
    });
  });
}

/**
 * @param {string} file
 * @returns {Promise<boolean>}
 */
async function exists(file) {
  try {
    await stat(file);
    return true;
  } catch {
    return false;
  }
}

/**
 * Download the Zig archive, verifying its SHA-256.
 *
 * @param {string} dest
 * @returns {Promise<void>}
 */
async function download(dest) {
  const expected = ZIG_SHA256[hostKey];
  if (!expected) {
    throw new Error(
      `No SHA-256 pinned for Zig ${ZIG_VERSION} on ${hostKey}. ` +
        `Add it to ZIG_SHA256 from https://ziglang.org/download/`,
    );
  }
  console.log(`Downloading ${downloadUrl}`);
  const response = await fetch(downloadUrl);
  if (!response.ok || !response.body) {
    throw new Error(`Failed to download Zig: HTTP ${response.status}`);
  }
  const hash = createHash('sha256');
  const tap = new Transform({
    transform(
      /** @type {import('node:crypto').BinaryLike} */ chunk,
      _enc,
      callback,
    ) {
      hash.update(chunk);
      callback(null, chunk);
    },
  });
  await pipeline(Readable.fromWeb(response.body), tap, createWriteStream(dest));
  const actual = hash.digest('hex');
  if (actual !== expected) {
    await rm(dest, { force: true });
    throw new Error(
      `Zig checksum mismatch for ${archiveName}:\n` +
        `  expected ${expected}\n  actual   ${actual}`,
    );
  }
}

/**
 * Extract a `.zip` archive (Windows hosts).
 *
 * @param {string} archive
 * @param {string} dest
 * @returns {Promise<void>}
 */
async function extractZip(archive, dest) {
  const zipFile = await yauzl.openPromise(archive);
  for await (const entry of zipFile.eachEntry()) {
    const { fileName } = entry;
    if (fileName.endsWith('/')) {
      continue;
    }
    const target = path.join(dest, fileName);
    await mkdir(path.dirname(target), { recursive: true });
    const readStream = await zipFile.openReadStreamPromise(entry);
    await pipeline(readStream, createWriteStream(target));
  }
}

/**
 * Provision the Zig toolchain into `zigHome` (cached across builds).
 *
 * @returns {Promise<void>}
 */
export async function ensureZig() {
  if (await exists(zigExe)) {
    return;
  }
  await rm(zigHome, { recursive: true, force: true });
  await mkdir(zigCacheRoot, { recursive: true });
  const archive = path.join(zigCacheRoot, archiveName);
  await download(archive);

  if (archiveExt === 'zip') {
    // The zip contains a top-level `${archiveBase}/` directory.
    await extractZip(archive, zigCacheRoot);
  } else {
    // System tar (GNU tar / bsdtar) autodetects xz; no npm xz dependency.
    await run('tar', ['-xf', archive, '-C', zigCacheRoot]);
    await chmod(zigExe, 0o755);
  }
  await rm(archive, { force: true });

  if (!(await exists(zigExe))) {
    throw new Error(`Zig binary not found after extraction: ${zigExe}`);
  }
}
