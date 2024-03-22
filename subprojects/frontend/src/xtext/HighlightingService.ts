/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import type EditorStore from '../editor/EditorStore';
import type { IHighlightRange } from '../editor/semanticHighlighting';

import type UpdateService from './UpdateService';
import { highlightingResult } from './xtextServiceResults';

const TYPE_HASH_HEX_PREFIX = 'typeHash-_';

export default class HighlightingService {
  constructor(
    private readonly store: EditorStore,
    private readonly updateService: UpdateService,
  ) {}

  onPush(push: unknown): void {
    const { regions } = highlightingResult.parse(push);
    const allChanges = this.updateService.computeChangesSinceLastUpdate();
    const ranges: IHighlightRange[] = [];
    const hexTypeHashes = new Set<string>();
    regions.forEach(({ offset, length, styleClasses }) => {
      if (styleClasses.length === 0) {
        return;
      }
      const from = allChanges.mapPos(offset);
      const to = allChanges.mapPos(offset + length);
      if (to <= from) {
        return;
      }
      ranges.push({
        from,
        to,
        classes: styleClasses,
      });
      styleClasses.forEach((styleClass) => {
        if (styleClass.startsWith(TYPE_HASH_HEX_PREFIX)) {
          hexTypeHashes.add(styleClass.substring(TYPE_HASH_HEX_PREFIX.length));
        }
      });
    });
    this.store.updateSemanticHighlighting(ranges, Array.from(hexTypeHashes));
  }

  onDisconnect(): void {
    this.store.updateSemanticHighlighting([], []);
  }
}
