/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import DangerousOutlinedIcon from '@mui/icons-material/DangerousOutlined';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import Button from '@mui/material/Button';
import type { SxProps, Theme } from '@mui/material/styles';
import { observer } from 'mobx-react-lite';

import AnimatedButton from './AnimatedButton';
import type EditorStore from './EditorStore';

const GENERATE_LABEL = 'Generate';

const GenerateButton = observer(function GenerateButton({
  editorStore,
  hideWarnings,
  sx,
}: {
  editorStore: EditorStore | undefined;
  hideWarnings?: boolean | undefined;
  sx?: SxProps<Theme> | undefined;
}): JSX.Element {
  if (editorStore === undefined) {
    return (
      <Button
        color="inherit"
        className="rounded shaded"
        disabled
        {...(sx === undefined ? {} : { sx })}
      >
        Loading&hellip;
      </Button>
    );
  }

  const { errorCount, warningCount } = editorStore;

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
        startIcon={<DangerousOutlinedIcon />}
        {...(sx === undefined ? {} : { sx })}
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
      {...(sx === undefined ? {} : { sx })}
    >
      {summary === '' ? GENERATE_LABEL : `${GENERATE_LABEL} (${summary})`}
    </AnimatedButton>
  );
});

GenerateButton.defaultProps = {
  hideWarnings: false,
  sx: undefined,
};

export default GenerateButton;
