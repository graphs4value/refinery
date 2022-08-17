import {
  type EditorView,
  type Panel,
  runScopeHandlers,
} from '@codemirror/view';

import type SearchPanelStore from './SearchPanelStore';

export default class SearchPanel implements Panel {
  readonly dom: HTMLDivElement;

  constructor(view: EditorView, store: SearchPanelStore) {
    this.dom = document.createElement('div');
    this.dom.id = store.id;
    this.dom.className = store.panelClass;
    this.dom.addEventListener(
      'keydown',
      (event) => {
        if (runScopeHandlers(view, event, 'search-panel')) {
          event.preventDefault();
        }
      },
      {
        capture: true,
      },
    );
  }

  get top(): boolean {
    return true;
  }
}
