/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import Button from '@mui/material/Button';
import { styled } from '@mui/material/styles';
import React from 'react';

import DialogActionBar from './DialogActionBar';
import DialogTitleBar from './DialogTitleBar';

const SlideInDialogRoot = styled('div', {
  name: 'SlideInDialog-Root',
})(({ theme }) => {
  return {
    maxHeight: '100%',
    maxWidth: '100%',
    overflow: 'hidden',
    display: 'flex',
    flexDirection: 'column',
    '.MuiFormControlLabel-root': {
      marginLeft: 0,
      paddingTop: theme.spacing(1),
      paddingLeft: theme.spacing(1),
      '& + .MuiFormControlLabel-root': {
        paddingTop: 0,
      },
    },
  };
});

export default function SlideInDialog({
  close,
  dialog,
  title,
  buttons,
  children,
}: {
  close: () => void;
  dialog?: boolean;
  title: string;
  buttons: React.ReactNode | ((close: () => void) => React.ReactNode);
  children?: React.ReactNode;
}): React.ReactElement {
  return (
    <SlideInDialogRoot>
      {dialog && <DialogTitleBar close={close} title={title} />}
      {children}
      <DialogActionBar
        divider={dialog ?? false}
        sx={dialog ? { mt: 2 } : undefined}
      >
        {typeof buttons === 'function' ? buttons(close) : buttons}
        {!dialog && (
          <Button color="inherit" onClick={close}>
            Close
          </Button>
        )}
      </DialogActionBar>
    </SlideInDialogRoot>
  );
}
