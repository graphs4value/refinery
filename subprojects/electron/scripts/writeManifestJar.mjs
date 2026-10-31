/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { createWriteStream } from 'node:fs';

import { ZipFile } from 'yazl';

/**
 * The jar manifest spec limits every physical line to 72 bytes. A logical
 * value longer than that continues on the next line, which must begin with
 * exactly one space; the continuation therefore carries at most 71 bytes of
 * payload. The limit is on UTF-8 *bytes*, and a multi-byte character must not
 * be split across the boundary.
 *
 * We sidestep the split-character hazard entirely by percent-encoding every
 * Class-Path entry (see toClassPathEntry), which makes the value pure ASCII so
 * one byte == one character. This wrapper still operates on bytes so it stays
 * correct even if a caller passes a non-ASCII header value elsewhere: it
 * advances by whole UTF-8 sequences and never exceeds the byte budget.
 *
 * @param {string} name The manifest header name.
 * @param {string} value The manifest header value.
 * @returns {string} The linewrapped manifest header.
 */
function wrapManifestLine(name, value) {
  const enc = new TextEncoder();
  const lines = [];
  let line = '';
  let bytes = 0;
  let budget = 72; // first line; continuations get 71

  for (const cp of `${name}: ${value}`) {
    const w = enc.encode(cp).length;
    if (bytes + w > budget) {
      lines.push(line);
      line = '';
      bytes = 0;
      budget = 71;
    }
    line += cp;
    bytes += w;
  }
  if (line) lines.push(line);

  return lines.map((l, i) => (i === 0 ? l : ' ' + l)).join('\r\n');
}

/**
 * Class-Path entries are URLs relative to the pathing jar. Percent-encode each
 * path segment so spaces, non-ASCII, and reserved characters survive, and keep
 * '/' as the separator (jar URLs use '/' on every OS, including Windows).
 * Directory entries must keep a trailing slash or the loader treats them as a
 * jar file.
 *
 * @param {string} relPath The relative path of the JAR to add to the classpath.
 * @return {string} The escaped classpath entry.
 */
function toClassPathEntry(relPath) {
  const isDir = /[\\/]$/.test(relPath);
  const parts = relPath.replace(/\\/g, '/').replace(/\/+$/, '').split('/');
  const encoded = parts.map((p) => encodeURIComponent(p)).join('/');
  return isDir ? encoded + '/' : encoded;
}

/**
 * Assemble the full manifest text. `entries` are paths relative to where the
 * pathing jar will sit at runtime, e.g. "lib/foo.jar" or "plugins/".
 * Order is preserved and is the classpath precedence order — sort upstream.
 *
 * @param {string[]} entries The classpath entires to write.
 * @param {Record<string, string>} extraHeaders Extra headers to write to the JAR manifest.
 * @returns {string} The complete manifest.
 */
function buildManifest(entries, extraHeaders = {}) {
  if (!Array.isArray(entries) || entries.length === 0) {
    throw new Error('pathing jar needs at least one Class-Path entry');
  }

  const classPath = entries.map(toClassPathEntry).join(' ');

  // Manifest-Version MUST be first. Class-Path is the payload. Callers can add
  // e.g. Created-By or a Main-Class, though a pathing jar usually has neither.
  /** @type {[string, string][]} */
  const headers = [
    ['Manifest-Version', '1.0'],
    ...Object.entries(extraHeaders),
    ['Class-Path', classPath],
  ];

  // Each header wrapped independently; blocks joined by CRLF; file ends CRLF.
  return headers.map(([k, v]) => wrapManifestLine(k, v)).join('\r\n') + '\r\n';
}

/**
 * Write the pathing jar. The manifest is STORED (not deflated): it is tiny, so
 * compression saves nothing and only adds a failure surface, and a stored jar
 * stays inspectable with `unzip -p`. yazl handles local headers, the central
 * directory, CRC-32, and offsets — the parts hand-rolled zip writers botch.
 *
 * A fixed mtime keeps the output byte-reproducible across builds, which is
 * what lets a content-addressed or cached build treat an unchanged jar as
 * unchanged.
 *
 * @param {string} outPath The output file path.
 * @param {Iterable<string>} entries The classpath entires to write.
 * @param {Record<string, string>} extraHeaders Extra headers to write to the JAR manifest.
 * @returns {Promise<void>} Resolves when the jar was successfully written.
 */
export default function writePathingJar(outPath, entries, extraHeaders = {}) {
  const manifest = buildManifest(Array.from(entries).toSorted(), extraHeaders);

  return new Promise((resolve, reject) => {
    const zip = new ZipFile();
    const out = createWriteStream(outPath);

    out.on('close', () => resolve());
    out.on('error', reject);
    zip.outputStream.on('error', reject);

    zip.addBuffer(Buffer.from(manifest, 'latin1'), 'META-INF/MANIFEST.MF', {
      compress: false,
      mtime: new Date(0),
      mode: 0o644,
    });

    zip.outputStream.pipe(out);
    zip.end();
  });
}
