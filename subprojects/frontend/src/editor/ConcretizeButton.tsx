/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import LockIcon from '@mui/icons-material/Lock';
import LockOpenIcon from '@mui/icons-material/LockOpen';
import { observer } from 'mobx-react-lite';

import Tooltip from '../Tooltip';

import AnimatedButton from './AnimatedButton';
import type EditorStore from './EditorStore';

function ConcretizeButton({
  editorStore,
  hideLabel = false,
}: {
  editorStore: EditorStore | undefined;
  hideLabel?: boolean;
}): React.ReactNode {
  if (editorStore === undefined) {
    return null;
  }

  const generatedModel = editorStore.selectedGeneratedModel !== undefined;
  const concretize = generatedModel || editorStore.concretize;
  const icon = concretize ? <LockIcon /> : <LockOpenIcon />;
  const label = concretize ? 'Concrete' : 'Partial';

  const button = (
    <AnimatedButton
      role="switch"
      aria-checked={concretize}
      aria-label="Calculate closed world interpretation"
      color={concretize ? 'inherit' : 'dim'}
      startIcon={icon}
      sx={
        hideLabel
          ? {
              minWidth: 0,
              '& .MuiButton-startIcon': { margin: 0 },
            }
          : undefined
      }
      onClick={() => editorStore.toggleConcretize()}
      disabled={generatedModel || !editorStore.opened}
    >
      {!hideLabel && label}
    </AnimatedButton>
  );

  return hideLabel ? <Tooltip title={label}>{button}</Tooltip> : button;
}

export default observer(ConcretizeButton);
