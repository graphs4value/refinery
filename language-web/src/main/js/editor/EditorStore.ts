import type { Editor, EditorConfiguration } from 'codemirror';
import {
  createAtom,
  makeAutoObservable,
  observable,
  runInAction,
} from 'mobx';
import type { IXtextOptions, IXtextServices } from 'xtext/xtext-codemirror';

import type { IEditorChunk } from './editor';
import type { ThemeStore } from '../theme/ThemeStore';

const xtextLang = 'problem';

const xtextOptions: IXtextOptions = {
  xtextLang,
  enableFormattingAction: true,
};

const codeMirrorGlobalOptions: EditorConfiguration = {
  mode: `xtext/${xtextLang}`,
  indentUnit: 2,
  styleActiveLine: true,
};

export class EditorStore {
  themeStore;

  atom;

  chunk?: IEditorChunk;

  editor?: Editor;

  xtextServices?: IXtextServices;

  value = '';

  showLineNumbers = false;

  initialSelection!: { start: number, end: number, focused: boolean };

  constructor(themeStore: ThemeStore) {
    this.themeStore = themeStore;
    this.atom = createAtom('EditorStore');
    this.resetInitialSelection();
    makeAutoObservable(this, {
      themeStore: false,
      atom: false,
      chunk: observable.ref,
      editor: observable.ref,
      xtextServices: observable.ref,
      initialSelection: false,
    });
    import('./editor').then(({ editorChunk }) => {
      runInAction(() => {
        this.chunk = editorChunk;
      });
    }).catch((error) => {
      console.warn('Error while loading editor', error);
    });
  }

  setInitialSelection(start: number, end: number, focused: boolean): void {
    this.initialSelection = { start, end, focused };
    this.applyInitialSelectionToEditor();
  }

  private resetInitialSelection(): void {
    this.initialSelection = {
      start: 0,
      end: 0,
      focused: false,
    };
  }

  private applyInitialSelectionToEditor(): void {
    if (this.editor) {
      const { start, end, focused } = this.initialSelection;
      const doc = this.editor.getDoc();
      const startPos = doc.posFromIndex(start);
      const endPos = doc.posFromIndex(end);
      doc.setSelection(startPos, endPos, {
        scroll: true,
      });
      if (focused) {
        this.editor.focus();
      }
      this.resetInitialSelection();
    }
  }

  /**
   * Attaches a new CodeMirror instance and creates Xtext services.
   *
   * The store will not subscribe to any CodeMirror events. Instead,
   * the editor component should subscribe to them and relay them to the store.
   *
   * @param newEditor The new CodeMirror instance
   */
  editorDidMount(newEditor: Editor): void {
    if (!this.chunk) {
      throw new Error('Editor not loaded yet');
    }
    if (this.editor) {
      throw new Error('CoreMirror editor mounted before unmounting');
    }
    this.editor = newEditor;
    this.xtextServices = this.chunk.createServices(newEditor, xtextOptions);
    this.applyInitialSelectionToEditor();
  }

  editorWillUnmount(): void {
    if (!this.chunk) {
      throw new Error('Editor not loaded yet');
    }
    if (this.editor) {
      this.chunk.removeServices(this.editor);
    }
    delete this.editor;
    delete this.xtextServices;
  }

  /**
   * Updates the contents of the editor.
   *
   * @param newValue The new contents of the editor
   */
  updateValue(newValue: string): void {
    this.value = newValue;
  }

  reportChanged(): void {
    this.atom.reportChanged();
  }

  protected observeEditorChanges(): void {
    this.atom.reportObserved();
  }

  get codeMirrorTheme(): string {
    return `problem-${this.themeStore.className}`;
  }

  get codeMirrorOptions(): EditorConfiguration {
    return {
      ...codeMirrorGlobalOptions,
      theme: this.codeMirrorTheme,
      lineNumbers: this.showLineNumbers,
    };
  }

  /**
   * @returns `true` if there is history to undo
   */
  get canUndo(): boolean {
    this.observeEditorChanges();
    if (!this.editor) {
      return false;
    }
    const { undo: undoSize } = this.editor.historySize();
    return undoSize > 0;
  }

  undo(): void {
    this.editor?.undo();
  }

  /**
   * @returns `true` if there is history to redo
   */
  get canRedo(): boolean {
    this.observeEditorChanges();
    if (!this.editor) {
      return false;
    }
    const { redo: redoSize } = this.editor.historySize();
    return redoSize > 0;
  }

  redo(): void {
    this.editor?.redo();
  }

  toggleLineNumbers(): void {
    this.showLineNumbers = !this.showLineNumbers;
  }
}
