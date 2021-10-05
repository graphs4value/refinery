module.exports = {
  plugins: [
    'sonarjs',
  ],
  extends: [
    './.eslintrc.ci.js',
    'plugin:sonarjs/recommended',
  ],
}
