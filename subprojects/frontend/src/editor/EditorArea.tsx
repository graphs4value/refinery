import { useTheme } from '@mui/material/styles';
import { observer } from 'mobx-react-lite';
import React, { useCallback, useEffect } from 'react';

import type EditorStore from './EditorStore';
import EditorTheme from './EditorTheme';

function EditorArea({
  editorStore,
}: {
  editorStore: EditorStore;
}): JSX.Element {
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
    />
  );
}

export default observer(EditorArea);
