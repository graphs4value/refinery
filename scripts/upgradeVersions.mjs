/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { readFile } from 'node:fs/promises';
import path from 'node:path';

import * as cheerio from 'cheerio';
import TOML from 'smol-toml';

/**
 * @typedef VersionCatalog
 * @prop {Record<string, string>} [versions]
 * @prop {Record<string, Library>} [libraries]
 */

/**
 * @typedef Library
 * @prop {string} [group]
 * @prop {string} [name]
 * @prop {{ ref?: string}} [version]
 */

/**
 * @param {string} repo URI of the Maven repository to check.
 * @param {string} libraryName
 * @param {Library} library
 * @param {Record<string, string>} versions
 * @returns {Promise<void>}
 */
async function checkMavenUpdates(repo, libraryName, library, versions) {
  const { group, name, version: versionSpec } = library;
  if (group === undefined || name === undefined) {
    console.error('Malformed library dependency:', libraryName, library);
    return;
  }
  if (versionSpec === undefined) {
    // Dependency version managed by a referenced BOM.
    return;
  }
  const { ref } = versionSpec;
  if (ref === undefined) {
    console.error('Only named versions are support:', libraryName, library);
    return;
  }
  const version = versions[ref];
  if (version === undefined) {
    console.error('Missing version', ref, 'in dependency', libraryName);
    return;
  }
  const uri = `${repo}${group.replaceAll('.', '/')}/${name}/maven-metadata.xml`;
  const response = await fetch(uri);
  const $ = cheerio.load(await response.text(), { xml: true });
  const release = $('versioning > release').text();
  if (release !== version) {
    console.log(`Possible upgrade ${ref} = "${release}" from ${group}:${name}`);
  }
  const latest = $('versioning > latest').text();
  if (latest !== release) {
    console.log(
      `Possible pre-release upgrade ${ref} = "${latest}" from ${group}:${name}`,
    );
  }
}

/**
 * Check a `versions.toml` file for version reference updates.
 *
 * @param {string} repo URI of the Maven repository to check.
 * @param {string} fileName Name of the TOML file to check.
 * @returns {Promise<void>}
 */
async function checkForUpdates(repo, fileName) {
  console.log('# Upgrades in', fileName);
  const filePath = path.join(import.meta.dirname, '../gradle', fileName);
  const contents = await readFile(filePath, 'utf8');
  const toml = /** @type {VersionCatalog} */ (TOML.parse(contents));
  const versions = toml.versions ?? {};
  const libraries = toml.libraries ?? {};
  /* eslint-disable no-await-in-loop -- Throttle requests deliberately. */
  for (const libraryName of Object.getOwnPropertyNames(libraries)) {
    const library = libraries[libraryName] ?? {};
    await checkMavenUpdates(repo, libraryName, library, versions);
  }
  /* eslint-enable no-await-in-loop */
}

async function main() {
  await checkForUpdates(
    'https://repo1.maven.org/maven2/',
    'libs.versions.toml',
  );
  await checkForUpdates(
    'https://plugins.gradle.org/m2/',
    'pluginLibs.versions.toml',
  );
}

main().catch((error) => console.error(error));
