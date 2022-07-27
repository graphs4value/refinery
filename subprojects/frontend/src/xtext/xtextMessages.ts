import { z } from 'zod';

export const xtextWebRequest = z.object({
  id: z.string().min(1),
  request: z.unknown(),
});

export type XtextWebRequest = z.infer<typeof xtextWebRequest>;

export const xtextWebOkResponse = z.object({
  id: z.string().min(1),
  response: z.unknown(),
});

export type XtextWebOkResponse = z.infer<typeof xtextWebOkResponse>;

export const xtextWebErrorKind = z.enum(['request', 'server']);

export type XtextWebErrorKind = z.infer<typeof xtextWebErrorKind>;

export const xtextWebErrorResponse = z.object({
  id: z.string().min(1),
  error: xtextWebErrorKind,
  message: z.string(),
});

export type XtextWebErrorResponse = z.infer<typeof xtextWebErrorResponse>;

export const xtextWebPushService = z.enum(['highlight', 'validate']);

export type XtextWebPushService = z.infer<typeof xtextWebPushService>;

export const xtextWebPushMessage = z.object({
  resource: z.string().min(1),
  stateId: z.string().min(1),
  service: xtextWebPushService,
  push: z.unknown(),
});

export type XtextWebPushMessage = z.infer<typeof xtextWebPushMessage>;
