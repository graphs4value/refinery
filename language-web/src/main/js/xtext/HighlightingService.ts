import { Decoration } from '@codemirror/view';
import { Range, RangeSet } from '@codemirror/rangeset';

import type { EditorStore } from '../editor/EditorStore';
import type { UpdateService } from './UpdateService';
import { getLogger } from '../utils/logger';
import { isHighlightingResult } from './xtextServiceResults';

const log = getLogger('xtext.ValidationService');

export class HighlightingService {
  private store: EditorStore;

  private updateService: UpdateService;

  constructor(store: EditorStore, updateService: UpdateService) {
    this.store = store;
    this.updateService = updateService;
  }

  onPush(push: unknown): void {
    if (!isHighlightingResult(push)) {
      log.error('Invalid highlighting result', push);
      return;
    }
    const allChanges = this.updateService.computeChangesSinceLastUpdate();
    const decorations: Range<Decoration>[] = [];
    push.regions.forEach(({ offset, length, styleClasses }) => {
      if (styleClasses.length === 0) {
        return;
      }
      const from = allChanges.mapPos(offset);
      const to = allChanges.mapPos(offset + length);
      if (to <= from) {
        return;
      }
      decorations.push(Decoration.mark({
        class: styleClasses.map((c) => `cmt-problem-${c}`).join(' '),
      }).range(from, to));
    });
    this.store.updateSemanticHighlighting(RangeSet.of(decorations, true));
  }
}
