/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors
 *
 * SPDX-License-Identifier: EPL-2.0
 */

const { readFileSync } = require('node:fs');
const path = require('node:path');

// Allow the Codium ESLint plugin to find `tsconfig.json` from the repository root.
const project = [
  path.join(__dirname, 'tsconfig.json'),
  path.join(__dirname, 'subprojects/client-js/tsconfig.json'),
  path.join(__dirname, 'subprojects/docs/tsconfig.json'),
  path.join(__dirname, 'subprojects/frontend/tsconfig.json'),
  path.join(__dirname, 'subprojects/frontend/tsconfig.node.json'),
  path.join(__dirname, 'subprojects/frontend/tsconfig.shared.json'),
];

const frontendPackageJSONPath = path.join(
  __dirname,
  'subprojects/frontend/package.json',
);
const frontendPackageJSON = /** @type {unknown} */ (
  JSON.parse(readFileSync(frontendPackageJSONPath, 'utf8'))
);
const reactVersion = /** @type { {dependencies?: {react?: string}} } */ (
  frontendPackageJSON
)?.dependencies?.react?.replace('^', '');

/** @type {import('eslint').Linter.Config} */
module.exports = {
  plugins: [
    '@typescript-eslint',
    'import',
    'jsx-a11y',
    'mobx',
    'prettier',
    'react',
    'react-hooks',
  ],
  extends: [
    'eslint:recommended',
    'plugin:@typescript-eslint/recommended-type-checked',
    'plugin:@typescript-eslint/stylistic-type-checked',
    'plugin:import/recommended',
    'plugin:import/typescript',
    'plugin:jsx-a11y/recommended',
    'plugin:mobx/recommended',
    'plugin:prettier/recommended',
    'plugin:react/recommended',
    'plugin:react-hooks/recommended',
  ],
  parserOptions: {
    projectService: true,
    sourceType: 'module',
  },
  parser: '@typescript-eslint/parser',
  settings: {
    'import/parsers': {
      '@typescript-eslint/parser': ['.ts', '.tsx'],
    },
    'import/resolver': {
      typescript: {
        alwaysTryTypes: true,
        noWarnOnMultipleProjects: true,
        project,
      },
    },
    react: {
      version: reactVersion,
    },
  },
  env: {
    browser: true,
  },
  ignorePatterns: [
    'build/**/*',
    'subprojects/*/build/**/*',
    'subprojects/*/dist/**/*',
    'subprojects/docs/.docusaurus/**/*',
    'subprojects/docs/.yarn/**/*',
    'subprojects/frontend/dev-dist/**/*',
    'subprojects/frontend/src/**/*.typegen.ts',
  ],
  rules: {
    // In typescript, some class methods implementing an inderface do not use `this`:
    // https://github.com/typescript-eslint/typescript-eslint/issues/1103
    'class-methods-use-this': 'off',
    eqeqeq: 'error',
    // Disable rules with a high performance cost.
    // See https://typescript-eslint.io/linting/troubleshooting/performance-troubleshooting/
    'import/default': 'off',
    'import/extensions': 'off',
    'import/named': 'off',
    'import/namespace': 'off',
    'import/no-named-as-default': 'off',
    'import/no-named-as-default-member': 'off',
    '@typescript-eslint/indent': 'off',
    // Make sure every import can be resolved by `eslint-import-resolver-typescript`.
    'import/no-unresolved': 'error',
    // Organize imports automatically.
    'import/order': [
      'error',
      {
        alphabetize: {
          order: 'asc',
        },
        'newlines-between': 'always',
      },
    ],
    // Not all components depend on observable state.
    'mobx/missing-observer': 'off',
    // Prefer a logger instead of `console.log`.
    'no-console': 'warn',
    // A dangling underscore, while not neccessary for all private fields,
    // is useful for backing fields of properties that should be read-only from outside the class.
    'no-underscore-dangle': [
      'error',
      {
        allowAfterThis: true,
        allowFunctionParams: true,
      },
    ],
    // Use prop spreading to conditionally add props with `exactOptionalPropertyTypes`.
    'react/jsx-props-no-spreading': 'off',
    // We use the `react-jsx` runtime, so there is no need to import `React`.
    'react/react-in-jsx-scope': 'off',
    // `defaultProps` are deprecated in React 18.
    'react/require-default-props': 'off',
    // `@mui/icons-material` resolves all icons to the same `.d.ts` file,
    // which makes the `eslint-plugin-import` version of this rule emit a warning.
    // See https://github.com/import-js/eslint-plugin-import/issues/1479#issuecomment-2408001379
    'import/no-duplicates': 'off',
    'no-duplicate-imports': 'warn',
  },
  overrides: [
    {
      files: ['subprojects/*/types/**/*.d.ts'],
      rules: {
        // We don't have control over exports of external modules.
        'import/prefer-default-export': 'off',
      },
    },
    {
      files: ['*.cjs'],
      rules: {
        '@typescript-eslint/no-require-imports': 'off',
        // https://github.com/typescript-eslint/typescript-eslint/issues/1724
        '@typescript-eslint/no-var-requires': 'off',
      },
    },
    {
      files: ['*.cts'],
      rules: {
        // Allow `import type` in CommonJS TypeScript modules.
        'import/no-import-module-exports': 'off',
      },
    },
    {
      files: [
        '.eslintrc.cjs',
        'scripts/*.cjs',
        'scripts/*.mjs',
        'subprojects/*/config/*.ts',
        'subprojects/*/config/*.cjs',
        'prettier.config.cjs',
        'subprojects/*/esbuild.mjs',
        'subprojects/*/vite.config.ts',
        'subprojects/*/vitest.config.ts',
        'subprojects/*/vitest.workspace.ts',
      ],
      env: {
        browser: false,
        node: true,
      },
      rules: {
        // Allow devDependencies in configuration files.
        'import/no-extraneous-dependencies': [
          'error',
          { devDependencies: true },
        ],
        // Allow writing to the console in ad-hoc scripts.
        'no-console': 'off',
        // Access to the environment in configuration files.
        'no-process-env': 'off',
      },
    },
    {
      files: ['subprojects/docs/src/**/*'],
      rules: {
        'import/no-unresolved': [
          'error',
          {
            ignore: [
              // These imports are resolved by Docusaurus, not TypeScript.
              '^@docusaurus/',
              '^@theme/',
              '^@theme-original/',
            ],
          },
        ],
      },
    },
  ],
};
