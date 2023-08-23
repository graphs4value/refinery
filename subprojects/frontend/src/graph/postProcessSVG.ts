/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { type BBox, parsePolygonBBox, parsePathBBox } from './parseBBox';

const SVG_NS = 'http://www.w3.org/2000/svg';

function clipCompartmentBackground(node: SVGGElement) {
  // Background rectangle of the node created by the `<table bgcolor="green">`
  // HTML element in dot. It was transformed into a rounded rect by `fixNodeBackground`.
  const container = node.querySelector<SVGRectElement>('rect[fill="green"]');
  // Background rectangle of the lower compartment created by the `<td bgcolor="white">`
  // HTML element in dot. It was transformed into a rounded rect by `fixNodeBackground`.
  // Since dot doesn't round the coners of `<td>` background,
  // we have to clip it ourselves.
  const compartment = node.querySelector<SVGPolygonElement>(
    'polygon[fill="white"]',
  );
  if (container === null || compartment === null) {
    return;
  }
  const copyOfContainer = container.cloneNode() as SVGRectElement;
  const clipPath = document.createElementNS(SVG_NS, 'clipPath');
  const clipId = `${node.id},,clip`;
  clipPath.setAttribute('id', clipId);
  clipPath.appendChild(copyOfContainer);
  node.appendChild(clipPath);
  compartment.setAttribute('clip-path', `url(#${clipId})`);
}

function createRect(
  { x, y, width, height }: BBox,
  original: SVGElement,
): SVGRectElement {
  const rect = document.createElementNS(SVG_NS, 'rect');
  rect.setAttribute('fill', original.getAttribute('fill') ?? '');
  rect.setAttribute('stroke', original.getAttribute('stroke') ?? '');
  rect.setAttribute('x', String(x));
  rect.setAttribute('y', String(y));
  rect.setAttribute('width', String(width));
  rect.setAttribute('height', String(height));
  return rect;
}

function optimizeNodeShapes(node: SVGGElement) {
  node.querySelectorAll('path').forEach((path) => {
    const bbox = parsePathBBox(path);
    const rect = createRect(bbox, path);
    rect.setAttribute('rx', '12');
    rect.setAttribute('ry', '12');
    node.replaceChild(rect, path);
  });
  node.querySelectorAll('polygon').forEach((polygon) => {
    const bbox = parsePolygonBBox(polygon);
    if (bbox.height === 0) {
      const polyline = document.createElementNS(SVG_NS, 'polyline');
      polyline.setAttribute('stroke', polygon.getAttribute('stroke') ?? '');
      polyline.setAttribute(
        'points',
        `${bbox.x},${bbox.y} ${bbox.x + bbox.width},${bbox.y}`,
      );
      node.replaceChild(polyline, polygon);
    } else {
      const rect = createRect(bbox, polygon);
      node.replaceChild(rect, polygon);
    }
  });
  clipCompartmentBackground(node);
}

export default function postProcessSvg(svg: SVGSVGElement) {
  svg
    .querySelectorAll<SVGTitleElement>('title')
    .forEach((title) => title.parentNode?.removeChild(title));
  svg.querySelectorAll<SVGGElement>('g.node').forEach(optimizeNodeShapes);
}
