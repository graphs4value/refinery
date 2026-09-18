/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import ShareIcon from '@mui/icons-material/Share';
import IconButton from '@mui/material/IconButton';
import { observer } from 'mobx-react-lite';

import Tooltip from '../Tooltip';
import type EditorStore from '../editor/EditorStore';

const ShareButton = observer(function ShareButton({
  editorStore,
  dialogID,
}: {
  editorStore: EditorStore | undefined;
  dialogID: string;
}): React.ReactElement {
  const dialogOpen = editorStore?.shareDialogOpen !== undefined;
  return (
    <Tooltip title="Share">
      <IconButton
        disabled={editorStore === undefined}
        onClick={() => editorStore?.openShareDialog('toolbarButton')}
        aria-haspopup="dialog"
        aria-expanded={dialogOpen}
        aria-controls={dialogOpen ? dialogID : undefined}
        color="inherit"
      >
        <ShareIcon fontSize="small" />
      </IconButton>
    </Tooltip>
  );
});

export default ShareButton;
