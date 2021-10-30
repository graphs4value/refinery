import { observer } from 'mobx-react-lite';
import Button from '@mui/material/Button';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import React from 'react';

import { useRootStore } from '../RootStore';

const GENERATE_LABEL = 'Generate';

export const GenerateButton = observer(() => {
  const { editorStore } = useRootStore();
  const { errorCount, warningCount } = editorStore;

  const diagnostics: string[] = [];
  if (errorCount > 0) {
    diagnostics.push(`${errorCount} error${errorCount === 1 ? '' : 's'}`);
  }
  if (warningCount > 0) {
    diagnostics.push(`${warningCount} warning${warningCount === 1 ? '' : 's'}`);
  }
  const summary = diagnostics.join(' and ');

  if (errorCount > 0) {
    return (
      <Button
        variant="outlined"
        color="error"
        onClick={() => editorStore.toggleLintPanel()}
      >
        {summary}
      </Button>
    );
  }

  return (
    <Button
      variant="outlined"
      color={warningCount > 0 ? 'warning' : 'primary'}
      startIcon={<PlayArrowIcon />}
    >
      {summary === '' ? GENERATE_LABEL : `${GENERATE_LABEL} (${summary})`}
    </Button>
  );
});
