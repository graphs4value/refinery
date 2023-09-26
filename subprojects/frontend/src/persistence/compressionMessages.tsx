/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { z } from 'zod';

/* eslint-disable @typescript-eslint/no-redeclare -- Declare types with their companion objects */

export const CompressRequest = z.object({
  request: z.literal('compress'),
  text: z.string(),
});

export type CompressRequest = z.infer<typeof CompressRequest>;

export const DecompressRequest = z.object({
  request: z.literal('decompress'),
  compressedText: z.string(),
});

export type DecompressRequest = z.infer<typeof DecompressRequest>;

export const CompressorRequest = z.union([CompressRequest, DecompressRequest]);

export type CompressorRequest = z.infer<typeof CompressorRequest>;

export const CompressResponse = z.object({
  response: z.literal('compressed'),
  compressedText: z.string(),
});

export type CompressResponse = z.infer<typeof CompressResponse>;

export const DecompressResponse = z.object({
  response: z.literal('decompressed'),
  text: z.string(),
});

export type DecompressResponse = z.infer<typeof DecompressResponse>;

export const ErrorResponse = z.object({
  response: z.literal('error'),
  message: z.string(),
});

export type ErrorResponse = z.infer<typeof ErrorResponse>;

export const CompressorResponse = z.union([
  CompressResponse,
  DecompressResponse,
  ErrorResponse,
]);

export type CompressorResponse = z.infer<typeof CompressorResponse>;
