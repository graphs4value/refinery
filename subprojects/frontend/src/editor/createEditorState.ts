/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import {
  acceptCompletion,
  autocompletion,
  closeBrackets,
  closeBracketsKeymap,
  completionKeymap,
} from '@codemirror/autocomplete';
import {
  copyLineDown,
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
import { search, searchKeymap, selectNextOccurrence } from '@codemirror/search';
import { Compartment, EditorState, type Extension } from '@codemirror/state';
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
import SearchPanel from './SearchPanel';
import exposeDiagnostics from './exposeDiagnostics';
import findOccurrences from './findOccurrences';
import semanticHighlighting from './semanticHighlighting';

export const historyCompartment = new Compartment();

export function createHistoryExtension(): Extension {
  return history();
}

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
      exposeDiagnostics,
      findOccurrences,
      highlightActiveLine(),
      highlightActiveLineGutter(),
      highlightSpecialChars(),
      historyCompartment.of([createHistoryExtension()]),
      indentOnInput(),
      rectangularSelection(),
      search({
        createPanel(view) {
          return new SearchPanel(view, store.searchPanel);
        },
      }),
      syntaxHighlighting(classHighlighter),
      semanticHighlighting,
      // We add the gutters to `extensions` in the order we want them to appear.
      lintGutter(),
      lineNumbers(),
      codeFolding({
        placeholderDOM(_view, onClick) {
          const button = document.createElement('button');
          button.className = 'problem-editor-foldPlaceholder';
          button.ariaLabel = 'Unfold lines';
          const span = document.createElement('span');
          span.innerText = '...';
          button.appendChild(span);
          button.addEventListener('click', onClick);
          return button;
        },
      }),
      foldGutter({
        markerDOM(open) {
          const div = document.createElement('div');
          div.className = [
            'problem-editor-foldMarker',
            `problem-editor-foldMarker-${open ? 'open' : 'closed'}`,
          ].join(' ');
          return div;
        },
      }),
      keymap.of([
        { key: 'Mod-Shift-f', run: () => store.formatText() },
        { key: 'Mod-o', run: () => store.openFile() },
        {
          key: 'Mod-s',
          run: () => store.saveFile(),
          shift: () => store.saveFileAs(),
          preventDefault: true,
        },
        ...closeBracketsKeymap,
        ...completionKeymap,
        ...foldKeymap,
        ...historyKeymap,
        // Enable accepting completions with tab, overrides `Tab` from
        // `indentWithTab` if there is an active completion.
        { key: 'Tab', run: acceptCompletion },
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
        // Override `Mod-d` from `searchKeymap`.
        {
          key: 'Mod-d',
          run: copyLineDown,
          shift: selectNextOccurrence,
          preventDefault: true,
        },
        ...searchKeymap,
        ...defaultKeymap,
      ]),
      problemLanguageSupport(),
    ],
  });
}
