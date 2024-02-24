/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import CloseIcon from '@mui/icons-material/Close';
import Button from '@mui/material/Button';
import IconButton from '@mui/material/IconButton';
import Typography from '@mui/material/Typography';
import { styled } from '@mui/material/styles';
import React, { useId } from 'react';

const SlideInDialogRoot = styled('div', {
  name: 'SlideInDialog-Root',
  shouldForwardProp: (propName) => propName !== 'dialog',
})<{ dialog: boolean }>(({ theme, dialog }) => {
  return {
    maxHeight: '100%',
    maxWidth: '100%',
    overflow: 'hidden',
    display: 'flex',
    flexDirection: 'column',
    '.SlideInDialog-title': {
      display: 'flex',
      flexDirection: 'row',
      alignItems: 'center',
      padding: theme.spacing(1),
      paddingLeft: theme.spacing(2),
      borderBottom: `1px solid ${theme.palette.divider}`,
      '& h2': {
        flexGrow: 1,
      },
      '.MuiIconButton-root': {
        flexGrow: 0,
        flexShrink: 0,
        marginLeft: theme.spacing(2),
      },
    },
    '.MuiFormControlLabel-root': {
      marginLeft: 0,
      paddingTop: theme.spacing(1),
      paddingLeft: theme.spacing(1),
      '& + .MuiFormControlLabel-root': {
        paddingTop: 0,
      },
    },
    '.SlideInDialog-buttons': {
      padding: theme.spacing(1),
      display: 'flex',
      flexDirection: 'row',
      justifyContent: 'flex-end',
      ...(dialog
        ? {
            marginTop: theme.spacing(1),
            borderTop: `1px solid ${theme.palette.divider}`,
          }
        : {}),
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
}): JSX.Element {
  const titleId = useId();

  return (
    <SlideInDialogRoot
      dialog={dialog ?? SlideInDialog.defaultProps.dialog}
      aria-labelledby={dialog ? titleId : undefined}
    >
      {dialog && (
        <div className="SlideInDialog-title">
          <Typography variant="h6" component="h2" id={titleId}>
            {title}
          </Typography>
          <IconButton aria-label="Close" onClick={close}>
            <CloseIcon />
          </IconButton>
        </div>
      )}
      {children}
      <div className="SlideInDialog-buttons">
        {typeof buttons === 'function' ? buttons(close) : buttons}
        {!dialog && (
          <Button color="inherit" onClick={close}>
            Close
          </Button>
        )}
      </div>
    </SlideInDialogRoot>
  );
}

SlideInDialog.defaultProps = {
  dialog: false,
  children: undefined,
};
