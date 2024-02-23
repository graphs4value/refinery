/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { jsPDF } from 'jspdf';
import { svg2pdf } from 'svg2pdf.js';

import boldFontURL from './open-sans-latin-bold.ttf?url';
import italicFontURL from './open-sans-latin-italic.ttf?url';
import normalFontURL from './open-sans-latin-regular.ttf?url';

export default async function serializePDF(
  svg: SVGSVGElement,
  embedFonts: boolean,
): Promise<Blob> {
  const width = svg.width.baseVal.value;
  const height = svg.height.baseVal.value;
  // eslint-disable-next-line new-cap -- jsPDF uses a lowercase constructor.
  const document = new jsPDF({
    orientation: width > height ? 'l' : 'p',
    unit: 'px',
    format: [width, height],
    compress: true,
  });
  if (embedFonts) {
    document.addFont(normalFontURL, 'Open Sans', 'normal', 400);
    document.addFont(italicFontURL, 'Open Sans', 'italic', 400);
    document.addFont(boldFontURL, 'Open Sans', 'normal', 700);
  }
  const result = await svg2pdf(svg, document, {
    width,
    height,
  });
  return result.output('blob');
}
