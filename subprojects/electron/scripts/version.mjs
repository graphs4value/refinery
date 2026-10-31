/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import path from 'node:path';

import { PropertiesFile } from 'java-properties';

const properties = new PropertiesFile(
  path.join(import.meta.dirname, '../../../gradle.properties'),
);

const rawVersion = properties.get('version');
if (typeof rawVersion !== 'string') {
  throw new Error('Invalid or no version in gradle.properties');
}

/** @type {string} */
const version = rawVersion;

export default version;
