import { createAtom, makeAutoObservable, observable } from 'mobx';
import ms from 'ms';
import { nanoid } from 'nanoid';
import { interpret } from 'xstate';

import CancelledError from '../utils/CancelledError';
import PendingTask from '../utils/PendingTask';
import getLogger from '../utils/getLogger';

import webSocketMachine, { isWebSocketURLLocal } from './webSocketMachine';
import {
  type XtextWebPushService,
  XtextResponse,
  type XtextWebRequest,
} from './xtextMessages';
import { PongResult } from './xtextServiceResults';

const XTEXT_SUBPROTOCOL_V1 = 'tools.refinery.language.web.xtext.v1';

// Use a large enough timeout so that a request can complete successfully
// even if the browser has throttled the background tab.
const REQUEST_TIMEOUT = ms('5s');

const log = getLogger('xtext.XtextWebSocketClient');

export type ReconnectHandler = () => void;

export type DisconnectHandler = () => void;

export type PushHandler = (
  resourceId: string,
  stateId: string,
  service: XtextWebPushService,
  data: unknown,
) => void;

export default class XtextWebSocketClient {
  private readonly stateAtom = createAtom('state');

  private webSocket: WebSocket | undefined;

  private readonly pendingRequests = new Map<string, PendingTask<unknown>>();

  private readonly interpreter = interpret(
    webSocketMachine
      .withContext({
        ...webSocketMachine.context,
        webSocketURL: `${window.location.origin.replace(
          /^http/,
          'ws',
        )}/xtext-service`,
      })
      .withConfig({
        actions: {
          openWebSocket: ({ webSocketURL }) => this.openWebSocket(webSocketURL),
          closeWebSocket: () => this.closeWebSocket(),
          notifyReconnect: () => this.onReconnect(),
          notifyDisconnect: () => this.onDisconnect(),
          cancelPendingRequests: () => this.cancelPendingRequests(),
        },
        services: {
          pingService: () => this.sendPing(),
        },
      }),
    {
      logger: log.log.bind(log),
    },
  );

  private readonly openListener = () => {
    if (this.webSocket === undefined) {
      throw new Error('Open listener called without a WebSocket');
    }
    const {
      webSocket: { protocol },
    } = this;
    if (protocol === XTEXT_SUBPROTOCOL_V1) {
      this.interpreter.send('OPENED');
    } else {
      this.interpreter.send({
        type: 'ERROR',
        message: `Unknown subprotocol ${protocol}`,
      });
    }
  };

  private readonly errorListener = (event: Event) => {
    log.error('WebSocket error', event);
    this.interpreter.send({ type: 'ERROR', message: 'WebSocket error' });
  };

  private readonly closeListener = ({ code, reason }: CloseEvent) =>
    this.interpreter.send({
      type: 'ERROR',
      message: `Socket closed unexpectedly: ${code} ${reason}`,
    });

  private readonly messageListener = ({ data }: MessageEvent) => {
    if (typeof data !== 'string') {
      this.interpreter.send({
        type: 'ERROR',
        message: 'Unexpected message format',
      });
      return;
    }
    let json: unknown;
    try {
      json = JSON.parse(data);
    } catch (error) {
      log.error('JSON parse error', error);
      this.interpreter.send({ type: 'ERROR', message: 'Malformed message' });
      return;
    }
    const responseResult = XtextResponse.safeParse(json);
    if (!responseResult.success) {
      log.error('Xtext response', json, 'is malformed:', responseResult.error);
      this.interpreter.send({ type: 'ERROR', message: 'Malformed message' });
      return;
    }
    const { data: response } = responseResult;
    if ('service' in response) {
      // `XtextWebPushMessage.push` is optional, but `service` is not.
      const { resource, stateId, service, push } = response;
      this.onPush(resource, stateId, service, push);
      return;
    }
    const { id } = response;
    const task = this.pendingRequests.get(id);
    if (task === undefined) {
      log.warn('Task', id, 'has been already resolved');
      return;
    }
    this.removeTask(id);
    if ('error' in response) {
      const formattedMessage = `${response.error} error: ${response.message}`;
      log.error('Task', id, formattedMessage);
      task.reject(new Error(formattedMessage));
    } else {
      task.resolve(response.response);
    }
  };

  constructor(
    private readonly onReconnect: ReconnectHandler,
    private readonly onDisconnect: DisconnectHandler,
    private readonly onPush: PushHandler,
  ) {
    makeAutoObservable<
      XtextWebSocketClient,
      | 'stateAtom'
      | 'webSocket'
      | 'interpreter'
      | 'openListener'
      | 'errorListener'
      | 'closeListener'
      | 'messageListener'
      | 'sendPing'
    >(this, {
      stateAtom: false,
      webSocket: observable.ref,
      interpreter: false,
      openListener: false,
      errorListener: false,
      closeListener: false,
      messageListener: false,
      sendPing: false,
    });
  }

