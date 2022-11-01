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
  RangeSet,
} from '@codemirror/state';
import { type Command, EditorView } from '@codemirror/view';
import { makeAutoObservable, observable } from 'mobx';
import { nanoid } from 'nanoid';

import type PWAStore from '../PWAStore';
import getLogger from '../utils/getLogger';
import XtextClient from '../xtext/XtextClient';

import DiagnosticValue from './DiagnosticValue';
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

  diagnostics: RangeSet<DiagnosticValue> = RangeSet.of([]);

  constructor(initialValue: string, pwaStore: PWAStore) {
    this.id = nanoid();
    this.state = createEditorState(initialValue, this);
    this.client = new XtextClient(this, pwaStore);
    this.searchPanel = new SearchPanelStore(this);
    this.lintPanel = new LintPanelStore(this);
    makeAutoObservable<EditorStore, 'client'>(this, {
      id: false,
      state: observable.ref,
      client: false,
      view: observable.ref,
      searchPanel: false,
      lintPanel: false,
      contentAssist: false,
      formatText: false,
    });
    this.client.start();
  }

  get opened(): boolean {
    return this.client.webSocketClient.opened;
  }

  get opening(): boolean {
    return this.client.webSocketClient.opening;
  }

  get disconnectedByUser(): boolean {
    return this.client.webSocketClient.disconnectedByUser;
  }

  get networkMissing(): boolean {
    return this.client.webSocketClient.networkMissing;
  }

  get connectionErrors(): string[] {
    return this.client.webSocketClient.errors;
  }

  connect(): void {
    this.client.webSocketClient.connect();
  }

  disconnect(): void {
    this.client.webSocketClient.disconnect();
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
    const { contentDOM } = view;
    contentDOM.removeAttribute('aria-expanded');
    contentDOM.setAttribute('aria-label', 'Code editor');

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

  private dispatchTransactionWithoutView(tr: Transaction): void {
    log.trace('Editor transaction', tr);
    this.state = tr.state;
    this.client.onTransaction(tr);
    if (tr.docChanged) {
      this.diagnostics = this.diagnostics.map(tr.changes);
    }
  }

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
    diagnostics.sort((a, b) => a.from - b.from);
    this.dispatch(setDiagnostics(this.state, diagnostics));
    this.diagnostics = RangeSet.of(
      diagnostics.map(({ severity, from, to }) =>
        DiagnosticValue.VALUES[severity].range(from, to),
      ),
    );
  }

  countDiagnostics(severity: Diagnostic['severity']): number {
    return this.diagnostics.update({
      filter: (_from, _to, value) => value.eq(DiagnosticValue.VALUES[severity]),
    }).size;
  }

  get errorCount(): number {
    return this.countDiagnostics('error');
  }

  get warningCount(): number {
    return this.countDiagnostics('warning');
  }

  get infoCount(): number {
    return this.countDiagnostics('info');
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

  toggleLineNumbers(): void {
    this.showLineNumbers = !this.showLineNumbers;
    log.debug('Show line numbers', this.showLineNumbers);
  }

  get hasSelection(): boolean {
    return this.state.selection.ranges.some(({ from, to }) => from !== to);
  }

  formatText(): boolean {
    this.client.formatText();
    return true;
  }

  dispose(): void {
    this.client.dispose();
  }
}
