/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import CancelIcon from '@mui/icons-material/Cancel';
import CloseIcon from '@mui/icons-material/Close';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import { observer } from 'mobx-react-lite';

import AnimatedButton from './AnimatedButton';
import type EditorStore from './EditorStore';

const GENERATE_LABEL = 'Generate';

const GenerateButton = observer(function GenerateButton({
  editorStore,
  hideWarnings,
}: {
  editorStore: EditorStore | undefined;
  hideWarnings?: boolean | undefined;
}): JSX.Element {
  if (editorStore === undefined) {
    return (
      <AnimatedButton color="inherit" disabled>
        Loading&hellip;
      </AnimatedButton>
    );
  }

  const {
    delayedErrors: { analyzing, errorCount, warningCount, semanticsError },
    generating,
  } = editorStore;

  if (analyzing) {
    return (
      <AnimatedButton color="inherit" disabled>
        Analyzing&hellip;
      </AnimatedButton>
    );
  }

  if (generating) {
    return (
      <AnimatedButton
        color="inherit"
        onClick={() => editorStore.cancelModelGeneration()}
        startIcon={<CloseIcon />}
      >
        Cancel
      </AnimatedButton>
    );
  }

  if (semanticsError !== undefined && editorStore.opened) {
    return (
      <AnimatedButton
        color="error"
        disabled
        startIcon={<CancelIcon />}
        sx={(theme) => ({
          '&.Mui-disabled': {
            color: `${theme.palette.error.main} !important`,
          },
        })}
      >
        Analysis error
      </AnimatedButton>
    );
  }

  const diagnostics: string[] = [];
  if (errorCount > 0) {
    diagnostics.push(`${errorCount} error${errorCount === 1 ? '' : 's'}`);
  }
  if (!(hideWarnings ?? false) && warningCount > 0) {
    diagnostics.push(`${warningCount} warning${warningCount === 1 ? '' : 's'}`);
  }
  const summary = diagnostics.join(' and ');

  if (errorCount > 0) {
    return (
      <AnimatedButton
        aria-label={`Select next diagnostic out of ${summary}`}
        onClick={() => editorStore.nextDiagnostic()}
        color="error"
        startIcon={<CancelIcon />}
      >
        {summary}
      </AnimatedButton>
    );
  }

  return (
    <AnimatedButton
      disabled={!editorStore.opened}
      color={warningCount > 0 ? 'warning' : 'primary'}
      startIcon={<PlayArrowIcon />}
      onClick={(event) => {
        if (event.shiftKey) {
          editorStore.startModelGeneration(1);
        } else {
          editorStore.startModelGeneration();
        }
      }}
    >
      {summary === '' ? GENERATE_LABEL : `${GENERATE_LABEL} (${summary})`}
    </AnimatedButton>
  );
});

GenerateButton.defaultProps = {
  hideWarnings: false,
};

export default GenerateButton;
