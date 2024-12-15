/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import type { Tooltip } from '@codemirror/view';

import type EditorStore from '../editor/EditorStore';

import type UpdateService from './UpdateService';
import findToken from './findToken';

export default class HoverService {
  constructor(
    private readonly store: EditorStore,
    private readonly updateService: UpdateService,
  ) {}

  async hoverTooltip(pos: number): Promise<Tooltip | null> {
    const { from, to } = findToken(pos, this.store.state) ?? {
      from: pos,
      to: pos,
    };
    const [result, { default: transformDocumentation }] = await Promise.all([
      this.updateService.fetchHoverTooltip(pos),
      import('./transformDocumentation'),
    ]);
    if (result.cancelled) {
      return null;
    }
    const {
      data: { title, content },
    } = result;
    if (title === undefined && content === undefined) {
      return null;
    }
    const wrappedTitle =
      title === undefined
        ? ''
        : `<h2 class="refinery-completion-title">${title}</h2>\n\n`;
    const text = wrappedTitle + (content ?? '');
    return {
      pos: from,
      end: to,
      create: () => ({
        dom: transformDocumentation(text),
      }),
    };
  }
}
