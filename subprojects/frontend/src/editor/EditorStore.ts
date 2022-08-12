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
  redo,
  redoDepth,
  undo,
  undoDepth,
} from '@codemirror/commands';
import {
  bracketMatching,
  foldGutter,
  foldKeymap,
  indentOnInput,
  syntaxHighlighting,
} from '@codemirror/language';
import { type Diagnostic, lintKeymap, setDiagnostics } from '@codemirror/lint';
import { search, searchKeymap } from '@codemirror/search';
import {
  EditorState,
  type StateCommand,
  StateEffect,
  type Transaction,
  type TransactionSpec,
} from '@codemirror/state';
import {
  drawSelection,
  EditorView,
  highlightActiveLine,
  highlightActiveLineGutter,
  highlightSpecialChars,
  keymap,
  lineNumbers,
  rectangularSelection,
} from '@codemirror/view';
import { classHighlighter } from '@lezer/highlight';
import { makeAutoObservable, observable, reaction } from 'mobx';

import problemLanguageSupport from '../language/problemLanguageSupport';
import type ThemeStore from '../theme/ThemeStore';
import getLogger from '../utils/getLogger';
import XtextClient from '../xtext/XtextClient';

import findOccurrences, {
  type IOccurrence,
  setOccurrences,
} from './findOccurrences';
import semanticHighlighting, {
  type IHighlightRange,
  setSemanticHighlighting,
} from './semanticHighlighting';

const log = getLogger('editor.EditorStore');

export default class EditorStore {
  private readonly themeStore;

  state: EditorState;

  private readonly client: XtextClient;

  showLineNumbers = false;

  showSearchPanel = false;

  showLintPanel = false;

  errorCount = 0;

  warningCount = 0;

  infoCount = 0;

  private readonly defaultDispatcher = (tr: Transaction): void => {
    this.onTransaction(tr);
  };

  private dispatcher = this.defaultDispatcher;

  constructor(initialValue: string, themeStore: ThemeStore) {
    this.themeStore = themeStore;
    this.state = EditorState.create({
      doc: initialValue,
      extensions: [
        autocompletion({
          activateOnTyping: true,
          override: [(context) => this.client.contentAssist(context)],
        }),
        closeBrackets(),
        bracketMatching(),
        drawSelection(),
        EditorState.allowMultipleSelections.of(true),
        EditorView.theme(
          {},
          {
            dark: this.themeStore.darkMode,
          },
        ),
        findOccurrences,
        highlightActiveLine(),
        highlightActiveLineGutter(),
        highlightSpecialChars(),
        history(),
        indentOnInput(),
        rectangularSelection(),
        search({
          top: true,
          caseSensitive: true,
        }),
        syntaxHighlighting(classHighlighter),
        semanticHighlighting,
        // We add the gutters to `extensions` in the order we want them to appear.
        lineNumbers(),
        foldGutter(),
        keymap.of([
          { key: 'Mod-Shift-f', run: () => this.formatText() },
          ...closeBracketsKeymap,
          ...completionKeymap,
          ...foldKeymap,
          ...historyKeymap,
          indentWithTab,
          // Override keys in `lintKeymap` to go through the `EditorStore`.
          { key: 'Mod-Shift-m', run: () => this.setLintPanelOpen(true) },
          ...lintKeymap,
          // Override keys in `searchKeymap` to go through the `EditorStore`.
          {
            key: 'Mod-f',
            run: () => this.setSearchPanelOpen(true),
            scope: 'editor search-panel',
          },
          {
            key: 'Escape',
            run: () => this.setSearchPanelOpen(false),
            scope: 'editor search-panel',
          },
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
            StateEffect.appendConfig.of(
              EditorView.theme(
                {},
                {
                  dark: darkMode,
                },
              ),
            ),
          ],
        });
      },
    );
    makeAutoObservable(this, {
      state: observable.ref,
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
        default:
          throw new Error('Unknown severity');
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

  updateSemanticHighlighting(ranges: IHighlightRange[]): void {
    this.dispatch(setSemanticHighlighting(ranges));
  }

  updateOccurrences(write: IOccurrence[], read: IOccurrence[]): void {
    this.dispatch(setOccurrences(write, read));
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
   * @param newShowSearchPanel whether we should show the search panel
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

  formatText(): boolean {
    this.client.formatText();
    return true;
  }
}
