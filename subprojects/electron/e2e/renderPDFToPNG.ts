/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { createCanvas } from '@napi-rs/canvas';
import type { PDFDocumentProxy } from 'pdfjs-dist/legacy/build/pdf.mjs';

import loadPDF from './loadPDF';

/**
 * Rasterizes `page` to a PNG the same width as `targetWidth`. PDF pages are
 * sized in points (1/72in), while our other outputs treat 1 unit as 1 CSS
 * pixel (1/96in) at scale 1, so we compute the exact scale to match
 * `targetWidth` instead of assuming `scale: 1` lines up with our raster
 * pixel grid.
 */
export async function renderPDFDocumentToPNG(
  document: PDFDocumentProxy,
  targetWidth: number,
): Promise<Buffer> {
  const page = await document.getPage(1);
  const nativeWidth = page.getViewport({ scale: 1 }).width;
  const viewport = page.getViewport({ scale: targetWidth / nativeWidth });
  const canvas = createCanvas(viewport.width, viewport.height);
  const canvasContext = canvas.getContext('2d');
  await page.render({ canvasContext, canvas, viewport }).promise;
  return canvas.toBuffer('image/png');
}

export default async function renderPDFToPNG(
  pdf: Buffer,
  targetWidth: number,
): Promise<Buffer> {
  const document = await loadPDF(pdf);
  try {
    return await renderPDFDocumentToPNG(document, targetWidth);
  } finally {
    await document.destroy();
  }
}
