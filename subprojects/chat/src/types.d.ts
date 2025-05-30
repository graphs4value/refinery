/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

declare module '*.md' {
  const content: string;
  export default content;
}

declare module '*.txt' {
  const content: string;
  export default content;
}
