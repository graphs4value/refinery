const path = require('node:path');

// Allow the Codium ESLint plugin to find `tsconfig.json` from the repository root.
const project = [
  path.join(__dirname, 'tsconfig.json'),
  path.join(__dirname, 'tsconfig.node.json'),
];

/** @type {import('eslint').Linter.Config} */
module.exports = {
  plugins: ['@typescript-eslint', 'mobx'],
  extends: [
    'airbnb',
    'airbnb-typescript',
    'airbnb/hooks',
    'plugin:@typescript-eslint/recommended',
    'plugin:@typescript-eslint/recommended-requiring-type-checking',
    'plugin:mobx/recommended',
    'plugin:prettier/recommended',
  ],
  parserOptions: {
    project,
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
        project,
      },
    },
  },
  env: {
    browser: true,
  },
  ignorePatterns: ['build/**/*', 'dev-dist/**/*'],
  rules: {
    // In typescript, some class methods implementing an inderface do not use `this`:
    // https://github.com/typescript-eslint/typescript-eslint/issues/1103
    'class-methods-use-this': 'off',
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
  },
  overrides: [
    {
      files: ['types/**/*.d.ts'],
      rules: {
        // We don't have control over exports of external modules.
        'import/prefer-default-export': 'off',
      },
    },
    {
      files: ['*.cjs'],
      rules: {
        // https://github.com/typescript-eslint/typescript-eslint/issues/1724
        '@typescript-eslint/no-var-requires': 'off',
      },
    },
    {
      files: ['.eslintrc.cjs', 'prettier.config.cjs', 'vite.config.ts'],
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
        // Access to the environment in configuration files.
        'no-process-env': 'off',
      },
    },
  ],
};
