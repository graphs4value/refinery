/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { z } from 'zod/v4';

const SVGExportRequest = z.object({
  outputFormat: z.literal('svg'),
  transparent: z.boolean(),
  embedFonts: z.boolean(),
  theme: z.enum(['light', 'dark', 'auto']),
});

const PDFExportRequest = z.object({
  outputFormat: z.literal('pdf'),
  transparent: z.boolean(),
  embedFonts: z.boolean(),
  theme: z.enum(['light', 'dark']),
});

const PNGExportRequest = z.object({
  outputFormat: z.literal('png'),
  transparent: z.boolean(),
  theme: z.enum(['light', 'dark']),
  scale: z.number(),
});

export const ExportRequest = z.discriminatedUnion('outputFormat', [
  SVGExportRequest,
  PDFExportRequest,
  PNGExportRequest,
]);

export type ExportRequest = z.infer<typeof ExportRequest>;
