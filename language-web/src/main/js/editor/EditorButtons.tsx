import { observer } from 'mobx-react-lite';
import Stack from '@mui/material/Stack';
import ToggleButton from '@mui/material/ToggleButton';
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup';
import FormatListNumberedIcon from '@mui/icons-material/FormatListNumbered';
import RedoIcon from '@mui/icons-material/Redo';
import UndoIcon from '@mui/icons-material/Undo';
import React from 'react';

import { useRootStore } from '../RootStore';

export const EditorButtons = observer(() => {
  const { editorStore } = useRootStore();

  return (
    <Stack
      direction="row"
      spacing={1}
    >
      <ToggleButtonGroup
        size="small"
      >
        <ToggleButton
          disabled={!editorStore.canUndo}
          onClick={() => editorStore.undo()}
          aria-label="Undo"
          value="undo"
        >
          <UndoIcon fontSize="small" />
        </ToggleButton>
        <ToggleButton
          disabled={!editorStore.canRedo}
          onClick={() => editorStore.redo()}
          aria-label="Redo"
          value="redo"
        >
          <RedoIcon fontSize="small" />
        </ToggleButton>
      </ToggleButtonGroup>
      <ToggleButton
        selected={editorStore.showLineNumbers}
        onChange={() => editorStore.toggleLineNumbers()}
        size="small"
        aria-label="Show line numbers"
        value="show-line-numbers"
      >
        <FormatListNumberedIcon fontSize="small" />
      </ToggleButton>
    </Stack>
  );
});
