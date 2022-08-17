import { closeLintPanel, openLintPanel } from '@codemirror/lint';

import type EditorStore from './EditorStore';
import PanelStore from './PanelStore';

export default class LintPanelStore extends PanelStore {
  constructor(store: EditorStore) {
    super('cm-panel-lint', openLintPanel, closeLintPanel, store);
  }
}
