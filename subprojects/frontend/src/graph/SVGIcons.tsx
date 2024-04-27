/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { styled } from '@mui/material/styles';
import { useCallback } from 'react';

import icons from './icons';

export const SVG_NS = 'http://www.w3.org/2000/svg';

const SVGIconsHolder = styled('div', {
  name: 'SVGIcons-Holder',
})({
  position: 'absolute',
  top: 0,
  left: 0,
  width: 0,
  height: 0,
  visibility: 'hidden',
});

export default function SVGIcons(): JSX.Element {
  const addNodes = useCallback((element: HTMLDivElement | null) => {
    if (element === null) {
      return;
    }
    const svgElement = document.createElementNS(SVG_NS, 'svg');
    const defs = document.createElementNS(SVG_NS, 'defs');
    svgElement.appendChild(defs);
    icons.forEach((value) => {
      const importedValue = document.importNode(value, true);
      importedValue.id = `refinery-${importedValue.id}`;
      defs.appendChild(importedValue);
    });
    element.replaceChildren(svgElement);
  }, []);
  return <SVGIconsHolder ref={addNodes} />;
}
