/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import CancelIcon from '@mui/icons-material/Cancel';
import LabelIcon from '@mui/icons-material/Label';
import LabelOutlinedIcon from '@mui/icons-material/LabelOutlined';
import { styled } from '@mui/material/styles';

const Label = styled('div', {
  name: 'ValueRenderer-Label',
  shouldForwardProp: (prop) => prop !== 'value',
})<{
  value: 'TRUE' | 'UNKNOWN' | 'ERROR';
}>(({ theme, value }) => ({
  display: 'flex',
  alignItems: 'center',
  ...(value === 'UNKNOWN'
    ? {
        color: theme.palette.text.secondary,
      }
    : {}),
  ...(value === 'ERROR'
    ? {
        color: theme.palette.error.main,
      }
    : {}),
  '& svg': {
    marginRight: theme.spacing(0.5),
  },
}));

export default function ValueRenderer({
  value,
}: {
  value: string | undefined;
}): React.ReactNode {
  switch (value) {
    case 'TRUE':
      return (
        <Label value={value}>
          <LabelIcon fontSize="small" /> true
        </Label>
      );
    case 'UNKNOWN':
      return (
        <Label value={value}>
          <LabelOutlinedIcon fontSize="small" /> unknown
        </Label>
      );
    case 'ERROR':
      return (
        <Label value={value}>
          <CancelIcon fontSize="small" /> error
        </Label>
      );
    default:
      return value;
  }
}
