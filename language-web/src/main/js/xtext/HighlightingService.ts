import type { EditorStore } from '../editor/EditorStore';
import type { IHighlightRange } from '../editor/semanticHighlighting';
import type { UpdateService } from './UpdateService';
import { getLogger } from '../utils/logger';
import { isHighlightingResult } from './xtextServiceResults';

const log = getLogger('xtext.ValidationService');

export class HighlightingService {
  private readonly store: EditorStore;

  private readonly updateService: UpdateService;

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
    const ranges: IHighlightRange[] = [];
    push.regions.forEach(({ offset, length, styleClasses }) => {
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
    });
    this.store.updateSemanticHighlighting(ranges);
  }
}
