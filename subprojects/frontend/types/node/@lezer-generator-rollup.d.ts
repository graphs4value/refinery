// We have to explicitly redeclare the type of the `./rollup` ESM export of `@lezer/generator`,
// because TypeScript can't find it on its own even with `"moduleResolution": "Node16"`.
declare module '@lezer/generator/rollup' {
  import type { PluginOptions } from 'vite';

  export function lezer(): PluginOptions;
}
