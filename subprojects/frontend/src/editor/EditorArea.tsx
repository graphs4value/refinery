import { useTheme } from '@mui/material/styles';
import { observer } from 'mobx-react-lite';
import React, { useCallback, useEffect } from 'react';

import { useRootStore } from '../RootStore';

import EditorTheme from './EditorTheme';

function EditorArea(): JSX.Element {
  const { editorStore } = useRootStore();
  const {
    palette: { mode: paletteMode },
  } = useTheme();

  useEffect(
    () => editorStore.setDarkMode(paletteMode === 'dark'),
    [editorStore, paletteMode],
  );

  const editorParentRef = useCallback(
    (editorParent: HTMLDivElement | null) => {
      editorStore.setEditorParent(editorParent);
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
