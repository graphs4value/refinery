/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { styled, type CSSObject } from '@mui/material/styles';

function createEdgeColor(suffix: string, color: string): CSSObject {
  return {
    [`& .edge-${suffix}`]: {
      '& text': {
        fill: color,
      },
      '& [stroke="black"]': {
        stroke: color,
      },
      '& [fill="black"]': {
        fill: color,
      },
    },
  };
}

export default styled('div', {
  name: 'GraphTheme',
})(({ theme }) => ({
  '& svg': {
    userSelect: 'none',
    '& .node': {
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
        stroke: theme.palette.background.default,
      },
    },
    '& .edge': {
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
    ...createEdgeColor('UNKNOWN', theme.palette.text.secondary),
    ...createEdgeColor('ERROR', theme.palette.error.main),
  },
}));
