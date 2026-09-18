/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import type {
  PDFDocumentProxy,
  PDFPageProxy,
} from 'pdfjs-dist/legacy/build/pdf.mjs';

import loadPDF from './loadPDF';

function isEmbeddedFont(data: unknown): boolean {
  return (
    data !== null &&
    typeof data === 'object' &&
    'missingFile' in data &&
    data.missingFile === false
  );
}

async function pdfPageHasEmbeddedFont(page: PDFPageProxy): Promise<boolean> {
  // Fonts are only loaded into `commonObjs` once the page's content stream
  // has been evaluated -- a no-op if `page.render()` already did this.
  await page.getOperatorList();
  let hasEmbeddedFont = false;
  for (const [, data] of page.commonObjs) {
    if (isEmbeddedFont(data)) {
      hasEmbeddedFont = true;
    }
  }
  return hasEmbeddedFont;
}
export async function pdfDocumentHasEmbeddedFont(
  document: PDFDocumentProxy,
): Promise<boolean> {
  for (let i = 1; i <= document.numPages; i++) {
    const page = await document.getPage(i);
    if (await pdfPageHasEmbeddedFont(page)) {
      return true;
    }
  }
  return false;
}

export default async function pdfHasEmbeddedFont(
  pdf: Buffer,
): Promise<boolean> {
  const document = await loadPDF(pdf);
  try {
    return await pdfDocumentHasEmbeddedFont(document);
  } finally {
    await document.destroy();
  }
}
