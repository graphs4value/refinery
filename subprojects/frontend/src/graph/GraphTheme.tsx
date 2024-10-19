/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import {
  alpha,
  styled,
  type CSSObject,
  type Theme,
} from '@mui/material/styles';
import { type HCLColor, lch } from 'd3-color';
import { range } from 'lodash-es';

import obfuscateColor from './obfuscateColor';

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

function saturate(color: HCLColor | string, saturation: number): string {
  if (saturation === 1) {
    return typeof color === 'string' ? color : color.formatRgb();
  }
  const parsedColor = typeof color === 'string' ? lch(color) : color;
  parsedColor.c *= saturation;
  return parsedColor.formatRgb();
}

function createTypeHashStyles(
  theme: Theme,
  colorNodes: boolean,
  typeHashes: string[],
  saturation = 1,
): CSSObject {
  if (!colorNodes) {
    return {};
  }
  const result: CSSObject = {};
  range(theme.palette.highlight.typeHash.length).forEach((i) => {
    result[`.node-typeHash-${obfuscateColor(i.toString(10))} .node-header`] = {
      fill: saturate(
        theme.palette.highlight.typeHash[i]?.box ?? '#fff',
        saturation,
      ),
    };
  });
  typeHashes.forEach((typeHash) => {
    let color = lch(`#${typeHash}`);
    if (theme.palette.mode === 'dark') {
      color = color.darker();
      if (color.l > 50) {
        color.l = 50;
      }
    } else if (color.l < 70) {
      color.l = 70;
    }
    result[`.node-typeHash-_${obfuscateColor(typeHash)} .node-header`] = {
      fill: saturate(color, saturation),
    };
  });
  return result;
}

export function createGraphTheme({
  theme,
  colorNodes,
  hexTypeHashes,
  concretize,
  useOpacity,
}: {
  theme: Theme;
  colorNodes: boolean;
  hexTypeHashes: string[];
  concretize: boolean;
  useOpacity?: boolean;
}): CSSObject {
  const shadowAlapha = theme.palette.mode === 'dark' ? 0.32 : 0.24;
  const errorColor = concretize ? theme.palette.info : theme.palette.error;

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
    '.node-shadow.node-bg': useOpacity
      ? {
          // Inkscape can't handle RGBA in exported SVG.
          fill: theme.palette.text.primary,
          opacity: shadowAlapha,
        }
      : {
          // But using `opacity` with the transition animation leads to flashing shadows,
          // so we still use RGBA whenever possible.
          fill: alpha(theme.palette.text.primary, shadowAlapha),
        },
    '.node-exists-UNKNOWN .node-outline': {
      strokeDasharray: '5 2',
    },
    ...createTypeHashStyles(theme, colorNodes, hexTypeHashes),
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
    ...createEdgeColor('ERROR', errorColor.main),
    '.icon-TRUE': {
      fill: theme.palette.text.primary,
    },
    '.icon-UNKNOWN': {
      fill: theme.palette.text.secondary,
    },
    '.icon-ERROR': {
      fill: errorColor.main,
    },
    'text.label-UNKNOWN': {
      fill: theme.palette.text.secondary,
    },
    'text.label-ERROR': {
      fill: errorColor.main,
    },
    '.node-exists-FALSE': {
      'text:not(.label-ERROR)': {
        fill: theme.palette.text.secondary,
      },
      '.node-outline': {
        stroke: theme.palette.text.secondary,
        strokeDasharray: '2 4',
      },
      '.node-header': {
        fill: theme.palette.background.default,
      },
      '.icon-TRUE': {
        fill: theme.palette.text.secondary,
      },
    },
    '.node-exists-ERROR': {
      '.node-outline': {
        stroke: errorColor.main,
      },
      '.node-header': {
        fill: theme.palette.background.default,
      },
    },
  };
}

export default styled('div', {
  name: 'GraphTheme',
  shouldForwardProp: (prop) =>
    prop !== 'colorNodes' && prop !== 'hexTypeHashes' && prop !== 'concretize',
})<{ colorNodes: boolean; hexTypeHashes: string[]; concretize: boolean }>(
  (args) => ({
    '& svg': {
      userSelect: 'none',
      ...createGraphTheme(args),
    },
    '&.simplified svg': {
      'text, .edge-arrow, .icon, .node-shadow.node-bg': {
        display: 'none !important',
      },
      '.edge-line, .node-exists-UNKNOWN .node-outline, .node-exists-FALSE .node-outline':
        {
          strokeDasharray: 'none !important',
        },
    },
    '.node-bg, .node-header': {
      transition: args.theme.transitions.create('fill', {
        duration: args.theme.transitions.duration.short,
      }),
    },
    '&.dimmed svg': {
      '.node .node-bg:not(.node-shadow)': {
        fill: args.theme.palette.outer.disabled,
        '@media (prefers-reduced-motion: reduce)': {
          fill: args.theme.palette.background.default,
        },
      },
      '.node-header': {
        fill: saturate(
          args.theme.palette.mode === 'dark'
            ? args.theme.palette.primary.dark
            : args.theme.palette.primary.light,
          0.6,
        ),
      },
      ...createTypeHashStyles(
        args.theme,
        args.colorNodes,
        args.hexTypeHashes,
        0.6,
      ),
      '@media (prefers-reduced-motion: reduce)': {
        '.node-header': {
          fill:
            args.theme.palette.mode === 'dark'
              ? args.theme.palette.primary.dark
              : args.theme.palette.primary.light,
        },
        ...createTypeHashStyles(
          args.theme,
          args.colorNodes,
          args.hexTypeHashes,
        ),
      },
    },
  }),
);
