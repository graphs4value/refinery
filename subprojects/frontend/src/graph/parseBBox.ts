/*
 * Copyright 2017, Magnus Jacobsson
 * Copyright 2023, The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *
 * This file Incorporates patches from the Refinery authors.
 *
 * Redistribution and use is only permitted if neither
 * the name of the copyright holder Magnus Jacobsson nor the names of other
 * contributors to the d3-graphviz project are used to endorse or promote
 * products derived from this software as per the 3rd clause of the
 * 3-clause BSD license.
 *
 * See LICENSES/BSD-3-Clause.txt for more details.
 */

export interface BBox {
  x: number;
  y: number;
  width: number;
  height: number;
}

function parsePoints(points: string[]): BBox {
  const x = points.map((p) => Number(p.split(',')[0] ?? 0));
  const y = points.map((p) => Number(p.split(',')[1] ?? 0));
  const xmin = Math.min.apply(null, x);
  const xmax = Math.max.apply(null, x);
  const ymin = Math.min.apply(null, y);
  const ymax = Math.max.apply(null, y);
  return {
    x: xmin,
    y: ymin,
    width: xmax - xmin,
    height: ymax - ymin,
  };
}

/**
 * Compute the bounding box of a polygon without adding it to the DOM.
 *
 * Copyed from
 * https://github.com/magjac/d3-graphviz/blob/81ab523fe5189a90da2d9d9cc9015c7079eea780/src/element.js#L36-L53
 *
 * @param path The polygon to compute the bounding box of.
 * @returns The computed bounding box.
 */
export function parsePolygonBBox(polygon: SVGPolygonElement): BBox {
  const points = (polygon.getAttribute('points') ?? '').split(' ');
  return parsePoints(points);
}

/**
 * Compute the bounding box of a path without adding it to the DOM.
 *
 * Copyed from
 * https://github.com/magjac/d3-graphviz/blob/81ab523fe5189a90da2d9d9cc9015c7079eea780/src/element.js#L56-L75
 *
 * @param path The path to compute the bounding box of.
 * @returns The computed bounding box.
 */
export function parsePathBBox(path: SVGPathElement): BBox {
  const d = path.getAttribute('d') ?? '';
  const points = d.split(/[A-Z ]/);
  points.shift();
  return parsePoints(points);
}
