import { Command, EditorView } from '@codemirror/view';
import { closeSearchPanel, openSearchPanel } from '@codemirror/search';
import { closeLintPanel, openLintPanel } from '@codemirror/lint';
import { observer } from 'mobx-react-lite';
import React, { useEffect, useRef, useState } from 'react';

import { EditorParent } from './EditorParent';
import { getLogger } from '../logging';
import { useRootStore } from '../RootStore';

const log = getLogger('EditorArea');

function usePanel(
  label: string,
  stateToSet: boolean,
  editorView: EditorView | null,
  openCommand: Command,
  closeCommand: Command,
) {
  const [cachedViewState, setCachedViewState] = useState<boolean>(false);
  useEffect(() => {
    if (editorView === null || cachedViewState === stateToSet) {
      return;
    }
    const success = stateToSet ? openCommand(editorView) : closeCommand(editorView);
    if (!success) {
      log.error(
        'Failed to synchronize',
        label,
        'panel state - store state:',
        cachedViewState,
        'view state:',
        stateToSet,
      );
    }
    setCachedViewState(stateToSet);
  }, [
    stateToSet,
    editorView,
    cachedViewState,
    label,
    openCommand,
    closeCommand,
  ]);
  return setCachedViewState;
}

export const EditorArea = observer(() => {
  const { editorStore } = useRootStore();
  const editorParentRef = useRef<HTMLDivElement | null>(null);
  const [editorViewState, setEditorViewState] = useState<EditorView | null>(null);

  const setSearchPanelOpen = usePanel(
    'search',
    editorStore.showSearchPanel,
    editorViewState,
    openSearchPanel,
    closeSearchPanel,
  );

  const setLintPanelOpen = usePanel(
    'lint',
    editorStore.showLintPanel,
    editorViewState,
    openLintPanel,
    closeLintPanel,
  );

  useEffect(() => {
    if (editorParentRef.current === null) {
      // Nothing to clean up.
      return () => {};
    }

    const editorView = new EditorView({
      state: editorStore.state,
      parent: editorParentRef.current,
      dispatch: (transaction) => {
        editorStore.onTransaction(transaction);
        editorView.update([transaction]);
        if (editorView.state !== editorStore.state) {
          log.error(
            'Failed to synchronize editor state - store state:',
            editorStore.state,
            'view state:',
            editorView.state,
          );
        }
      },
    });
    setEditorViewState(editorView);
    setSearchPanelOpen(false);
    setLintPanelOpen(false);
    // `dispatch` is bound to the view instance,
    // so it does not have to be called as a method.
    // eslint-disable-next-line @typescript-eslint/unbound-method
    editorStore.updateDispatcher(editorView.dispatch);
    log.info('Editor created');

    return () => {
      editorStore.updateDispatcher(null);
      editorView.destroy();
      log.info('Editor destroyed');
    };
  }, [
    editorParentRef,
    editorStore,
    setSearchPanelOpen,
    setLintPanelOpen,
  ]);

  return (
    <EditorParent
      className="dark"
      sx={{
        '.cm-lineNumbers': editorStore.showLineNumbers ? {} : {
          display: 'none !important',
        },
      }}
      ref={editorParentRef}
    />
  );
});
