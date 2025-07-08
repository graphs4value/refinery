/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import cancelSVG from '@material-icons/svg/svg/cancel/baseline.svg?raw';
import labelSVG from '@material-icons/svg/svg/label/baseline.svg?raw';
import labelOutlinedSVG from '@material-icons/svg/svg/label/outline.svg?raw';

import numberSVG from './dot_filled.svg?raw';
import numberOutlinedSVG from './dot_outlined.svg?raw';

const icons = new Map<string, Element>();

export default icons;

function importSVG(svgSource: string, className: string): void {
  const parser = new DOMParser();
  const svgDocument = parser.parseFromString(svgSource, 'image/svg+xml');
  const root = svgDocument.children[0];
  if (root === undefined) {
    return;
  }
  root.id = className;
  root.classList.add(className);
  icons.set(className, root);
}

importSVG(labelSVG, 'icon-true');
importSVG(labelOutlinedSVG, 'icon-unknown');
importSVG(cancelSVG, 'icon-error');
importSVG(numberSVG, 'icon-attribute-true');
importSVG(numberOutlinedSVG, 'icon-attribute-unknown');
