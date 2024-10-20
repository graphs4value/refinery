/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { Theme } from '@mui/material';
import { hcl } from 'd3-color';

export default function typeHashTextColor(
  typeHash: string,
  theme: Theme,
): string {
  if (typeHash.length === 0) {
    return theme.palette.text.primary;
  }
  if (typeHash[0] !== '#') {
    let index: number;
    try {
      index = parseInt(typeHash, 10);
    } catch {
      return theme.palette.text.primary;
    }
    return (
      theme.palette.highlight.typeHash[index]?.text ??
      theme.palette.text.primary
    );
  }
  let color = hcl(typeHash);
  if (theme.palette.mode === 'dark') {
    color = color.brighter();
    if (color.l < 60) {
      color.l = 60;
    }
  } else {
    color = color.darker();
    if (color.l > 60) {
      color.l = 60;
    }
  }
  return color.formatRgb();
}
