/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import LockIcon from '@mui/icons-material/Lock';
import MeetingRoomOutlinedIcon from '@mui/icons-material/MeetingRoomOutlined';
import { observer } from 'mobx-react-lite';

import AnimatedButton from './AnimatedButton';
import type EditorStore from './EditorStore';

function ConcretizeButton({
  editorStore,
  abbreviate,
}: {
  editorStore: EditorStore | undefined;
  abbreviate?: boolean | undefined;
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
      color={concretize ? 'inherit' : 'darkened'}
      startIcon={concretize ? <LockIcon /> : <MeetingRoomOutlinedIcon />}
      onClick={() => editorStore.toggleConcretize()}
      disabled={
        editorStore.selectedGeneratedModel !== undefined || !editorStore.opened
      }
    >
      {concretize ? 'Closed' : 'Open'}
      {abbreviate ? '' : ' world'}
    </AnimatedButton>
  );
}

export default observer(ConcretizeButton);
