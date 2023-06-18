/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

const { writeFile } = require('node:fs/promises');
const path = require('node:path');
const { Readable } = require('node:stream');
const { pipeline } = require('node:stream/promises');

const { ESLint } = require('eslint');

const rootDir = path.join(__dirname, '..');

/**
 * Write ESLint report to console.
 *
 * @param cli {import('eslint').ESLint} The ESLint CLI.
 * @param report {import('eslint').ESLint.LintResult[]} The ESLint report.
 * @return {Promise<void>} A promise that resolves when the report is finished.
 */
async function reportToConsole(cli, report) {
  const stylishFormatter = await cli.loadFormatter('stylish');
  const output = new Readable();
  output.push(await stylishFormatter.format(report));
  output.push(null);
  return pipeline(output, process.stdout);
}

/**
 * Write ESLint report to the <code>build</code> directory.
 *
 * @param cli {import('eslint').ESLint} The ESLint CLI.
 * @param report {import('eslint').ESLint.LintResult[]} The ESLint report.
 * @return {Promise<void>} A promise that resolves when the report is finished.
 */
async function reportToJson(cli, report) {
  const jsonFormatter = await cli.loadFormatter('json');
  const json = await jsonFormatter.format(report);
  const reportPath = path.join(rootDir, 'build', 'eslint.json');
  return writeFile(reportPath, json, 'utf-8');
}

async function createReport() {
  const cli = new ESLint({
    useEslintrc: true,
    cwd: rootDir,
  });
  const report = await cli.lintFiles('.');
  await Promise.all([reportToConsole(cli, report), reportToJson(cli, report)]);

  if (report.some((entry) => entry.errorCount > 0)) {
    process.exitCode = 1;
  }
}

createReport().catch(console.error);
