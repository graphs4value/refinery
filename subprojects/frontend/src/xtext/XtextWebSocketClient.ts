import { nanoid } from 'nanoid';

import PendingTask from '../utils/PendingTask';
import Timer from '../utils/Timer';
import getLogger from '../utils/getLogger';

import {
  XtextWebErrorResponse,
  XtextWebRequest,
  XtextWebOkResponse,
  XtextWebPushMessage,
  XtextWebPushService,
} from './xtextMessages';
import { PongResult } from './xtextServiceResults';

const XTEXT_SUBPROTOCOL_V1 = 'tools.refinery.language.web.xtext.v1';

const WEBSOCKET_CLOSE_OK = 1000;

const WEBSOCKET_CLOSE_GOING_AWAY = 1001;

const RECONNECT_DELAY_MS = [200, 1000, 5000, 30_000];

const MAX_RECONNECT_DELAY_MS =
  RECONNECT_DELAY_MS[RECONNECT_DELAY_MS.length - 1];

const BACKGROUND_IDLE_TIMEOUT_MS = 5 * 60 * 1000;

const PING_TIMEOUT_MS = 10 * 1000;

const REQUEST_TIMEOUT_MS = 1000;

const log = getLogger('xtext.XtextWebSocketClient');

export type ReconnectHandler = () => void;

export type PushHandler = (
  resourceId: string,
  stateId: string,
  service: XtextWebPushService,
  data: unknown,
) => void;

enum State {
  Initial,
  Opening,
  TabVisible,
  TabHiddenIdle,
  TabHiddenWaitingToClose,
  Error,
  ClosedDueToInactivity,
}

export default class XtextWebSocketClient {
  private nextMessageId = 0;

  private connection!: WebSocket;

  private readonly pendingRequests = new Map<string, PendingTask<unknown>>();

  private readonly onReconnect: ReconnectHandler;

  private readonly onPush: PushHandler;

  private state = State.Initial;

  private reconnectTryCount = 0;

  private readonly idleTimer = new Timer(() => {
    this.handleIdleTimeout();
  }, BACKGROUND_IDLE_TIMEOUT_MS);

  private readonly pingTimer = new Timer(() => {
    this.sendPing();
  }, PING_TIMEOUT_MS);