  start(): void {
    this.interpreter
      .onTransition((state, event) => {
        log.trace('WebSocke state transition', state.value, 'on event', event);
        this.stateAtom.reportChanged();
      })
      .start();

    this.interpreter.send(window.navigator.onLine ? 'ONLINE' : 'OFFLINE');
    window.addEventListener('offline', () => this.interpreter.send('OFFLINE'));
    window.addEventListener('online', () => this.interpreter.send('ONLINE'));
    this.updateVisibility();
    document.addEventListener('visibilitychange', () =>
      this.updateVisibility(),
    );
    window.addEventListener('pagehide', () =>
      this.interpreter.send('PAGE_HIDE'),
    );
    window.addEventListener('pageshow', () => {
      this.updateVisibility();
      this.interpreter.send('PAGE_SHOW');
    });
    // https://developer.chrome.com/blog/page-lifecycle-api/#new-features-added-in-chrome-68
    if ('wasDiscarded' in document) {
      document.addEventListener('freeze', () =>
        this.interpreter.send('PAGE_FREEZE'),
      );
      document.addEventListener('resume', () =>
        this.interpreter.send('PAGE_RESUME'),
      );
    }
    this.interpreter.send('CONNECT');
  }

  get state() {
    this.stateAtom.reportObserved();
    return this.interpreter.state;
  }

  get opening(): boolean {
    return this.state.matches('connection.socketCreated.open.opening');
  }

  get opened(): boolean {
    return this.state.matches('connection.socketCreated.open.opened');
  }

  get disconnectedByUser(): boolean {
    return this.state.matches('connection.disconnected');
  }

  get networkMissing(): boolean {
    return (
      this.state.matches('connection.temporarilyOffline') ||
      (this.disconnectedByUser &&
        this.state.matches('network.offline') &&
        !isWebSocketURLLocal(this.state.context.webSocketURL))
    );
  }

  get errors(): string[] {
    return this.state.context.errors;
  }

  connect(): void {
    this.interpreter.send('CONNECT');
  }

  disconnect(): void {
    this.interpreter.send('DISCONNECT');
  }

  forceReconnectOnError(): void {
    this.interpreter.send({
      type: 'ERROR',
      message: 'Client error',
    });
  }

  send(request: unknown): Promise<unknown> {
    if (!this.opened || this.webSocket === undefined) {
      throw new Error('Not connected');
    }

    const id = nanoid();

    const promise = new Promise((resolve, reject) => {
      const task = new PendingTask(resolve, reject, REQUEST_TIMEOUT, () => {
        this.interpreter.send({
          type: 'ERROR',
          message: 'Connection timed out',
        });
        this.removeTask(id);
      });
      this.pendingRequests.set(id, task);
    });

    const webRequest: XtextWebRequest = { id, request };
    const json = JSON.stringify(webRequest);
    this.webSocket.send(json);

    return promise;
  }

  private updateVisibility(): void {
    this.interpreter.send(document.hidden ? 'TAB_HIDDEN' : 'TAB_VISIBLE');
  }

  private openWebSocket(webSocketURL: string | undefined): void {
    if (this.webSocket !== undefined) {
      throw new Error('WebSocket already open');
    }

    if (webSocketURL === undefined) {
      throw new Error('URL not configured');
    }

    log.debug('Creating WebSocket');

    this.webSocket = new WebSocket(webSocketURL, XTEXT_SUBPROTOCOL_V1);
    this.webSocket.addEventListener('open', this.openListener);
    this.webSocket.addEventListener('close', this.closeListener);
    this.webSocket.addEventListener('error', this.errorListener);
    this.webSocket.addEventListener('message', this.messageListener);
  }

  private closeWebSocket() {
    if (this.webSocket === undefined) {
      throw new Error('WebSocket already closed');
    }

    log.debug('Closing WebSocket');

    this.webSocket.removeEventListener('open', this.openListener);
    this.webSocket.removeEventListener('close', this.closeListener);
    this.webSocket.removeEventListener('error', this.errorListener);
    this.webSocket.removeEventListener('message', this.messageListener);
    this.webSocket.close(1000, 'Closing connection');
    this.webSocket = undefined;
  }

  private removeTask(id: string): void {
    this.pendingRequests.delete(id);
  }

  private cancelPendingRequests(): void {
    this.pendingRequests.forEach((task) =>
      task.reject(new CancelledError('Closing connection')),
    );
    this.pendingRequests.clear();
  }

  private async sendPing(): Promise<void> {
    const ping = nanoid();
    const result = await this.send({ ping });
    const { pong } = PongResult.parse(result);
    if (ping !== pong) {
      throw new Error(`Expected pong ${ping} but got ${pong} instead`);
    }
  }
}
