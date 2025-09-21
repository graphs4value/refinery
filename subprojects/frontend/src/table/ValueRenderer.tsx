/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import CancelIcon from '@mui/icons-material/Cancel';
import LabelIcon from '@mui/icons-material/Label';
import LabelOutlinedIcon from '@mui/icons-material/LabelOutlined';
import SvgIcon from '@mui/material/SvgIcon';
import { styled } from '@mui/material/styles';

import NumberSVG from '../graph/dot_filled.svg?react';
import NumberOutlinedSVG from '../graph/dot_outlined.svg?react';
import {
  type Value,
  extractValue,
  extractValueColor,
} from '../graph/valueUtils';

const Label = styled('div', {
  name: 'ValueRenderer-Label',
  shouldForwardProp: (prop) => prop !== 'value',
})<{
  value: 'true' | 'unknown' | 'error';
  concretize: boolean;
}>(({ theme, value, concretize }) => ({
  display: 'flex',
  alignItems: 'center',
  ...(value === 'unknown'
    ? {
        color: theme.palette.text.secondary,
      }
    : {}),
  ...(value === 'error'
    ? {
        color: concretize ? theme.palette.info.main : theme.palette.error.main,
      }
    : {}),
  '& svg': {
    marginRight: theme.spacing(0.5),
  },
}));

export default function ValueRenderer({
  concretize,
  value,
  attribute,
}: {
  concretize: boolean;
  value: Value | undefined;
  attribute: boolean | undefined;
}): React.ReactNode {
  const color = extractValueColor(value);
  let icon: React.ReactNode;
  switch (color) {
    case 'true':
      if (attribute) {
        icon = (
          <SvgIcon component={NumberSVG} inheritViewBox fontSize="small" />
        );
      } else {
        icon = <LabelIcon fontSize="small" />;
      }
      break;
    case 'error':
      icon = <CancelIcon fontSize="small" />;
      break;
    default:
      if (attribute) {
        icon = (
          <SvgIcon
            component={NumberOutlinedSVG}
            inheritViewBox
            fontSize="small"
          />
        );
      } else {
        icon = <LabelOutlinedIcon fontSize="small" />;
      }
      break;
  }
  return (
    <Label concretize={concretize} value={color}>
      {icon} {extractValue(value)}
    </Label>
  );
}