  private readonly reconnectTimer = new Timer(() => {
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
    return (
      this.state === State.Error || this.state === State.ClosedDueToInactivity
    );
  }

  get isOpen(): boolean {
    return (
      this.state === State.TabVisible ||
      this.state === State.TabHiddenIdle ||
      this.state === State.TabHiddenWaitingToClose
    );
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
        log.error(
          'Unknown subprotocol',
          this.connection.protocol,
          'selected by server',
        );
        this.forceReconnectOnError();
      }
      if (document.visibilityState === 'hidden') {
        this.handleTabHidden();
      } else {
        this.handleTabVisibleConnected();
      }
      log.info('Connected to websocket');
      this.nextMessageId = 0;
      this.reconnectTryCount = 0;
      this.pingTimer.schedule();
      this.onReconnect();
    });
    this.connection.addEventListener('error', (event) => {
      log.error('Unexpected websocket error', event);
      this.forceReconnectOnError();
    });
    this.connection.addEventListener('message', (event) => {
      this.handleMessage(event.data);
    });
    this.connection.addEventListener('close', (event) => {
      const closedOnRequest =
        this.isLogicallyClosed &&
        event.code === WEBSOCKET_CLOSE_OK &&
        this.pendingRequests.size === 0;
      const closedOnNavigation = event.code === WEBSOCKET_CLOSE_GOING_AWAY;
      if (closedOnNavigation) {
        this.state = State.ClosedDueToInactivity;
      }
      if (closedOnRequest || closedOnNavigation) {
        log.info('Websocket closed');
        return;
      }
      log.error('Websocket closed unexpectedly', event.code, event.reason);
      this.forceReconnectOnError();
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
    if (
      this.state === State.TabHiddenIdle ||
      this.state === State.TabHiddenWaitingToClose
    ) {
      this.handleTabVisibleConnected();
      return;
    }
    if (this.state === State.ClosedDueToInactivity) {
      this.reconnect();
    }
  }

  private handleTabHidden() {
    log.debug('Tab hidden while websocket is connected');
    this.state = State.TabHiddenIdle;
    this.idleTimer.schedule();
  }

  private handleTabVisibleConnected() {
    log.debug('Tab visible while websocket is connected');
    this.state = State.TabVisible;
  }

  private handleIdleTimeout() {
    log.trace('Waiting for pending tasks before disconnect');
    if (this.state === State.TabHiddenIdle) {
      this.state = State.TabHiddenWaitingToClose;
      this.handleWaitingForDisconnect();
    }
  }

  private handleWaitingForDisconnect() {
    if (this.state !== State.TabHiddenWaitingToClose) {
      return;
    }
    const pending = this.pendingRequests.size;
    if (pending === 0) {
      log.info('Closing idle websocket');
      this.state = State.ClosedDueToInactivity;
      this.closeConnection(1000, 'idle timeout');
      return;
    }
    log.info(
      'Waiting for',
      pending,
      'pending requests before closing websocket',
    );
  }

  private sendPing() {
    if (!this.isOpen) {
      return;
    }
    const ping = nanoid();
    log.trace('Ping', ping);
    this.send({ ping })
      .then((result) => {
        const parsedPongResult = PongResult.safeParse(result);
        if (parsedPongResult.success && parsedPongResult.data.pong === ping) {
          log.trace('Pong', ping);
          this.pingTimer.schedule();
        } else {
          log.error('Invalid pong:', parsedPongResult, 'expected:', ping);
          this.forceReconnectOnError();
        }
      })
      .catch((error) => {
        log.error('Error while waiting for ping', error);
        this.forceReconnectOnError();
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
    } as XtextWebRequest);
    log.trace('Sending message', message);
    return new Promise((resolve, reject) => {
      const task = new PendingTask(resolve, reject, REQUEST_TIMEOUT_MS, () => {
        this.removePendingRequest(messageId);
      });
      this.pendingRequests.set(messageId, task);
      this.connection.send(message);
    });
  }

  private handleMessage(messageStr: unknown) {
    if (typeof messageStr !== 'string') {
      log.error('Unexpected binary message', messageStr);
      this.forceReconnectOnError();
      return;
    }
    log.trace('Incoming websocket message', messageStr);
    let message: unknown;
    try {
      message = JSON.parse(messageStr);
    } catch (error) {
      log.error('Json parse error', error);
      this.forceReconnectOnError();
      return;
    }
    const okResponse = XtextWebOkResponse.safeParse(message);
    if (okResponse.success) {
      const { id, response } = okResponse.data;
      this.resolveRequest(id, response);
      return;
    }
    const errorResponse = XtextWebErrorResponse.safeParse(message);
    if (errorResponse.success) {
      const { id, error, message: errorMessage } = errorResponse.data;
      this.rejectRequest(id, new Error(`${error} error: ${errorMessage}`));
      if (error === 'server') {
        log.error('Reconnecting due to server error: ', errorMessage);
        this.forceReconnectOnError();
      }
      return;
    }
    const pushMessage = XtextWebPushMessage.safeParse(message);
    if (pushMessage.success) {
      const { resource, stateId, service, push } = pushMessage.data;
      this.onPush(resource, stateId, service, push);
    } else {
      log.error(
        'Unexpected websocket message:',
        message,
        'not ok response because:',
        okResponse.error,
        'not error response because:',
        errorResponse.error,
        'not push message because:',
        pushMessage.error,
      );
      this.forceReconnectOnError();
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

  forceReconnectOnError(): void {
    if (this.isLogicallyClosed) {
      return;
    }
    this.pendingRequests.forEach((request) => {
      request.reject(new Error('Websocket disconnect'));
    });
    this.pendingRequests.clear();
    this.closeConnection(1000, 'reconnecting due to error');
    if (this.state === State.Error) {
      // We are already handling this error condition.
      return;
    }
    if (
      this.state === State.TabHiddenIdle ||
      this.state === State.TabHiddenWaitingToClose
    ) {
      log.error('Will reconned due to error once the tab becomes visible');
      this.idleTimer.cancel();
      this.state = State.ClosedDueToInactivity;
      return;
    }
    log.error('Reconnecting after delay due to error');
    this.state = State.Error;
    this.reconnectTryCount += 1;
    const delay =
      RECONNECT_DELAY_MS[this.reconnectTryCount - 1] ?? MAX_RECONNECT_DELAY_MS;
    log.info('Reconnecting in', delay, 'ms');
    this.reconnectTimer.schedule(delay);
  }

  private closeConnection(code: number, reason: string) {
    this.pingTimer.cancel();
    const { readyState } = this.connection;
    if (readyState !== WebSocket.CLOSING && readyState !== WebSocket.CLOSED) {
      this.connection.close(code, reason);
    }
  }

  private handleReconnect() {
    if (this.state !== State.Error) {
      log.error('Unexpected reconnect in', this.state);
      return;
    }
    if (document.visibilityState === 'hidden') {
      this.state = State.ClosedDueToInactivity;
    } else {
      this.reconnect();
    }
  }
}
