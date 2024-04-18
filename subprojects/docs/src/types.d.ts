/*
 * Copyright (c) 2016, Jeremy Stucki
 * Copyright (c) 2024 The Refinery Authors
 *
 * SPDX-License-Identifier: BSD-3-Clause AND EPL-2.0
 *
 * Typings for `ResponsiveImageOutput` copied from
 * https://github.com/dazuaz/responsive-loader/blob/ef2c806fcd36f06f6be8a0b97e09f40c3d86d3ac/README.md
 */

declare module '*?url' {
  const url: string;
  export default url;
}

declare module '*&url' {
  const url: string;
  export default url;
}

interface ResponsiveImageOutput {
  src: string;
  srcSet: string;
  placeholder: string | undefined;
  images: { path: string; width: number; height: number }[];
  width: number;
  height: number;
  toString: () => string;
}

declare module '*?rl' {
  const src: ResponsiveImageOutput;
  export default src;
}

declare module '*&rl' {
  const src: ResponsiveImageOutput;
  export default src;
}
