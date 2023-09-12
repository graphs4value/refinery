/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import cancelSVG from '@material-icons/svg/svg/cancel/baseline.svg?raw';
import labelSVG from '@material-icons/svg/svg/label/baseline.svg?raw';
import labelOutlinedSVG from '@material-icons/svg/svg/label/outline.svg?raw';
import { alpha, styled, type CSSObject } from '@mui/material/styles';

import svgURL from '../utils/svgURL';

function createEdgeColor(
  suffix: string,
  stroke: string,
  fill?: string,
): CSSObject {
  return {
    [`.edge-${suffix}`]: {
      '& text': {
        fill: stroke,
      },
      '& [stroke="black"]': {
        stroke,
      },
      '& [fill="black"]': {
        fill: fill ?? stroke,
      },
    },
  };
}

export default styled('div', {
  name: 'GraphTheme',
})(({ theme }) => ({
  '& svg': {
    userSelect: 'none',
    '.node': {
      '& text': {
        fontFamily: theme.typography.fontFamily,
        fill: theme.palette.text.primary,
      },
      '& [stroke="black"]': {
        stroke: theme.palette.text.primary,
      },
      '& [fill="green"]': {
        fill:
          theme.palette.mode === 'dark'
            ? theme.palette.primary.dark
            : theme.palette.primary.light,
      },
      '& [fill="white"]': {
        fill: theme.palette.background.default,
      },
    },
    '.node-INDIVIDUAL': {
      '& [stroke="black"]': {
        strokeWidth: 2,
      },
    },
    '.node-shadow[fill="white"]': {
      fill: alpha(
        theme.palette.text.primary,
        theme.palette.mode === 'dark' ? 0.32 : 0.24,
      ),
    },
    '.node-exists-UNKNOWN [stroke="black"]': {
      strokeDasharray: '5 2',
    },
    '.edge': {
      '& text': {
        fontFamily: theme.typography.fontFamily,
        fill: theme.palette.text.primary,
      },
      '& [stroke="black"]': {
        stroke: theme.palette.text.primary,
      },
      '& [fill="black"]': {
        fill: theme.palette.text.primary,
      },
    },
    ...createEdgeColor('UNKNOWN', theme.palette.text.secondary, 'none'),
    ...createEdgeColor('ERROR', theme.palette.error.main),
    '.icon': {
      maskSize: '12px 12px',
      maskPosition: '50% 50%',
      maskRepeat: 'no-repeat',
      width: '100%',
      height: '100%',
    },
    '.icon-TRUE': {
      maskImage: svgURL(labelSVG),
      background: theme.palette.text.primary,
    },
    '.icon-UNKNOWN': {
      maskImage: svgURL(labelOutlinedSVG),
      background: theme.palette.text.secondary,
    },
    '.icon-ERROR': {
      maskImage: svgURL(cancelSVG),
      background: theme.palette.error.main,
    },
    'text.label-UNKNOWN': {
      fill: theme.palette.text.secondary,
    },
    'text.label-ERROR': {
      fill: theme.palette.error.main,
    },
  },
}));
