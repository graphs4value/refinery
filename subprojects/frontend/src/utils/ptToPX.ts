/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

// Graphviz emits coordinates in points (1/72in), but the browser and our
// raster outputs treat SVG user units as CSS pixels (1/96in).
const PX_PER_PT = 4 / 3;

export default function ptToPX(pt: number): number {
  return pt * PX_PER_PT;
}

export function pxToPT(px: number): number {
  return px / PX_PER_PT;
}
