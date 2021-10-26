import { nanoid } from 'nanoid';

import { getLogger } from '../logging';
import { PendingRequest } from './PendingRequest';
import { Timer } from '../utils/Timer';
import {
  isErrorResponse,
  isOkResponse,
  isPushMessage,
  IXtextWebRequest,
} from './xtextMessages';
import { isPongResult } from './xtextServiceResults';

const XTEXT_SUBPROTOCOL_V1 = 'tools.refinery.language.web.xtext.v1';

const WEBSOCKET_CLOSE_OK = 1000;

const RECONNECT_DELAY_MS = [1000, 5000, 30_000];

const MAX_RECONNECT_DELAY_MS = RECONNECT_DELAY_MS[RECONNECT_DELAY_MS.length - 1];

const MAX_APP_ERROR_COUNT = 1;

const BACKGROUND_IDLE_TIMEOUT_MS = 5 * 60 * 1000;

const PING_TIMEOUT_MS = 10 * 1000;

const log = getLogger('XtextWebSocketClient');

type ReconnectHandler = () => Promise<void>;

type PushHandler = (
  resourceId: string,
  stateId: string,
  service: string,
  data: unknown
) => Promise<void>;

enum State {
  Initial,
  Opening,
  TabVisible,
  TabHiddenIdle,
  TabHiddenWaiting,
  Error,
  TimedOut,
}

export class XtextWebSocketClient {
  nextMessageId = 0;

  connection!: WebSocket;

  pendingRequests = new Map<string, PendingRequest>();

  onReconnect: ReconnectHandler;

  onPush: PushHandler;

  state = State.Initial;

  appErrorCount = 0;

  reconnectTryCount = 0;

  idleTimer = new Timer(() => {
    this.handleIdleTimeout();
  }, BACKGROUND_IDLE_TIMEOUT_MS);

  pingTimer = new Timer(() => {
    this.sendPing();
  }, PING_TIMEOUT_MS);

  reconnectTimer = new Timer(() => {
    this.handleReconnect();
  });

  constructor(onReconnect: ReconnectHandler, onPush: PushHandler) {
    this.onReconnect = onReconnect;
    this.onPush = onPush;
    document.addEventListener('visibilitychange', () => {
      this.handleVisibilityChange();
    });
    this.reconnect();
  }

  private get isLogicallyClosed(): boolean {
    return this.state === State.Error || this.state === State.TimedOut;
  }

  get isOpen(): boolean {
    return this.state === State.TabVisible
      || this.state === State.TabHiddenIdle
      || this.state === State.TabHiddenWaiting;
  }

  private reconnect() {
    if (this.isOpen || this.state === State.Opening) {
      log.error('Trying to reconnect from', this.state);
      return;
    }
    this.state = State.Opening;
    const webSocketServer = window.origin.replace(/^http/, 'ws');
    const webSocketUrl = `${webSocketServer}/xtext-service`;
    this.connection = new WebSocket(webSocketUrl, XTEXT_SUBPROTOCOL_V1);
    this.connection.addEventListener('open', () => {
      if (this.connection.protocol !== XTEXT_SUBPROTOCOL_V1) {
        log.error('Unknown subprotocol', this.connection.protocol, 'selected by server');
        this.handleProtocolError();
      }
      if (document.visibilityState === 'hidden') {
        this.handleTabHidden();
      } else {
        this.handleTabVisibleConnected();
      }
      log.info('Connected to websocket');
      this.nextMessageId = 0;
      this.appErrorCount = 0;
      this.reconnectTryCount = 0;
      this.pingTimer.schedule();
      this.onReconnect().catch((error) => {
        log.error('Unexpected error in onReconnect handler', error);
      });
    });
    this.connection.addEventListener('error', (event) => {
      log.error('Unexpected websocket error', event);
      this.handleProtocolError();
    });
    this.connection.addEventListener('message', (event) => {
      this.handleMessage(event.data);
    });
    this.connection.addEventListener('close', (event) => {
      if (this.isLogicallyClosed && event.code === WEBSOCKET_CLOSE_OK
        && this.pendingRequests.size === 0) {
        log.info('Websocket closed');
        return;
      }
      log.error('Websocket closed unexpectedly', event.code, event.reason);
      this.handleProtocolError();
    });
  }

  private handleVisibilityChange() {
    if (document.visibilityState === 'hidden') {
      if (this.state === State.TabVisible) {
        this.handleTabHidden();
      }
      return;
    }
    this.idleTimer.cancel();
    if (this.state === State.TabHiddenIdle || this.state === State.TabHiddenWaiting) {
      this.handleTabVisibleConnected();
      return;
    }
    if (this.state === State.TimedOut) {
      this.reconnect();
    }
  }

  private handleTabHidden() {
    log.trace('Tab became hidden while websocket is connected');
    this.state = State.TabHiddenIdle;
    this.idleTimer.schedule();
  }

  private handleTabVisibleConnected() {
    log.trace('Tab became visible while websocket is connected');
    this.state = State.TabVisible;
  }

  private handleIdleTimeout() {
    log.trace('Waiting for pending tasks before disconnect');
    if (this.state === State.TabHiddenIdle) {
      this.state = State.TabHiddenWaiting;
      this.handleWaitingForDisconnect();
    }
  }

