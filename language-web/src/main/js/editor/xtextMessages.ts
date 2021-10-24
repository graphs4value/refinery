export interface IXtextWebRequest {
  id: string;

  request: unknown;
}

export interface IXtextWebOkResponse {
  id: string;

  response: unknown;
}

export function isOkResponse(response: unknown): response is IXtextWebOkResponse {
  const okResponse = response as IXtextWebOkResponse;
  return typeof okResponse.id === 'string'
    && typeof okResponse.response !== 'undefined';
}

export const VALID_XTEXT_WEB_ERROR_KINDS = ['request', 'server'] as const;

export type XtextWebErrorKind = typeof VALID_XTEXT_WEB_ERROR_KINDS[number];

export interface IXtextWebErrorResponse {
  id: string;

  error: XtextWebErrorKind;

  message: string;
}

export function isErrorResponse(response: unknown): response is IXtextWebErrorResponse {
  const errorResponse = response as IXtextWebErrorResponse;
  return typeof errorResponse.id === 'string'
    && typeof errorResponse.error === 'string'
    && VALID_XTEXT_WEB_ERROR_KINDS.includes(errorResponse.error)
    && typeof errorResponse.message === 'string';
}

export interface IXtextWebPushMessage {
  resource: string;

  stateId: string;

  service: string;

  push: unknown;
}

export function isPushMessage(response: unknown): response is IXtextWebPushMessage {
  const pushMessage = response as IXtextWebPushMessage;
  return typeof pushMessage.resource === 'string'
    && typeof pushMessage.stateId === 'string'
    && typeof pushMessage.service === 'string'
    && typeof pushMessage.push !== 'undefined';
}
