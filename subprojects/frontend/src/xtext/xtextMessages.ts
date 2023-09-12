/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

/* eslint-disable @typescript-eslint/no-redeclare -- Declare types with their companion objects */

import { z } from 'zod';

export const XtextWebRequest = z.object({
  id: z.string().min(1),
  request: z.unknown(),
});

export type XtextWebRequest = z.infer<typeof XtextWebRequest>;

export const XtextWebOkResponse = z.object({
  id: z.string().min(1),
  response: z.unknown(),
});

export type XtextWebOkResponse = z.infer<typeof XtextWebOkResponse>;

export const XtextWebErrorKind = z.enum(['request', 'server']);

export type XtextWebErrorKind = z.infer<typeof XtextWebErrorKind>;

export const XtextWebErrorResponse = z.object({
  id: z.string().min(1),
  error: XtextWebErrorKind,
  message: z.string(),
});

export type XtextWebErrorResponse = z.infer<typeof XtextWebErrorResponse>;

export const XtextWebPushService = z.enum([
  'highlight',
  'validate',
  'semantics',
  'modelGeneration',
]);

export type XtextWebPushService = z.infer<typeof XtextWebPushService>;

export const XtextWebPushMessage = z.object({
  resource: z.string().min(1),
  stateId: z.string().min(1),
  service: XtextWebPushService,
  push: z.unknown(),
});

export type XtextWebPushMessage = z.infer<typeof XtextWebPushMessage>;

export const XtextResponse = z.union([
  XtextWebOkResponse,
  XtextWebErrorResponse,
  XtextWebPushMessage,
]);

export type XtextResponse = z.infer<typeof XtextResponse>;
