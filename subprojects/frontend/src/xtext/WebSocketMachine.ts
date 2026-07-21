/*
 * SPDX-FileCopyrightText: 2021-2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { type IObservableArray, makeAutoObservable, observable } from 'mobx';
import ms from 'ms';

import getLogger from '../utils/getLogger';

const log = getLogger('xtext.webSocketMachine');

const ERROR_WAIT_TIMES = (['200ms', '1s', '5s', '30s'] as const).map(ms);
/** How long to wait for `OPENED` before giving up on a connection attempt. */
const OPEN_TIMEOUT = ms('10s');
/** How often to ping the server to detect a dropped connection. */
const PING_PERIOD = ms('10s');
/** How long to stay connected in the background before giving up. */
const IDLE_TIMEOUT = ms('5m');

export interface WebSocketMachineHandlers {
  /** Create the underlying `WebSocket` and start listening on it. */
  openWebSocket(): void;
  /** Tear down the underlying `WebSocket` created by {@link openWebSocket}. */
  closeWebSocket(): void;
  /** Reject any requests that are still waiting for a response. */
  cancelPendingRequests(): void;
  /** Called whenever the connection is (re-)established. */
  notifyReconnect(): void;
  /** Called whenever the connection is lost or given up on. */
  notifyDisconnect(): void;
  /** Send a ping over the open socket and resolve once the matching pong arrives. */
  sendPing(): Promise<void>;
}

export interface WebSocketMachineOptions {
  /**
   * Whether to close the connection when the tab is in the background and no
   * generation is running.
   *
   * Defaults to `true`. Set to `false` to keep the connection alive even
   * while the tab is hidden.
   */
  disconnectInBackground?: boolean;

  /**
   * Whether to ignore the browser's online/offline signal when deciding to
   * connect or disconnect.
   *
   * Defaults to `false`. The browser can incorrectly report the network as
   * offline (e.g. with Wi-Fi disabled) even though a backend reachable via
   * loopback is still available, so callers talking to such a backend should
   * set this to `true`.
   */
  ignoreNetworkStatus?: boolean;
}

type ConnectionState =
  | { readonly name: 'disconnected' }
  | { readonly name: 'waiting' }
  | {
      readonly name: 'errorWait';
      retryTimer: ReturnType<typeof setTimeout> | undefined;
    }
  | { readonly name: 'offline' }
  | { readonly name: 'pageHidden' }
  | {
      readonly name: 'connecting';
      openTimer: ReturnType<typeof setTimeout> | undefined;
    }
  | {
      readonly name: 'connected';
      pingTimer: ReturnType<typeof setTimeout> | undefined;
      idleTimer: ReturnType<typeof setTimeout> | undefined;
    };

/**
 * Explicit lifecycle of the Xtext web socket connection.
 *
 * ```
 *                ┌──────────────┐
 *                │ disconnected │ ◀┐
 *                └──────────────┘  │
 *                  │               │
 *                  │ connect()     │ disconnect()
 *                  ▼               │
 *                ┌──────────────┐  │
 *   ┌──────────▶ │    active    │ ─┘
 *   │            └──────────────┘
 *   │              │
 *   │ page shown   │ page hidden
 *   │              ▼
 *   │            ┌──────────────┐
 *   └─────────── │  pageHidden  │
 *                └──────────────┘
 * ```
 *
 * `active` expands into (entered through the initial substate `waiting`):
 *
 * ```
 *             online
 *   ┌──────────────────────┐
 *   │                      ▼
 * ┌─────────┐  !online   ┌─────────────────────────────────────────────────────┐
 * │ offline │ ◀───────── │                  waiting (initial)                  │ ◀┐
 * └─────────┘            └─────────────────────────────────────────────────────┘  │
 *                          │                  ▲                 ▲                 │
 *                          │ visible          │ hidden & idle   │ hidden & idle   │
 *                          ▼                  │ (immediate)     │ (after 5m)      │
 *                        ┌─────────────────┐  │                 │                 │
 *              ┌──────── │   connecting    │ ─┘                 │                 │
 *              │         └─────────────────┘                    │                 │
 *              │           │                                    │                 │
 *              │           │ socketOpened()                     │                 │ backoff
 *              │           ▼                                    │                 │
 *              │         ┌─────────────────┐                    │                 │
 *              │ error() │    connected    │ ───────────────────┘                 │
 *              │         └─────────────────┘                                      │
 *              │           │                                                      │
 *              │           │ error()                                              │
 *              │           ▼                                                      │
 *              │         ┌─────────────────┐                                      │
 *              └───────▶ │    errorWait    │ ─────────────────────────────────────┘
 *                        └─────────────────┘
 * ```
 *
 * Both `hidden & idle` transitions require the tab to be hidden and no
 * generation to be running: aborting a pending `connecting` attempt happens
 * immediately, while disconnecting an idle `connected` socket happens after
 * five minutes. `disconnect()` and a hidden/frozen page reach `disconnected`
 * and `pageHidden` from any of these states, exactly as shown above for
 * `active` as a whole.
 *
 * Every timer captures the exact {@link ConnectionState} object it was
 * created for and only acts while `this.state` still points at it, so a
 * stale timer from a superseded phase becomes a no-op automatically.
 */
