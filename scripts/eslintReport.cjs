/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

const { writeFile } = require('node:fs/promises');
const path = require('node:path');
const { Readable } = require('node:stream');
const { pipeline } = require('node:stream/promises');

const { ESLint } = require('eslint');

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
 * @param workspace {string} The workspace path.
 * @param report {import('eslint').ESLint.LintResult[]} The ESLint report.
 * @return {Promise<void>} A promise that resolves when the report is finished.
 */
async function reportToJson(cli, workspace, report) {
  const jsonFormatter = await cli.loadFormatter('json');
  const json = await jsonFormatter.format(report);
  const reportPath = path.join(workspace, 'build', 'eslint.json');
  return writeFile(reportPath, json, 'utf-8');
}

/**
 * Write ESLint report to both the console and the <code>build</code> directory.
 *
 * @param workspace {string | undefined} The workspace path or `undefined`
 *                                       for the root workspace.
 * @param fix {boolean} `true` if errors should be fixed.
 * @return {Promise<void>} A promise that resolves when the report is finished.
 */
async function createReport(workspace, fix) {
  const absoluteWorkspace = path.resolve(__dirname, '..', workspace ?? '.');
  /** @type {import('eslint').ESLint.Options} */
  const options = {
    useEslintrc: true,
    cwd: absoluteWorkspace,
    fix,
  };
  if (workspace === undefined) {
    options.overrideConfig = {
      ignorePatterns: ['subprojects/**/*'],
    };
  }
  const cli = new ESLint(options);
  const report = await cli.lintFiles('.');
  await Promise.all([
    reportToConsole(cli, report),
    reportToJson(cli, absoluteWorkspace, report),
  ]);

  if (report.some((entry) => entry.errorCount > 0)) {
    process.exitCode = 1;
  }
}

const fixArg = '--fix';
let fix = false;
/** @type {string | undefined} */
let workspace;
if (process.argv[2] === fixArg) {
  fix = true;
} else {
  /* eslint-disable-next-line prefer-destructuring --
   * Destructuring is harder to read here.
   */
  workspace = process.argv[2];
  fix = process.argv[3] === fixArg;
}
createReport(workspace, fix).catch(console.error);
