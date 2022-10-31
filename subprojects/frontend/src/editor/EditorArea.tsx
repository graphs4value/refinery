import Box from '@mui/material/Box';
import { useTheme } from '@mui/material/styles';
import { observer } from 'mobx-react-lite';
import React, { useCallback, useEffect } from 'react';

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
    <Box flexGrow={1} flexShrink={1} overflow="auto">
      <EditorTheme
        showLineNumbers={editorStore.showLineNumbers}
        showActiveLine={!editorStore.hasSelection}
        ref={editorParentRef}
      />
    </Box>
  );
});
