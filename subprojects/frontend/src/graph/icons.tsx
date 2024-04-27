/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import cancelSVG from '@material-icons/svg/svg/cancel/baseline.svg?raw';
import labelSVG from '@material-icons/svg/svg/label/baseline.svg?raw';
import labelOutlinedSVG from '@material-icons/svg/svg/label/outline.svg?raw';

const icons: Map<string, Element> = new Map();

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

importSVG(labelSVG, 'icon-TRUE');
importSVG(labelOutlinedSVG, 'icon-UNKNOWN');
importSVG(cancelSVG, 'icon-ERROR');
