/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import CloseIcon from '@mui/icons-material/Close';
import DialogTitle from '@mui/material/DialogTitle';
import IconButton from '@mui/material/IconButton';
import { styled } from '@mui/material/styles';

const DialogTitleBarRoot = styled('div', {
  name: 'DialogTitleBar-Root',
})(({ theme }) => ({
  alignItems: 'center',
  borderBottom: `1px solid ${theme.palette.divider}`,
  display: 'flex',
  appRegion: 'drag',
  minHeight: '57px',
  padding: theme.spacing(1),
  paddingLeft: theme.spacing(2),
  '.MuiDialogTitle-root': {
    flexGrow: 1,
    padding: 0,
  },
  '.MuiIconButton-root': {
    flexGrow: 0,
    flexShrink: 0,
    marginLeft: theme.spacing(2),
  },
}));

export default function DialogTitleBar({
  close,
  closeDisabled = false,
  title,
}: {
  close?: (() => void) | undefined;
  closeDisabled?: boolean;
  title: string;
}): React.ReactElement {
  return (
    <DialogTitleBarRoot>
      <DialogTitle>{title}</DialogTitle>
      {close !== undefined && (
        <IconButton
          aria-label="Close"
          disabled={closeDisabled}
          onClick={close}
          sx={{ appRegion: 'no-drag' }}
        >
          <CloseIcon />
        </IconButton>
      )}
    </DialogTitleBarRoot>
  );
}
