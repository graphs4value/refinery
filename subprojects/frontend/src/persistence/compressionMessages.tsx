/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { Visibility } from '@tools.refinery/client';
import { z } from 'zod/v4';

/* eslint-disable @typescript-eslint/no-redeclare -- Declare types with their companion objects */

export const CompressorVersion = z.union([z.literal(1), z.literal(2)]);

export type CompressorVersion = z.infer<typeof CompressorVersion>;

export const CompressRequest = z.object({
  request: z.literal('compress'),
  text: z.string(),
  version: CompressorVersion,
});

export type CompressRequest = z.infer<typeof CompressRequest>;

export const DecompressRequest = z.object({
  request: z.literal('decompress'),
  compressedText: z.string(),
  version: CompressorVersion,
});

export type DecompressRequest = z.infer<typeof DecompressRequest>;

export const CompressorRequest = z.discriminatedUnion('request', [
  CompressRequest,
  DecompressRequest,
]);

export type CompressorRequest = z.infer<typeof CompressorRequest>;

export const CompressResponse = z.object({
  response: z.literal('compressed'),
  compressedText: z.string(),
  version: CompressorVersion,
});

export type CompressResponse = z.infer<typeof CompressResponse>;

export const DecompressResponse = z.object({
  response: z.literal('decompressed'),
  text: z.string(),
  version: CompressorVersion,
});

export type DecompressResponse = z.infer<typeof DecompressResponse>;

export const ErrorResponse = z.object({
  response: z.literal('error'),
  message: z.string(),
});

export type ErrorResponse = z.infer<typeof ErrorResponse>;

export const CompressorResponse = z.discriminatedUnion('response', [
  CompressResponse,
  DecompressResponse,
  ErrorResponse,
]);

export type CompressorResponse = z.infer<typeof CompressorResponse>;

export const V2Payload = z.object({
  t: z.string(),
  v: z.record(z.string(), Visibility),
});

export type V2Payload = z.infer<typeof V2Payload>;
