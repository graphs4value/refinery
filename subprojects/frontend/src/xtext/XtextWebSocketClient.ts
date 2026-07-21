/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { makeAutoObservable, observable } from 'mobx';
import ms from 'ms';
import { nanoid } from 'nanoid';
import sjson from 'secure-json-parse';

import CancelledError from '../utils/CancelledError';
import PendingTask from '../utils/PendingTask';
import getLogger from '../utils/getLogger';

import WebSocketMachine from './WebSocketMachine';
import type { BackendConfigWithDefaults } from './fetchBackendConfig';
import {
  type XtextWebPushService,
  XtextResponse,
  type XtextWebRequest,
} from './xtextMessages';
import { PongResult } from './xtextServiceResults';

const XTEXT_SUBPROTOCOL_V2 = 'tools.refinery.language.web.xtext.v2';

// Use a large enough timeout so that a request can complete successfully
// even if the browser has throttled the background tab.
const REQUEST_TIMEOUT = ms('5s');

const log = getLogger('xtext.XtextWebSocketClient');

// The browser can report the network as offline (e.g. with Wi-Fi disabled)
// even though a loopback backend is still perfectly reachable.
function isLocalBackend(webSocketURL: string): boolean {
  const { hostname } = new URL(webSocketURL);
  return (
    hostname === 'localhost' ||
    hostname === '::1' ||
    hostname === '[::1]' ||
    /^127(\.\d{1,3}){3}$/.test(hostname)
  );
}

export type ReconnectHandler = () => void;

export type DisconnectHandler = () => void;

export type PushHandler = (
  resourceId: string,
  stateId: string,
  service: XtextWebPushService,
  data: unknown,
) => void;

export default class XtextWebSocketClient {
  private webSocket: WebSocket | undefined;

  private readonly pendingRequests = new Map<string, PendingTask<unknown>>();

  private readonly machine: WebSocketMachine;

  private readonly openListener = () => {
    if (this.webSocket === undefined) {
      throw new Error('Open listener called without a WebSocket');
    }
    const {
      webSocket: { protocol },
    } = this;
    if (protocol === XTEXT_SUBPROTOCOL_V2) {
      this.machine.socketOpened();
    } else {
      this.machine.reportError(`Unknown subprotocol ${protocol}`);
    }
  };

  private readonly errorListener = (event: Event) => {
    log.error({ err: event }, 'WebSocket error');
    this.machine.reportError('WebSocket error');
  };

  private readonly closeListener = ({ code, reason }: CloseEvent) =>
    this.machine.reportError(`Socket closed unexpectedly: ${code} ${reason}`);

  private readonly messageListener = ({ data }: MessageEvent) => {
    if (typeof data !== 'string') {
      this.machine.reportError('Unexpected message format');
      return;
    }
    let json: unknown;
    try {
      json = sjson.parse(data, undefined, {
        constructorAction: 'error',
        protoAction: 'error',
      });
    } catch (err) {
      log.error({ err }, 'JSON parse error');
      this.machine.reportError('Malformed message');
      return;
    }
    const responseResult = XtextResponse.safeParse(json);
    if (!responseResult.success) {
      log.error(
        { err: responseResult.error, response: json },
        'Malformed Xtext response',
      );
      this.machine.reportError('Malformed message');
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
      log.warn('Task %s has been already resolved', id);
      return;
    }
    this.removeTask(id);
    if ('error' in response) {
      const formattedMessage = `${response.error} error: ${response.message}`;
      log.error({ response }, 'Task %s failed: %s', id, formattedMessage);
      task.reject(new Error(formattedMessage));
    } else {
      task.resolve(response.response);
    }
  };

