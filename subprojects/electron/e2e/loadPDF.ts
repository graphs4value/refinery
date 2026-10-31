/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import {
  getDocument,
  type PDFDocumentProxy,
  VerbosityLevel,
} from 'pdfjs-dist/legacy/build/pdf.mjs';

export default function loadPDF(pdf: Buffer): Promise<PDFDocumentProxy> {
  return getDocument({
    data: new Uint8Array(pdf),
    // Without a `standardFontDataUrl`, pdf.js can't substitute a standard
    // font for text that doesn't have its own font embedded -- which is
    // exactly the signal `pdfHasEmbeddedFont` relies on, so we don't want
    // pdf.js to paper over it, and silence the resulting warning here.
    verbosity: VerbosityLevel.ERRORS,
  }).promise;
}
