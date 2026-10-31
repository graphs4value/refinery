/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { jsPDF } from 'jspdf';
import { svg2pdf } from 'svg2pdf.js';

import { pxToPT } from '../../utils/ptToPX';

import boldFontURL from './open-sans-latin-bold.ttf?url';
import italicFontURL from './open-sans-latin-italic.ttf?url';
import normalFontURL from './open-sans-latin-regular.ttf?url';

export default async function serializePDF(
  svg: SVGSVGElement,
  embedFonts: boolean,
): Promise<Blob> {
  // jsPDF's `unit: 'px'` doesn't treat 1 page-space unit as 1 CSS pixel --
  // without the (unfortunately non-default) `px_scaling` hotfix, it scales
  // by 96/72 instead of 72/96, inflating the physical page size. Converting
  // to points ourselves avoids depending on that hotfix.
  // See https://github.com/parallax/jsPDF/blob/4562ce8aa35bd5ecd98cd5e262e3da2af96476f6/HOTFIX_README.md#px_scaling
  const width = pxToPT(svg.width.baseVal.value);
  const height = pxToPT(svg.height.baseVal.value);
  // eslint-disable-next-line new-cap -- jsPDF uses a lowercase constructor.
  const document = new jsPDF({
    orientation: width > height ? 'l' : 'p',
    unit: 'pt',
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
