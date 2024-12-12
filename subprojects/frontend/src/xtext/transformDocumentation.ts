/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import DOMPurify from 'dompurify';
import markdownit from 'markdown-it';

const md = markdownit({
  html: true,
  linkify: true,
  typographer: true,
})
  // See https://github.com/markdown-it/markdown-it/issues/1066
  .disable('code');

export default function transformDocumentation(documentation: string): Node {
  const dangeroudHTML = md.render(documentation);
  const html = DOMPurify.sanitize(dangeroudHTML);
  const node = document.createElement('div');
  node.classList.add('refinery-completion-documentation');
  node.innerHTML = html;
  return node;
}
