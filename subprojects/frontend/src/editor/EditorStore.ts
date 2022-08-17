import { CompletionContext, CompletionResult } from '@codemirror/autocomplete';
import { redo, redoDepth, undo, undoDepth } from '@codemirror/commands';
import {
  type Diagnostic,
  setDiagnostics,
  nextDiagnostic,
} from '@codemirror/lint';
import {
  type StateCommand,
  StateEffect,
  type Transaction,
  type TransactionSpec,
  type EditorState,
} from '@codemirror/state';
import { type Command, EditorView } from '@codemirror/view';
import { action, computed, makeObservable, observable } from 'mobx';
import { nanoid } from 'nanoid';

import getLogger from '../utils/getLogger';
import XtextClient from '../xtext/XtextClient';

import LintPanelStore from './LintPanelStore';
import SearchPanelStore from './SearchPanelStore';
import createEditorState from './createEditorState';
import { type IOccurrence, setOccurrences } from './findOccurrences';
import {
  type IHighlightRange,
  setSemanticHighlighting,
} from './semanticHighlighting';

const log = getLogger('editor.EditorStore');

export default class EditorStore {
  readonly id: string;

  state: EditorState;

  private readonly client: XtextClient;

  view: EditorView | undefined;

  readonly searchPanel: SearchPanelStore;

  readonly lintPanel: LintPanelStore;

  showLineNumbers = false;

  errorCount = 0;

  warningCount = 0;

  infoCount = 0;

  constructor(initialValue: string) {
    this.id = nanoid();
    this.state = createEditorState(initialValue, this);
    this.client = new XtextClient(this);
    this.searchPanel = new SearchPanelStore(this);
    this.lintPanel = new LintPanelStore(this);
    makeObservable(this, {
      state: observable.ref,
      view: observable.ref,
      showLineNumbers: observable,
      errorCount: observable,
      warningCount: observable,
      infoCount: observable,
      highestDiagnosticLevel: computed,
      canUndo: computed,
      canRedo: computed,
      setDarkMode: action,
      setEditorParent: action,
      dispatch: action,
      dispatchTransaction: action,
      doCommand: action,
      doStateCommand: action,
      updateDiagnostics: action,
      nextDiagnostic: action,
      updateOccurrences: action,
      updateSemanticHighlighting: action,
      undo: action,
      redo: action,
      toggleLineNumbers: action,
    });
  }

  setDarkMode(darkMode: boolean): void {
    log.debug('Update editor dark mode', darkMode);
    this.dispatch({
      effects: [
        StateEffect.appendConfig.of([EditorView.darkTheme.of(darkMode)]),
      ],
    });
  }

  setEditorParent(editorParent: Element | undefined): void {
    if (this.view !== undefined) {
      this.view.destroy();
    }
    if (editorParent === undefined) {
      this.view = undefined;
      return;
    }
    const view = new EditorView({
      state: this.state,
      parent: editorParent,
      dispatch: (transaction) => {
        this.dispatchTransactionWithoutView(transaction);
        view.update([transaction]);
        if (view.state !== this.state) {
          log.error(
            'Failed to synchronize editor state - store state:',
            this.state,
            'view state:',
            view.state,
          );
        }
      },
    });
    this.view = view;
    this.searchPanel.synchronizeStateToView();
    this.lintPanel.synchronizeStateToView();

    // Reported by Lighthouse 8.3.0.
    const { contentDOM, dom: containerDOM } = view;
    contentDOM.removeAttribute('aria-expanded');
    contentDOM.setAttribute('aria-label', 'Code editor');
    const lineNumbersGutter = containerDOM.querySelector('.cm-lineNumbers');
    if (lineNumbersGutter === null) {
      log.error('No line numbers in editor');
    } else {
      lineNumbersGutter.id = this.lineNumbersId;
    }

    log.info('Editor created');
  }

  dispatch(...specs: readonly TransactionSpec[]): void {
    const transaction = this.state.update(...specs);
    this.dispatchTransaction(transaction);
  }

  dispatchTransaction(transaction: Transaction): void {
    if (this.view === undefined) {
      this.dispatchTransactionWithoutView(transaction);
    } else {
      this.view.dispatch(transaction);
    }
  }

  private readonly dispatchTransactionWithoutView = action(
    (tr: Transaction) => {
      log.trace('Editor transaction', tr);
      this.state = tr.state;
      this.client.onTransaction(tr);
    },
  );

  doCommand(command: Command): boolean {
    if (this.view === undefined) {
      return false;
    }
    return command(this.view);
  }

  doStateCommand(command: StateCommand): boolean {
    return command({
      state: this.state,
      dispatch: (transaction) => this.dispatchTransaction(transaction),
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

  nextDiagnostic(): void {
    this.doCommand(nextDiagnostic);
  }

  get highestDiagnosticLevel(): Diagnostic['severity'] | undefined {
    if (this.errorCount > 0) {
      return 'error';
    }
    if (this.warningCount > 0) {
      return 'warning';
    }
    if (this.infoCount > 0) {
      return 'info';
    }
    return undefined;
  }

  updateSemanticHighlighting(ranges: IHighlightRange[]): void {
    this.dispatch(setSemanticHighlighting(ranges));
  }

  updateOccurrences(write: IOccurrence[], read: IOccurrence[]): void {
    this.dispatch(setOccurrences(write, read));
  }

  contentAssist(context: CompletionContext): Promise<CompletionResult> {
    return this.client.contentAssist(context);
  }

  /**
   * @returns `true` if there is history to undo
   */
  get canUndo(): boolean {
    return undoDepth(this.state) > 0;
  }

  undo(): void {
    log.debug('Undo', this.doStateCommand(undo));
  }

  /**
   * @returns `true` if there is history to redo
   */
  get canRedo(): boolean {
    return redoDepth(this.state) > 0;
  }

  redo(): void {
    log.debug('Redo', this.doStateCommand(redo));
  }

  get lineNumbersId(): string {
    return `${this.id}-lineNumbers`;
  }

  toggleLineNumbers(): void {
    this.showLineNumbers = !this.showLineNumbers;
    log.debug('Show line numbers', this.showLineNumbers);
  }

  formatText(): boolean {
    this.client.formatText();
    return true;
  }
}
