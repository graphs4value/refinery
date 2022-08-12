declare module '*.grammar' {
  import type { LRParser } from '@lezer/lr';

  export const parser: LRParser;
}
