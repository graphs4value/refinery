import { Command, EditorView } from '@codemirror/view';
import { closeSearchPanel, openSearchPanel } from '@codemirror/search';
import { closeLintPanel, openLintPanel } from '@codemirror/lint';
import { observer } from 'mobx-react-lite';
import React, {
  useCallback,
  useEffect,
  useRef,
  useState,
} from 'react';

import { EditorParent } from './EditorParent';
import { useRootStore } from '../RootStore';
import { getLogger } from '../utils/logger';

const log = getLogger('editor.EditorArea');

function usePanel(
  panelId: string,
  stateToSet: boolean,
  editorView: EditorView | null,
  openCommand: Command,
  closeCommand: Command,
  closeCallback: () => void,
) {
  const [cachedViewState, setCachedViewState] = useState<boolean>(false);
  useEffect(() => {
    if (editorView === null || cachedViewState === stateToSet) {
      return;
    }
    if (stateToSet) {
      openCommand(editorView);
      const buttonQuery = `.cm-${panelId}.cm-panel button[name="close"]`;
      const closeButton = editorView.dom.querySelector(buttonQuery);
      if (closeButton) {
        log.debug('Addig close button callback to', panelId, 'panel');
        // We must remove the event listener added by CodeMirror from the button
        // that dispatches a transaction without going through `EditorStorre`.
        // Cloning a DOM node removes event listeners,
        // see https://stackoverflow.com/a/9251864
        const closeButtonWithoutListeners = closeButton.cloneNode(true);
        closeButtonWithoutListeners.addEventListener('click', (event) => {
          closeCallback();
          event.preventDefault();
        });
        closeButton.replaceWith(closeButtonWithoutListeners);
      } else {
        log.error('Opened', panelId, 'panel has no close button');
      }
    } else {
      closeCommand(editorView);
    }
    setCachedViewState(stateToSet);
  }, [
    stateToSet,
    editorView,
    cachedViewState,
    panelId,
    openCommand,
    closeCommand,
    closeCallback,
  ]);
  return setCachedViewState;
}

function fixCodeMirrorAccessibility(editorView: EditorView) {
  // Reported by Lighthouse 8.3.0.
  const { contentDOM } = editorView;
  contentDOM.removeAttribute('aria-expanded');
  contentDOM.setAttribute('aria-label', 'Code editor');
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
    useCallback(() => editorStore.setSearchPanelOpen(false), [editorStore]),
  );

  const setLintPanelOpen = usePanel(
    'panel-lint',
    editorStore.showLintPanel,
    editorViewState,
    openLintPanel,
    closeLintPanel,
    useCallback(() => editorStore.setLintPanelOpen(false), [editorStore]),
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
    fixCodeMirrorAccessibility(editorView);
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
