/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { execFile } from 'node:child_process';
import { readFile } from 'node:fs/promises';
import path from 'node:path';
import { promisify } from 'node:util';

import rawVersion from './version.mjs';

const execFileAsync = promisify(execFile);

/**
 * @param {string[]} args
 * @returns {Promise<string | undefined>}
 */
async function runGit(args) {
  try {
    const { stdout } = await execFileAsync('git', args, {
      cwd: import.meta.dirname,
      encoding: 'utf8',
    });
    return stdout.trim() || undefined;
  } catch {
    return undefined;
  }
}

/**
 * @returns {Promise<{
 *   branch: string | undefined;
 *   commitsSinceLatestTag: string | undefined;
 *   revision: string | undefined;
 * }>}
 */
async function getGitMetadata() {
  const revision = await runGit(['rev-parse', '--short=12', 'HEAD']);
  const latestTag = await runGit(['describe', '--tags', '--abbrev=0']);
  const commitCount = await runGit(
    latestTag
      ? ['rev-list', '--count', `${latestTag}..HEAD`]
      : ['rev-list', '--count', 'HEAD'],
  );
  const branchFromEnvironment = [
    process.env['GITHUB_HEAD_REF'],
    process.env['GITHUB_REF_NAME'],
  ].find((branch) => branch !== undefined && branch !== '');
  const branch =
    branchFromEnvironment ?? (await runGit(['branch', '--show-current']));
  return {
    branch,
    commitsSinceLatestTag: /^\d+$/.test(commitCount ?? '')
      ? commitCount
      : undefined,
    revision,
  };
}

/**
 * @param {string | undefined} branch
 * @param {string} fallback
 * @returns {string}
 */
function sanitizeBranch(branch, fallback) {
  const sanitized = (branch ?? fallback)
    .toLowerCase()
    .replace(/[^a-z0-9-]+/g, '-')
    .replace(/^-+|-+$/g, '');
  if (!sanitized) {
    return fallback;
  }
  return /^[0-9]/.test(sanitized) ? `branch-${sanitized}` : sanitized;
}

const snapshotSuffix = /-SNAPSHOT$/i;
const gitMetadata = snapshotSuffix.test(rawVersion)
  ? await getGitMetadata()
  : undefined;
const branch = sanitizeBranch(
  gitMetadata?.branch,
  gitMetadata?.revision ? 'detached' : 'local',
);
const versionSuffix = [
  gitMetadata?.commitsSinceLatestTag,
  branch,
  gitMetadata?.revision,
]
  .filter(Boolean)
  .join('.');
const version = snapshotSuffix.test(rawVersion)
  ? `${rawVersion.replace(snapshotSuffix, '')}-${versionSuffix}`
  : rawVersion;

/** @type {unknown} */
const packageJSON = JSON.parse(
  await readFile(
    path.join(import.meta.dirname, '../app/package.json'),
    'utf-8',
  ),
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

export default version;
