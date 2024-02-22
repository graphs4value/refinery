/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import cancelSVG from '@material-icons/svg/svg/cancel/baseline.svg?raw';
import labelSVG from '@material-icons/svg/svg/label/baseline.svg?raw';
import labelOutlinedSVG from '@material-icons/svg/svg/label/outline.svg?raw';
import {
  alpha,
  styled,
  type CSSObject,
  type Theme,
} from '@mui/material/styles';
import { range } from 'lodash-es';

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
      '.edge-line': {
        stroke,
      },
      '.edge-arrow': {
        fill: fill ?? stroke,
      },
    },
  };
}

function createTypeHashStyles(theme: Theme, colorNodes: boolean): CSSObject {
  if (!colorNodes) {
    return {};
  }
  const result: CSSObject = {};
  range(theme.palette.highlight.typeHash.length).forEach((i) => {
    result[`.node-typeHash-${i} .node-header`] = {
      fill: theme.palette.highlight.typeHash[i]?.box,
    };
  });
  return result;
}

function iconStyle(
  svg: string,
  color: string,
  noEmbedIcons?: boolean,
): CSSObject {
  if (noEmbedIcons) {
    return {
      fill: color,
    };
  }
  return {
    maskImage: svgURL(svg),
    background: color,
  };
}

export function createGraphTheme({
  theme,
  colorNodes,
  noEmbedIcons,
}: {
  theme: Theme;
  colorNodes: boolean;
  noEmbedIcons?: boolean;
}): CSSObject {
  const shadowAlapha = theme.palette.mode === 'dark' ? 0.32 : 0.24;

  return {
    '.node': {
      '& text': {
        fontFamily: theme.typography.fontFamily,
        fill: theme.palette.text.primary,
      },
      '.node-outline': {
        stroke: theme.palette.text.primary,
      },
      '.node-header': {
        fill:
          theme.palette.mode === 'dark'
            ? theme.palette.primary.dark
            : theme.palette.primary.light,
      },
      '.node-bg': {
        fill: theme.palette.background.default,
      },
    },
    '.node-INDIVIDUAL .node-outline': {
      strokeWidth: 2,
    },
    '.node-shadow.node-bg': noEmbedIcons
      ? {
          // Inkscape can't handle opacity in exported SVG.
          fill: theme.palette.text.primary,
          opacity: shadowAlapha,
        }
      : {
          fill: alpha(theme.palette.text.primary, shadowAlapha),
        },
    '.node-exists-UNKNOWN .node-outline': {
      strokeDasharray: '5 2',
    },
    ...createTypeHashStyles(theme, colorNodes),
    '.edge': {
      '& text': {
        fontFamily: theme.typography.fontFamily,
        fill: theme.palette.text.primary,
      },
      '.edge-line': {
        stroke: theme.palette.text.primary,
      },
      '.edge-arrow': {
        fill: theme.palette.text.primary,
      },
    },
    ...createEdgeColor('UNKNOWN', theme.palette.text.secondary, 'none'),
    ...createEdgeColor('ERROR', theme.palette.error.main),
    ...(noEmbedIcons
      ? {}
      : {
          '.icon': {
            maskSize: '12px 12px',
            maskPosition: '50% 50%',
            maskRepeat: 'no-repeat',
            width: '100%',
            height: '100%',
          },
        }),
    '.icon-TRUE': iconStyle(labelSVG, theme.palette.text.primary, noEmbedIcons),
    '.icon-UNKNOWN': iconStyle(
      labelOutlinedSVG,
      theme.palette.text.secondary,
      noEmbedIcons,
    ),
    '.icon-ERROR': iconStyle(cancelSVG, theme.palette.error.main, noEmbedIcons),
    'text.label-UNKNOWN': {
      fill: theme.palette.text.secondary,
    },
    'text.label-ERROR': {
      fill: theme.palette.error.main,
    },
  };
}

export default styled('div', {
  name: 'GraphTheme',
})<{ colorNodes: boolean }>((args) => ({
  '& svg': {
    userSelect: 'none',
    ...createGraphTheme(args),
  },
}));