  private handleWaitingForDisconnect() {
    if (this.state !== State.TabHiddenWaiting) {
      return;
    }
    const pending = this.pendingRequests.size;
    if (pending === 0) {
      log.info('Closing idle websocket');
      this.state = State.TimedOut;
      this.closeConnection(1000, 'idle timeout');
      return;
    }
    log.info('Waiting for', pending, 'pending requests before closing websocket');
  }

  private sendPing() {
    if (!this.isOpen) {
      return;
    }
    const ping = nanoid();
    log.trace('Ping', ping);
    this.send({ ping }).then((result) => {
      if (isPongResult(result) && result.pong === ping) {
        log.trace('Pong', ping);
        this.pingTimer.schedule();
      } else {
        log.error('Invalid pong');
        this.handleProtocolError();
      }
    }).catch((error) => {
      log.error('Error while waiting for ping', error);
      this.handleProtocolError();
    });
  }

  send(request: unknown): Promise<unknown> {
    if (!this.isOpen) {
      throw new Error('Not open');
    }
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
    const promise = new Promise((resolve, reject) => {
      this.pendingRequests.set(messageId, new PendingRequest(resolve, reject, () => {
        this.removePendingRequest(messageId);
      }));
    });
    log.trace('Sending message', message);
    this.connection.send(message);
    return promise;
  }

  private handleMessage(messageStr: unknown) {
    if (typeof messageStr !== 'string') {
      log.error('Unexpected binary message', messageStr);
      this.handleProtocolError();
      return;
    }
    log.trace('Incoming websocket message', messageStr);
    let message: unknown;
    try {
      message = JSON.parse(messageStr);
    } catch (error) {
      log.error('Json parse error', error);
      this.handleProtocolError();
      return;
    }
    if (isOkResponse(message)) {
      this.resolveRequest(message.id, message.response);
    } else if (isErrorResponse(message)) {
      this.rejectRequest(message.id, new Error(`${message.error} error: ${message.message}`));
      if (message.error === 'server') {
        log.error('Reconnecting due to server error: ', message.message);
        this.handleApplicationError();
      }
    } else if (isPushMessage(message)) {
      this.onPush(
        message.resource,
        message.stateId,
        message.service,
        message.push,
      ).catch((error) => {
        log.error('Unexpected error in onPush handler', error);
      });
    } else {
      log.error('Unexpected websocket message', message);
      this.handleProtocolError();
    }
  }

  private resolveRequest(messageId: string, value: unknown) {
    const pendingRequest = this.pendingRequests.get(messageId);
    if (pendingRequest) {
      pendingRequest.resolve(value);
      this.removePendingRequest(messageId);
      return;
    }
    log.error('Trying to resolve unknown request', messageId, 'with', value);
  }

  private rejectRequest(messageId: string, reason?: unknown) {
    const pendingRequest = this.pendingRequests.get(messageId);
    if (pendingRequest) {
      pendingRequest.reject(reason);
      this.removePendingRequest(messageId);
      return;
    }
    log.error('Trying to reject unknown request', messageId, 'with', reason);
  }

  private removePendingRequest(messageId: string) {
    this.pendingRequests.delete(messageId);
    this.handleWaitingForDisconnect();
  }

  private handleProtocolError() {
    if (this.isLogicallyClosed) {
      return;
    }
    this.abortPendingRequests();
    this.closeConnection(1000, 'reconnecting due to protocol error');
    log.error('Reconnecting after delay due to protocol error');
    this.handleErrorState();
  }

  handleApplicationError(): void {
    if (this.isLogicallyClosed) {
      return;
    }
    this.abortPendingRequests();
    this.closeConnection(1000, 'reconnecting due to application error');
    this.appErrorCount += 1;
    if (this.appErrorCount <= MAX_APP_ERROR_COUNT) {
      log.error('Immediately reconnecting due to application error');
      this.state = State.Initial;
      this.reconnect();
    } else {
      log.error('Reconnecting after delay due to application error');
      this.handleErrorState();
    }
  }

  private abortPendingRequests() {
    this.pendingRequests.forEach((request) => {
      request.reject(new Error('Websocket disconnect'));
    });
    this.pendingRequests.clear();
  }

  private closeConnection(code: number, reason: string) {
    this.pingTimer.cancel();
    const { readyState } = this.connection;
    if (readyState !== WebSocket.CLOSING && readyState !== WebSocket.CLOSED) {
      this.connection.close(code, reason);
    }
  }

  private handleErrorState() {
    this.state = State.Error;
    this.reconnectTryCount += 1;
    const delay = RECONNECT_DELAY_MS[this.reconnectTryCount - 1] || MAX_RECONNECT_DELAY_MS;
    log.info('Reconnecting in', delay, 'ms');
    this.reconnectTimer.schedule(delay);
  }

  private handleReconnect() {
    if (this.state !== State.Error) {
      log.error('Unexpected reconnect in', this.state);
      return;
    }
    if (document.visibilityState === 'hidden') {
      this.state = State.TimedOut;
    } else {
      this.reconnect();
    }
  }
}
