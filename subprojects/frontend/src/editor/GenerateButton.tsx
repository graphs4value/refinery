import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import Button from '@mui/material/Button';
import { observer } from 'mobx-react-lite';
import React from 'react';

import { useRootStore } from '../RootStore';

const GENERATE_LABEL = 'Generate';

function GenerateButton(): JSX.Element {
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
      <Button color="error" onClick={() => editorStore.nextDiagnostic()}>
        {summary}
      </Button>
    );
  }

  return (
    <Button
      color={warningCount > 0 ? 'warning' : 'primary'}
      startIcon={<PlayArrowIcon />}
    >
      {summary === '' ? GENERATE_LABEL : `${GENERATE_LABEL} (${summary})`}
    </Button>
  );
}

export default observer(GenerateButton);
