/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import LockIcon from '@mui/icons-material/Lock';
import LockOpenIcon from '@mui/icons-material/LockOpen';
import { observer } from 'mobx-react-lite';

import AnimatedButton from './AnimatedButton';
import type EditorStore from './EditorStore';

function ConcretizeButton({
  editorStore,
}: {
  editorStore: EditorStore | undefined;
}): React.ReactNode {
  if (editorStore === undefined) {
    return null;
  }

  const { concretize } = editorStore;

  return (
    <AnimatedButton
      role="switch"
      aria-checked={concretize}
      aria-label="Calculate closed world interpretation"
      color={concretize ? 'inherit' : 'dim'}
      startIcon={concretize ? <LockIcon /> : <LockOpenIcon />}
      onClick={() => editorStore.toggleConcretize()}
      disabled={
        editorStore.selectedGeneratedModel !== undefined || !editorStore.opened
      }
    >
      {concretize ? 'Concrete' : 'Partial'}
    </AnimatedButton>
  );
}

export default observer(ConcretizeButton);
