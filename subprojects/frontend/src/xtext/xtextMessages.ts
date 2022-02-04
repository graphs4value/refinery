import { z } from 'zod';

export const xtextWebRequest = z.object({
  id: z.string().nonempty(),
  request: z.unknown(),
});

export type XtextWebRequest = z.infer<typeof xtextWebRequest>;

export const xtextWebOkResponse = z.object({
  id: z.string().nonempty(),
  response: z.unknown(),
});

export type XtextWebOkResponse = z.infer<typeof xtextWebOkResponse>;

export const xtextWebErrorKind = z.enum(['request', 'server']);

export type XtextWebErrorKind = z.infer<typeof xtextWebErrorKind>;

export const xtextWebErrorResponse = z.object({
  id: z.string().nonempty(),
  error: xtextWebErrorKind,
  message: z.string(),
});

export type XtextWebErrorResponse = z.infer<typeof xtextWebErrorResponse>;

export const xtextWebPushService = z.enum(['highlight', 'validate']);

export type XtextWebPushService = z.infer<typeof xtextWebPushService>;

export const xtextWebPushMessage = z.object({
  resource: z.string().nonempty(),
  stateId: z.string().nonempty(),
  service: xtextWebPushService,
  push: z.unknown(),
});

export type XtextWebPushMessage = z.infer<typeof xtextWebPushMessage>;
