import { nanoid } from 'nanoid';

import { getLogger } from '../logging';
import { PendingRequest } from './PendingRequest';
import {
  isErrorResponse,
  isOkResponse,
  isPushMessage,
  IXtextWebRequest,
} from './xtextMessages';
import { isPongResult } from './xtextServiceResults';

const XTEXT_SUBPROTOCOL_V1 = 'tools.refinery.language.web.xtext.v1';

const WEBSOCKET_CLOSE_OK = 1000;

const RECONNECT_DELAY_MS = 1000;

const IDLE_TIMEOUT_MS = 10 * 60 * 1000;

const PING_TIMEOUT_MS = 10 * 1000;

const log = getLogger('XtextWebSocketClient');

type ReconnectHandler = () => void;

type PushHandler = (resourceId: string, stateId: string, service: string, data: unknown) => void;

export class XtextWebSocketClient {
  nextMessageId = 0;

  closing = false;

  connection!: WebSocket;

  pendingRequests = new Map<string, PendingRequest>();

  onReconnect: ReconnectHandler;

  onPush: PushHandler;

  reconnectTimeout: NodeJS.Timeout | null = null;

  idleTimeout: NodeJS.Timeout | null = null;

  pingTimeout: NodeJS.Timeout | null = null;

  constructor(onReconnect: ReconnectHandler, onPush: PushHandler) {
    this.onReconnect = onReconnect;
    this.onPush = onPush;
    this.reconnect();
  }

  get isOpen(): boolean {
    return this.connection.readyState === WebSocket.OPEN;
  }

  get isClosed(): boolean {
    return this.connection.readyState === WebSocket.CLOSING
      || this.connection.readyState === WebSocket.CLOSED;
  }

  ensureOpen(): void {
    if (this.isClosed) {
      this.closing = false;
      this.reconnect();
    }
  }

  private reconnect() {
    this.reconnectTimeout = null;
    const webSocketServer = window.origin.replace(/^http/, 'ws');
    const webSocketUrl = `${webSocketServer}/xtext-service`;
    this.connection = new WebSocket(webSocketUrl, XTEXT_SUBPROTOCOL_V1);
    this.connection.addEventListener('open', () => {
      if (this.connection.protocol !== XTEXT_SUBPROTOCOL_V1) {
        log.error('Unknown subprotocol', this.connection.protocol, 'selected by server');
        this.forceReconnectDueToError();
        return;
      }
      log.info('Connected to xtext web services');
      this.onReconnect();
    });
    this.connection.addEventListener('error', (event) => {
      log.error('Unexpected websocket error', event);
      this.forceReconnectDueToError();
    });
    this.connection.addEventListener('message', (event) => {
      this.handleMessage(event.data);
    });
    this.connection.addEventListener('close', (event) => {
      if (!this.closing || event.code !== WEBSOCKET_CLOSE_OK) {
        log.error('Websocket closed undexpectedly', event.code, event.reason);
      }
      this.cleanupAndMaybeReconnect();
    });
    this.scheduleIdleTimeout();
    this.schedulePingTimeout();
  }

  private scheduleIdleTimeout() {
    if (this.idleTimeout !== null) {
      clearTimeout(this.idleTimeout);
    }
    this.idleTimeout = setTimeout(() => {
      log.info('Closing websocket connection due to inactivity');
      this.close();
    }, IDLE_TIMEOUT_MS);
  }

  private schedulePingTimeout() {
    if (this.pingTimeout !== null) {
      return;
    }
    this.pingTimeout = setTimeout(() => {
      if (this.isClosed) {
        return;
      }
      if (this.isOpen) {
        const ping = nanoid();
        log.trace('ping:', ping);
        this.pingTimeout = null;
        this.internalSend({
          ping,
        }).catch((error) => {
          log.error('ping error', error);
          this.forceReconnectDueToError();
        }).then((result) => {
          if (!isPongResult(result) || result.pong !== ping) {
            log.error('invalid pong');
            this.forceReconnectDueToError();
          }
          log.trace('pong:', ping);
        });
      }
      this.schedulePingTimeout();
    }, PING_TIMEOUT_MS);
  }