export default class WebSocketMachine {
  private state: ConnectionState = { name: 'disconnected' };

  private online = true;

  private tabHidden = false;

  private keepAlive = false;

  private errorMessages: IObservableArray<string> = observable<string>([]);

  private readonly disconnectInBackground: boolean;

  private readonly ignoreNetworkStatus: boolean;

  constructor(
    private readonly handlers: WebSocketMachineHandlers,
    options: WebSocketMachineOptions = {},
  ) {
    this.disconnectInBackground = options.disconnectInBackground ?? true;
    this.ignoreNetworkStatus = options.ignoreNetworkStatus ?? false;
    makeAutoObservable<
      WebSocketMachine,
      'state' | 'handlers' | 'disconnectInBackground' | 'ignoreNetworkStatus'
    >(this, {
      // `state` is a plain object replaced wholesale on every transition, and
      // timers compare it by reference to detect a superseded phase, so it
      // must stay a plain reference rather than a deep MobX proxy.
      state: observable.ref,
      handlers: false,
      disconnectInBackground: false,
      ignoreNetworkStatus: false,
    });
  }

  get opening(): boolean {
    return this.state.name === 'connecting';
  }

  get opened(): boolean {
    return this.state.name === 'connected';
  }

  get disconnectedByUser(): boolean {
    return this.state.name === 'disconnected';
  }

  get networkMissing(): boolean {
    return (
      this.state.name === 'offline' ||
      (this.state.name === 'disconnected' && !this.effectivelyOnline)
    );
  }

  get errors(): readonly string[] {
    return this.errorMessages;
  }

  private get mayDisconnect(): boolean {
    return this.disconnectInBackground && this.tabHidden && !this.keepAlive;
  }

  private get effectivelyOnline(): boolean {
    return this.ignoreNetworkStatus || this.online;
  }

  /** Fires the notifications for the initial `disconnected` phase. */
  start(): void {
    this.handlers.notifyDisconnect();
  }

  connect(): void {
    if (this.state.name === 'connecting' || this.state.name === 'connected') {
      // Already trying to connect or connected, nothing to do.
      return;
    }
    this.leaveCurrentState();
    this.enterWaiting();
  }

  disconnect(): void {
    this.leaveCurrentState();
    this.enterDisconnected();
  }

  /** Called once the underlying `WebSocket` has completed its handshake. */
  socketOpened(): void {
    if (this.state.name !== 'connecting') {
      return;
    }
    if (this.state.openTimer !== undefined) {
      clearTimeout(this.state.openTimer);
    }
    this.errorMessages.clear();
    this.handlers.notifyReconnect();
    const state: ConnectionState = {
      name: 'connected',
      pingTimer: undefined,
      idleTimer: undefined,
    };
    this.setState(state);
    this.startPingWait(state);
    this.updateIdleTimer(state);
  }

  /** Reports a connection-level error, only meaningful while trying to connect or connected. */
  reportError(message: string): void {
    if (this.state.name !== 'connecting' && this.state.name !== 'connected') {
      return;
    }
    this.leaveCurrentState();
    this.enterErrorWait(message);
  }

  setOnline(online: boolean): void {
    if (online === this.online) {
      return;
    }
    this.online = online;
    this.reevaluate();
  }

  setTabHidden(hidden: boolean): void {
    if (hidden === this.tabHidden) {
      return;
    }
    this.tabHidden = hidden;
    this.reevaluate();
  }

  setKeepAlive(running: boolean): void {
    if (running === this.keepAlive) {
      return;
    }
    this.keepAlive = running;
    this.reevaluate();
  }

  /** Whether the whole page (not just the tab) is hidden or frozen, e.g. in the bfcache. */
  setPageFrozen(frozen: boolean): void {
    if (frozen) {
      if (this.state.name === 'disconnected') {
        // A user-initiated disconnect is not affected by page visibility.
        return;
      }
      this.leaveCurrentState();
      this.enterPageHidden();
      return;
    }
    if (this.state.name !== 'pageHidden') {
      return;
    }
    this.leaveCurrentState();
    this.enterWaiting();
  }

  private setState(state: ConnectionState): void {
    this.state = state;
    log.trace({ phase: state.name }, 'WebSocket phase changed');
  }