  constructor(
    private readonly backendConfig: BackendConfigWithDefaults,
    private readonly onReconnect: ReconnectHandler,
    private readonly onDisconnect: DisconnectHandler,
    private readonly onPush: PushHandler,
  ) {
    const ignoreNetworkStatus =
      import.meta.env.DEV || isLocalBackend(backendConfig.webSocketURL);
    this.machine = new WebSocketMachine(
      {
        openWebSocket: () => this.openWebSocket(),
        closeWebSocket: () => this.closeWebSocket(),
        cancelPendingRequests: () => this.cancelPendingRequests(),
        notifyReconnect: () => this.onReconnect(),
        notifyDisconnect: () => this.onDisconnect(),
        sendPing: () => this.sendPing(),
      },
      { ignoreNetworkStatus },
    );
    makeAutoObservable<
      XtextWebSocketClient,
      | 'webSocket'
      | 'machine'
      | 'openListener'
      | 'openWebSocket'
      | 'errorListener'
      | 'closeListener'
      | 'messageListener'
      | 'sendPing'
    >(this, {
      webSocket: observable.ref,
      machine: false,
      openListener: false,
      openWebSocket: false,
      errorListener: false,
      closeListener: false,
      messageListener: false,
      sendPing: false,
    });
  }

  start(): void {
    this.machine.start();

    this.machine.setOnline(window.navigator.onLine);
    window.addEventListener('offline', () => this.machine.setOnline(false));
    window.addEventListener('online', () => this.machine.setOnline(true));
    this.updateVisibility();
    document.addEventListener('visibilitychange', () =>
      this.updateVisibility(),
    );
    window.addEventListener('pagehide', () => this.machine.setPageFrozen(true));
    window.addEventListener('pageshow', () => {
      this.updateVisibility();
      this.machine.setPageFrozen(false);
    });
    // https://developer.chrome.com/blog/page-lifecycle-api/#new-features-added-in-chrome-68
    if ('wasDiscarded' in document) {
      document.addEventListener('freeze', () =>
        this.machine.setPageFrozen(true),
      );
      document.addEventListener('resume', () =>
        this.machine.setPageFrozen(false),
      );
    }
    this.machine.connect();
  }

  get opening(): boolean {
    return this.machine.opening;
  }

  get opened(): boolean {
    return this.machine.opened;
  }

  get disconnectedByUser(): boolean {
    return this.machine.disconnectedByUser;
  }

  get networkMissing(): boolean {
    return this.machine.networkMissing;
  }

  get errors(): readonly string[] {
    return this.machine.errors;
  }

  connect(): void {
    this.machine.connect();
  }

  disconnect(): void {
    this.machine.disconnect();
  }

  forceReconnectOnError(): void {
    this.machine.reportError('Client error');
  }

  send(request: unknown): Promise<unknown> {
    if (!this.opened || this.webSocket === undefined) {
      throw new Error('Not connected');
    }

    const id = nanoid();

    const promise = new Promise((resolve, reject) => {
      const task = new PendingTask(resolve, reject, REQUEST_TIMEOUT, () => {
        this.machine.reportError('Connection timed out');
        this.removeTask(id);
      });
      this.pendingRequests.set(id, task);
    });

    const webRequest: XtextWebRequest = { id, request };
    const json = JSON.stringify(webRequest);
    this.webSocket.send(json);

    return promise;
  }

  setKeepAlive(keepAlive: boolean): void {
    this.machine.setKeepAlive(keepAlive);
  }

  private updateVisibility(): void {
    this.machine.setTabHidden(document.hidden);
  }

  private openWebSocket(): void {
    if (this.webSocket !== undefined) {
      throw new Error('WebSocket already open');
    }

    log.debug('Creating WebSocket');

    this.webSocket = new WebSocket(
      this.backendConfig.webSocketURL,
      XTEXT_SUBPROTOCOL_V2,
    );
    this.webSocket.addEventListener('open', this.openListener);
    this.webSocket.addEventListener('close', this.closeListener);
    this.webSocket.addEventListener('error', this.errorListener);
    this.webSocket.addEventListener('message', this.messageListener);
  }

  private closeWebSocket() {
    if (this.webSocket === undefined) {
      // We might get here when there is a network error before the socket is initialized
      // and we don't have to do anything to close it.
      return;
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