  private cleanupAndMaybeReconnect() {
    this.cleanup();
    if (!this.closing) {
      this.delayedReconnect();
    }
  }

  private cleanup() {
    this.pendingRequests.forEach((pendingRequest) => {
      pendingRequest.reject(new Error('Websocket closed'));
    });
    this.pendingRequests.clear();
    if (this.idleTimeout !== null) {
      clearTimeout(this.idleTimeout);
      this.idleTimeout = null;
    }
    if (this.pingTimeout !== null) {
      clearTimeout(this.pingTimeout);
      this.pingTimeout = null;
    }
  }

  private delayedReconnect() {
    if (this.reconnectTimeout !== null) {
      clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = null;
    }
    this.reconnectTimeout = setTimeout(() => {
      log.info('Attempting to reconnect websocket');
      this.reconnect();
    }, RECONNECT_DELAY_MS);
  }

  public forceReconnectDueToError(): void {
    this.closeConnection();
    this.cleanupAndMaybeReconnect();
  }

  send(request: unknown): Promise<unknown> {
    if (!this.isOpen) {
      throw new Error('Connection is not open');
    }
    this.scheduleIdleTimeout();
    return this.internalSend(request);
  }

  private internalSend(request: unknown): Promise<unknown> {
    const messageId = this.nextMessageId.toString(16);
    if (messageId in this.pendingRequests) {
      log.error('Message id wraparound still pending', messageId);
      this.rejectRequest(messageId, new Error('Message id wraparound'));
    }
    if (this.nextMessageId >= Number.MAX_SAFE_INTEGER) {
      this.nextMessageId = 0;
    } else {
      this.nextMessageId += 1;
    }
    const message = JSON.stringify({
      id: messageId,
      request,
    } as IXtextWebRequest);
    return new Promise((resolve, reject) => {
      this.connection.send(message);
      this.pendingRequests.set(messageId, new PendingRequest(resolve, reject));
    });
  }

  private handleMessage(messageStr: unknown) {
    if (typeof messageStr !== 'string') {
      log.error('Unexpected binary message', messageStr);
      this.forceReconnectDueToError();
      return;
    }
    log.trace('Incoming websocket message', messageStr);
    let message: unknown;
    try {
      message = JSON.parse(messageStr);
    } catch (error) {
      log.error('Json parse error', error);
      this.forceReconnectDueToError();
      return;
    }
    if (isOkResponse(message)) {
      this.resolveRequest(message.id, message.response);
    } else if (isErrorResponse(message)) {
      this.rejectRequest(message.id, new Error(`${message.error} error: ${message.message}`));
      if (message.error === 'server') {
        log.error('Reconnecting due to server error: ', message.message);
        this.forceReconnectDueToError();
      }
    } else if (isPushMessage(message)) {
      this.onPush(message.resource, message.stateId, message.service, message.push);
    } else {
      log.error('Unexpected websocket message', message);
      this.forceReconnectDueToError();
    }
  }

  private resolveRequest(messageId: string, value: unknown) {
    const pendingRequest = this.pendingRequests.get(messageId);
    this.pendingRequests.delete(messageId);
    if (pendingRequest) {
      pendingRequest.resolve(value);
      return;
    }
    log.error('Trying to resolve unknown request', messageId, 'with', value);
  }

  private rejectRequest(messageId: string, reason?: unknown) {
    const pendingRequest = this.pendingRequests.get(messageId);
    this.pendingRequests.delete(messageId);
    if (pendingRequest) {
      pendingRequest.reject(reason);
      return;
    }
    log.error('Trying to reject unknown request', messageId, 'with', reason);
  }

  private closeConnection() {
    if (!this.isClosed) {
      log.info('Closing websocket connection');
      this.connection.close(1000, 'end session');
    }
  }

  close(): void {
    this.closing = true;
    this.closeConnection();
    this.cleanup();
  }
}
