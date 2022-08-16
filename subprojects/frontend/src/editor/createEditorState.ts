import {
  closeBrackets,
  closeBracketsKeymap,
  autocompletion,
  completionKeymap,
} from '@codemirror/autocomplete';
import {
  defaultKeymap,
  history,
  historyKeymap,
  indentWithTab,
} from '@codemirror/commands';
import {
  bracketMatching,
  codeFolding,
  foldGutter,
  foldKeymap,
  indentOnInput,
  syntaxHighlighting,
} from '@codemirror/language';
import { lintKeymap, lintGutter } from '@codemirror/lint';
import { search, searchKeymap } from '@codemirror/search';
import { EditorState } from '@codemirror/state';
import {
  drawSelection,
  highlightActiveLine,
  highlightActiveLineGutter,
  highlightSpecialChars,
  keymap,
  lineNumbers,
  rectangularSelection,
} from '@codemirror/view';
import { classHighlighter } from '@lezer/highlight';

import problemLanguageSupport from '../language/problemLanguageSupport';

import type EditorStore from './EditorStore';
import editorClassNames from './editorClassNames';
import findOccurrences from './findOccurrences';
import semanticHighlighting from './semanticHighlighting';

export default function createEditorState(
  initialValue: string,
  store: EditorStore,
): EditorState {
  return EditorState.create({
    doc: initialValue,
    extensions: [
      autocompletion({
        activateOnTyping: true,
        override: [(context) => store.contentAssist(context)],
      }),
      closeBrackets(),
      bracketMatching(),
      drawSelection(),
      EditorState.allowMultipleSelections.of(true),
      findOccurrences,
      highlightActiveLine(),
      highlightActiveLineGutter(),
      highlightSpecialChars(),
      history(),
      indentOnInput(),
      rectangularSelection(),
      search({ top: true }),
      syntaxHighlighting(classHighlighter),
      semanticHighlighting,
      // We add the gutters to `extensions` in the order we want them to appear.
      lintGutter(),
      lineNumbers(),
      codeFolding({
        placeholderDOM(_view, onClick) {
          const button = document.createElement('button');
          button.className = editorClassNames.foldPlaceholder;
          button.ariaLabel = 'Unfold lines';
          button.innerText = '...';
          button.onclick = onClick;
          return button;
        },
      }),
      foldGutter({
        markerDOM(open) {
          const button = document.createElement('button');
          button.className = [
            editorClassNames.foldMarker,
            open
              ? editorClassNames.foldMarkerOpen
              : editorClassNames.foldMarkerClosed,
          ].join(' ');
          button.ariaPressed = open ? 'true' : 'false';
          button.ariaLabel = 'Fold lines';
          return button;
        },
      }),
      keymap.of([
        { key: 'Mod-Shift-f', run: () => store.formatText() },
        ...closeBracketsKeymap,
        ...completionKeymap,
        ...foldKeymap,
        ...historyKeymap,
        indentWithTab,
        // Override keys in `lintKeymap` to go through the `EditorStore`.
        { key: 'Mod-Shift-m', run: () => store.lintPanel.open() },
        ...lintKeymap,
        // Override keys in `searchKeymap` to go through the `EditorStore`.
        {
          key: 'Mod-f',
          run: () => store.searchPanel.open(),
          scope: 'editor search-panel',
        },
        {
          key: 'Escape',
          run: () => store.searchPanel.close(),
          scope: 'editor search-panel',
        },
        ...searchKeymap,
        ...defaultKeymap,
      ]),
      problemLanguageSupport(),
    ],
  });
}
