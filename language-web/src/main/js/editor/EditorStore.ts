import { autocompletion, completionKeymap } from '@codemirror/autocomplete';
import { closeBrackets, closeBracketsKeymap } from '@codemirror/closebrackets';
import { defaultKeymap, indentWithTab } from '@codemirror/commands';
import { commentKeymap } from '@codemirror/comment';
import { foldGutter, foldKeymap } from '@codemirror/fold';
import { highlightActiveLineGutter, lineNumbers } from '@codemirror/gutter';
import { classHighlightStyle } from '@codemirror/highlight';
import {
  history,
  historyKeymap,
  redo,
  redoDepth,
  undo,
  undoDepth,
} from '@codemirror/history';
import { indentOnInput } from '@codemirror/language';
import {
  Diagnostic,
  lintKeymap,
  setDiagnostics,
} from '@codemirror/lint';
import { bracketMatching } from '@codemirror/matchbrackets';
import { rectangularSelection } from '@codemirror/rectangular-selection';
import { searchConfig, searchKeymap } from '@codemirror/search';
import {
  EditorState,
  StateCommand,
  StateEffect,
  Transaction,
  TransactionSpec,
} from '@codemirror/state';
import {
  drawSelection,
  EditorView,
  highlightActiveLine,
  highlightSpecialChars,
  keymap,
} from '@codemirror/view';
import {
  makeAutoObservable,
  observable,
  reaction,
} from 'mobx';

import { getLogger } from '../logging';
import { problemLanguageSupport } from './problemLanguageSupport';
import type { ThemeStore } from '../theme/ThemeStore';
import { XtextClient } from './XtextClient';

const log = getLogger('EditorStore');

export class EditorStore {
  themeStore;

  state: EditorState;

  client: XtextClient;

  showLineNumbers = false;

  showSearchPanel = false;

  showLintPanel = false;

  errorCount = 0;

  warningCount = 0;

  infoCount = 0;

  readonly defaultDispatcher = (tr: Transaction): void => {
    this.onTransaction(tr);
  };

  dispatcher = this.defaultDispatcher;

  constructor(initialValue: string, themeStore: ThemeStore) {
    this.themeStore = themeStore;
    this.state = EditorState.create({
      doc: initialValue,
      extensions: [
        autocompletion(),
        classHighlightStyle.extension,
        closeBrackets(),
        bracketMatching(),
        drawSelection(),
        EditorState.allowMultipleSelections.of(true),
        EditorView.theme({}, {
          dark: this.themeStore.darkMode,
        }),
        highlightActiveLine(),
        highlightActiveLineGutter(),
        highlightSpecialChars(),
        history(),
        indentOnInput(),
        rectangularSelection(),
        searchConfig({
          top: true,
          matchCase: true,
        }),
        // We add the gutters to `extensions` in the order we want them to appear.
        foldGutter(),
        lineNumbers(),
        keymap.of([
          ...closeBracketsKeymap,
          ...commentKeymap,
          ...completionKeymap,
          ...foldKeymap,
          ...historyKeymap,
          indentWithTab,
          // Override keys in `lintKeymap` to go through the `EditorStore`.
          { key: 'Mod-Shift-m', run: () => this.setLintPanelOpen(true) },
          ...lintKeymap,
          // Override keys in `searchKeymap` to go through the `EditorStore`.
          { key: 'Mod-f', run: () => this.setSearchPanelOpen(true), scope: 'editor search-panel' },
          { key: 'Escape', run: () => this.setSearchPanelOpen(false), scope: 'editor search-panel' },
          ...searchKeymap,
          ...defaultKeymap,
        ]),
        problemLanguageSupport(),
      ],
    });
    this.client = new XtextClient(this);
    reaction(
      () => this.themeStore.darkMode,
      (darkMode) => {
        log.debug('Update editor dark mode', darkMode);
        this.dispatch({
          effects: [
            StateEffect.appendConfig.of(EditorView.theme({}, {
              dark: darkMode,
            })),
          ],
        });
      },
    );
    makeAutoObservable(this, {
      themeStore: false,
      state: observable.ref,
      defaultDispatcher: false,
      dispatcher: false,
    });
  }

  updateDispatcher(newDispatcher: ((tr: Transaction) => void) | null): void {
    this.dispatcher = newDispatcher || this.defaultDispatcher;
  }

  onTransaction(tr: Transaction): void {
    log.trace('Editor transaction', tr);
    this.state = tr.state;
    this.client.onTransaction(tr);
  }

  dispatch(...specs: readonly TransactionSpec[]): void {
    this.dispatcher(this.state.update(...specs));
  }

  doStateCommand(command: StateCommand): boolean {
    return command({
      state: this.state,
      dispatch: this.dispatcher,
    });
  }

  updateDiagnostics(diagnostics: Diagnostic[]): void {
    this.dispatch(setDiagnostics(this.state, diagnostics));
    this.errorCount = 0;
    this.warningCount = 0;
    this.infoCount = 0;
    diagnostics.forEach(({ severity }) => {
      switch (severity) {
        case 'error':
          this.errorCount += 1;
          break;
        case 'warning':
          this.warningCount += 1;
          break;
        case 'info':
          this.infoCount += 1;
          break;
      }
    });
  }

  get highestDiagnosticLevel(): Diagnostic['severity'] | null {
    if (this.errorCount > 0) {
      return 'error';
    }
    if (this.warningCount > 0) {
      return 'warning';
    }
    if (this.infoCount > 0) {
      return 'info';
    }
    return null;
  }

  /**
   * @returns `true` if there is history to undo
   */
  get canUndo(): boolean {
    return undoDepth(this.state) > 0;
  }

  // eslint-disable-next-line class-methods-use-this
  undo(): void {
    log.debug('Undo', this.doStateCommand(undo));
  }

  /**
   * @returns `true` if there is history to redo
   */
  get canRedo(): boolean {
    return redoDepth(this.state) > 0;
  }

  // eslint-disable-next-line class-methods-use-this
  redo(): void {
    log.debug('Redo', this.doStateCommand(redo));
  }

  toggleLineNumbers(): void {
    this.showLineNumbers = !this.showLineNumbers;
    log.debug('Show line numbers', this.showLineNumbers);
  }

  /**
   * Sets whether the CodeMirror search panel should be open.
   *
   * This method can be used as a CodeMirror command,
   * because it returns `false` if it didn't execute,
   * allowing other commands for the same keybind to run instead.
   * This matches the behavior of the `openSearchPanel` and `closeSearchPanel`
   * commands from `'@codemirror/search'`.
   *
   * @param newShosSearchPanel whether we should show the search panel
   * @returns `true` if the state was changed, `false` otherwise
   */
  setSearchPanelOpen(newShowSearchPanel: boolean): boolean {
    if (this.showSearchPanel === newShowSearchPanel) {
      return false;
    }
    this.showSearchPanel = newShowSearchPanel;
    log.debug('Show search panel', this.showSearchPanel);
    return true;
  }

  toggleSearchPanel(): void {
    this.setSearchPanelOpen(!this.showSearchPanel);
  }

  setLintPanelOpen(newShowLintPanel: boolean): boolean {
    if (this.showLintPanel === newShowLintPanel) {
      return false;
    }
    this.showLintPanel = newShowLintPanel;
    log.debug('Show lint panel', this.showLintPanel);
    return true;
  }

  toggleLintPanel(): void {
    this.setLintPanelOpen(!this.showLintPanel);
  }
}
