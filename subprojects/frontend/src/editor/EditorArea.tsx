import Portal from '@mui/material/Portal';
import { useTheme } from '@mui/material/styles';
import { observer } from 'mobx-react-lite';
import React, { useCallback, useEffect } from 'react';

import { useRootStore } from '../RootStore';

import EditorTheme from './EditorTheme';
import SearchToolbar from './SearchToolbar';

function EditorArea(): JSX.Element {
  const { editorStore } = useRootStore();
  const { searchPanel: searchPanelStore } = editorStore;
  const { element: searchPanelContainer } = searchPanelStore;
  const {
    palette: { mode: paletteMode },
  } = useTheme();

  useEffect(
    () => editorStore.setDarkMode(paletteMode === 'dark'),
    [editorStore, paletteMode],
  );

  const editorParentRef = useCallback(
    (editorParent: HTMLDivElement | null) => {
      editorStore.setEditorParent(editorParent ?? undefined);
    },
    [editorStore],
  );

  return (
    <EditorTheme
      showLineNumbers={editorStore.showLineNumbers}
      ref={editorParentRef}
    >
      {searchPanelContainer !== undefined && (
        <Portal container={searchPanelContainer}>
          <SearchToolbar store={searchPanelStore} />
        </Portal>
      )}
    </EditorTheme>
  );
}

export default observer(EditorArea);
