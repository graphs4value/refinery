/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import CloudIcon from '@mui/icons-material/Cloud';
import CloudOffIcon from '@mui/icons-material/CloudOff';
import SyncIcon from '@mui/icons-material/Sync';
import SyncProblemIcon from '@mui/icons-material/SyncProblem';
import IconButton from '@mui/material/IconButton';
import { keyframes, styled } from '@mui/material/styles';
import { observer } from 'mobx-react-lite';

import type EditorStore from './EditorStore';

const rotateKeyframe = keyframes`
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(-360deg);
  }
`;

const AnimatedSyncIcon = styled(SyncIcon)`
  animation: ${rotateKeyframe} 1.4s linear infinite;
`;

export default observer(function ConnectButton({
  editorStore,
}: {
  editorStore: EditorStore | undefined;
}): JSX.Element {
  if (
    editorStore !== undefined &&
    (editorStore.opening || editorStore.opened)
  ) {
    return (
      <IconButton
        onClick={() => editorStore.disconnect()}
        aria-label="Disconnect"
        color="inherit"
      >
        {editorStore.opening ? (
          <AnimatedSyncIcon fontSize="small" />
        ) : (
          <CloudIcon fontSize="small" />
        )}
      </IconButton>
    );
  }

  let disconnectedIcon: JSX.Element;
  if (editorStore === undefined) {
    disconnectedIcon = <SyncIcon fontSize="small" />;
  } else if (editorStore.connectionErrors.length > 0) {
    disconnectedIcon = <SyncProblemIcon fontSize="small" />;
  } else {
    disconnectedIcon = <CloudOffIcon fontSize="small" />;
  }

  return (
    <IconButton
      disabled={editorStore === undefined}
      onClick={() => editorStore?.connect()}
      aria-label="Connect"
      color="inherit"
    >
      {disconnectedIcon}
    </IconButton>
  );
});