  /** Cleans up any resources held by the current phase before leaving it. */
  private leaveCurrentState(): void {
    switch (this.state.name) {
      case 'errorWait':
        if (this.state.retryTimer !== undefined) {
          clearTimeout(this.state.retryTimer);
        }
        break;
      case 'connecting':
        if (this.state.openTimer !== undefined) {
          clearTimeout(this.state.openTimer);
        }
        this.handlers.cancelPendingRequests();
        this.handlers.closeWebSocket();
        break;
      case 'connected':
        if (this.state.pingTimer !== undefined) {
          clearTimeout(this.state.pingTimer);
        }
        if (this.state.idleTimer !== undefined) {
          clearTimeout(this.state.idleTimer);
        }
        this.handlers.cancelPendingRequests();
        this.handlers.closeWebSocket();
        break;
      default:
        break;
    }
  }

  /** Re-checks the current phase against the latest environment inputs. */
  private reevaluate(): void {
    switch (this.state.name) {
      case 'waiting':
        this.leaveCurrentState();
        this.enterWaiting();
        break;
      case 'errorWait':
        if (!this.effectivelyOnline) {
          this.leaveCurrentState();
          this.enterOffline();
        }
        break;
      case 'offline':
        if (this.effectivelyOnline) {
          this.leaveCurrentState();
          this.enterWaiting();
        }
        break;
      case 'connecting':
        if (this.mayDisconnect) {
          this.leaveCurrentState();
          this.enterWaiting();
        }
        break;
      case 'connected':
        this.updateIdleTimer(this.state);
        break;
      default:
        break;
    }
  }

  private enterDisconnected(): void {
    if (this.state.name === 'disconnected') {
      return;
    }
    this.errorMessages.clear();
    this.setState({ name: 'disconnected' });
    this.handlers.notifyDisconnect();
  }

  private enterWaiting(): void {
    if (!this.effectivelyOnline) {
      this.enterOffline();
      return;
    }
    if (this.tabHidden && this.disconnectInBackground) {
      if (this.state.name !== 'waiting') {
        this.setState({ name: 'waiting' });
      }
      return;
    }
    this.enterConnecting();
  }

  private enterOffline(): void {
    if (this.state.name === 'offline') {
      return;
    }
    this.errorMessages.clear();
    this.setState({ name: 'offline' });
    this.handlers.notifyDisconnect();
  }

  private enterPageHidden(): void {
    if (this.state.name === 'pageHidden') {
      return;
    }
    this.errorMessages.clear();
    this.setState({ name: 'pageHidden' });
  }

  private enterConnecting(): void {
    this.handlers.openWebSocket();
    const state: ConnectionState = { name: 'connecting', openTimer: undefined };
    this.setState(state);
    state.openTimer = setTimeout(() => {
      if (this.state === state) {
        this.reportError('Open timeout');
      }
    }, OPEN_TIMEOUT);
  }

  private enterErrorWait(message: string): void {
    this.errorMessages.push(message);
    if (!this.effectivelyOnline) {
      this.enterOffline();
      return;
    }
    // The first error waits the shortest amount of time, backing off from there.
    const index = Math.min(
      this.errorMessages.length - 1,
      ERROR_WAIT_TIMES.length - 1,
    );
    const delay = ERROR_WAIT_TIMES[index] ?? 0;
    const state: ConnectionState = { name: 'errorWait', retryTimer: undefined };
    this.setState(state);
    state.retryTimer = setTimeout(() => {
      if (this.state === state) {
        this.enterWaiting();
      }
    }, delay);
  }

  private startPingWait(state: ConnectionState & { name: 'connected' }): void {
    state.pingTimer = setTimeout(() => {
      if (this.state !== state) {
        return;
      }
      state.pingTimer = undefined;
      this.handlers
        .sendPing()
        .then(() => {
          if (this.state === state) {
            this.startPingWait(state);
          }
        })
        .catch((err: unknown) => {
          if (this.state === state) {
            this.reportError(String(err));
          }
        });
    }, PING_PERIOD);
  }

  /**
   * Starts, cancels, or leaves running the timer that disconnects an idle
   * background connection, depending on the current environment inputs.
   */
  private updateIdleTimer(
    state: ConnectionState & { name: 'connected' },
  ): void {
    if (this.mayDisconnect) {
      state.idleTimer ??= setTimeout(() => {
        if (this.state === state) {
          this.leaveCurrentState();
          this.enterWaiting();
        }
      }, IDLE_TIMEOUT);
      return;
    }
    if (state.idleTimer !== undefined) {
      clearTimeout(state.idleTimer);
      state.idleTimer = undefined;
    }
  }
}
