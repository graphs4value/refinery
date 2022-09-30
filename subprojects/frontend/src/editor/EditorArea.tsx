import Box from '@mui/material/Box';
import { useTheme } from '@mui/material/styles';
import { observer } from 'mobx-react-lite';
import React, { useCallback, useEffect, useState } from 'react';

import EditorAreaDecorations from './EditorAreaDecorations';
import type EditorStore from './EditorStore';
import EditorTheme from './EditorTheme';

export default observer(function EditorArea({
  editorStore,
}: {
  editorStore: EditorStore;
}): JSX.Element {
  const {
    palette: { mode: paletteMode },
  } = useTheme();
  const [parent, setParent] = useState<HTMLElement | undefined>();
  const [scroller, setScroller] = useState<HTMLElement | undefined>();

  useEffect(
    () => editorStore.setDarkMode(paletteMode === 'dark'),
    [editorStore, paletteMode],
  );

  const parentRef = useCallback(
    (value: HTMLElement | null) => setParent(value ?? undefined),
    [],
  );

  const editorParentRef = useCallback(
    (editorParent: HTMLDivElement | null) => {
      editorStore.setEditorParent(editorParent ?? undefined);
      setScroller(editorStore.view?.scrollDOM);
    },
    [editorStore, setScroller],
  );

  return (
    <Box
      ref={parentRef}
      position="relative"
      flexGrow={1}
      flexShrink={1}
      overflow="auto"
    >
      <EditorTheme
        showLineNumbers={editorStore.showLineNumbers}
        showActiveLine={!editorStore.hasSelection}
        ref={editorParentRef}
      />
      <EditorAreaDecorations parent={parent} scroller={scroller} />
    </Box>
  );
});
