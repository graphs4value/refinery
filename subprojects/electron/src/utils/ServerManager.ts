/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { ChildProcess } from 'node:child_process';
import { EventEmitter } from 'node:events';

import { type ProcessLike, treeKillSync } from '@alloc/tree-kill';
import ms from 'ms';
import type { Logger } from 'pino';

import getLogger from '../logger/getLogger';

/** Delay between successive health-check polls while starting. */
export const HEALTH_POLL_INTERVAL = ms('100ms');
/** Delay before respawning after an unexpected termination. */
export const RESTART_DELAY = ms('1s');
/** Grace period between `SIGTERM` and `SIGKILL` when stopping. */
export const KILL_TIMEOUT = ms('1s');
/**
 * Upper bound on how long we wait for a freshly spawned server to become
 * healthy before giving up.
 */
export const STARTUP_TIMEOUT = ms('5m');
/**
 * Upper bound on how long {@link ServerManager['stop']}'s returned promise waits
 * for the child to actually exit before giving up on it, well past the
 * `SIGTERM`-to-`SIGKILL` escalation via {@link KILL_TIMEOUT}. Without this, a
 * child that somehow never reports `exit` (the promise has no way to reject)
 * would leave any awaiting caller -- e.g. a dependent cleanup step, or
 * Electron's own quit sequence -- hanging forever.
 */
export const STOP_TIMEOUT = ms('5s');

/** A value that may be available synchronously or via a promise. */
type Awaitable<T> = T | PromiseLike<T>;

interface StartResolvers {
  readonly resolve: () => void;
  readonly reject: (error: Error) => void;
}

/**
 * Explicit lifecycle of a managed server process.
 *
 * ```
 *                                exit
 *                   ┌───────────────────────────────────────────────┐
 *                   ▼                                               │
 *       stop()    ┌────────────┐                                    │
 *   ┌───────────▶ │  stopped   │ ◀┐                                 │
 *   │             └────────────┘  │                                 │
 *   │               │             │                                 │
 *   │               │ start()     │ error (user start)              │
 *   │               ▼             │                                 │
 *   │             ┌───────────────────────────────────┐  stop()   ┌──────────┐
 *   │    ┌──────▶ │             starting              │ ────────▶ │ stopping │
 *   │    │        └───────────────────────────────────┘           └──────────┘
 *   │    │          │             │                                 ▲
 *   │    │          │ healthy     │                                 │ stop()
 *   │    │          ▼             │                                 │
 *   │    │        ┌────────────┐  │                                 │
 *   │    │ timer  │  started   │ ─┼─────────────────────────────────┘
 *   │    │        └────────────┘  │
 *   │    │          │             │
 *   │    │          │ crash       │ error (auto)
 *   │    │          ▼             │
 *   │    │        ┌────────────┐  │
 *   │    └─────── │ restarting │ ◀┘
 *   │             └────────────┘
 *   │               │
 *   └───────────────┘
 * ```
 *
 * Every asynchronous callback captures the exact {@link State} object it was
 * created for and only acts while `this.state` still points at it. Because a
 * new object is minted on every transition, callbacks belonging to a superseded
 * child (late `exit`, stale poll, expired timer) become no-ops automatically.
 */
type State =
  | { readonly name: 'stopped' }
  | {
      readonly name: 'spawning';
      /** Present only for a user-initiated {@link ServerManager.start}. */
      readonly resolvers: StartResolvers | undefined;
    }
  | {
      readonly name: 'starting';
      readonly child: ChildProcess;
      /** Present only for a user-initiated {@link ServerManager.start}. */
      readonly resolvers: StartResolvers | undefined;
      pollTimer: NodeJS.Timeout | undefined;
      startupTimer: NodeJS.Timeout | undefined;
    }
  | {
      readonly name: 'started';
      readonly child: ChildProcess;
    }
  | {
      readonly name: 'stopping';
      readonly child: ChildProcess;
      readonly killTimer: NodeJS.Timeout;
    }
  | { readonly name: 'restarting'; readonly restartTimer: NodeJS.Timeout };

export type ServerStatus = State['name'];

interface ServerManagerEvents {
  'health-changed': [healthy: boolean];
}

export default abstract class ServerManager<
  SpawnOptions = undefined,
> extends EventEmitter<ServerManagerEvents> {
  private internalState: State = { name: 'stopped' };

  /** Resolvers for pending {@link stop} calls, flushed once `stopped` is reached. */
  private stopWaiters: (() => void)[] = [];

  protected readonly logger: Logger;

  constructor(
    name: string,
    private readonly treeKill = true,
  ) {
    super();
    this.logger = getLogger(name);
  }

  private get state(): State {
    return this.internalState;
  }

  /** Assigns the new state and emits `health-changed` if `healthy` flipped. */
  private set state(state: State) {
    const wasHealthy = this.healthy;
    this.internalState = state;
    if (this.healthy !== wasHealthy) {
      this.emit('health-changed', this.healthy);
    }
    if (state.name === 'stopped' && this.stopWaiters.length > 0) {
      const waiters = this.stopWaiters;
      this.stopWaiters = [];
      waiters.forEach((resolve) => resolve());
    }
  }

  /**
   * Spawns the server and resolves once it reports healthy. Rejects if the
   * process fails to come up. Must only be called from the `stopped` state.
   */
  start(options?: SpawnOptions): Promise<void> {
    if (this.state.name !== 'stopped') {
      return Promise.reject(new Error('Server is already running'));
    }
    return new Promise<void>((resolve, reject) => {
      this.spawn({ resolve, reject }, options);
    });
  }

  protected abstract spawnChild(
    options?: SpawnOptions,
  ): Awaitable<ChildProcess>;

  /**
   * Requests a graceful shutdown (`SIGTERM`, escalating to `SIGKILL`) and
   * cancels any pending restart. Idempotent and safe from any state. The
   * returned promise resolves once the child has actually exited.
   */
  stop(): Promise<void> {
    const { state } = this;
    switch (state.name) {
      case 'stopped':
      case 'stopping':
        break;
      case 'restarting':
        clearTimeout(state.restartTimer);
        this.state = { name: 'stopped' };
        break;
      case 'spawning':
        // No child exists yet; the pending spawn observes the state change and
        // reaps the process if it is already too late to cancel.
        this.state = { name: 'stopped' };
        state.resolvers?.reject(new Error('Server startup aborted'));
        break;
      case 'starting':
        this.clearStartupTimers(state);
        this.beginStopping(state.child);
        state.resolvers?.reject(new Error('Server startup aborted'));
        break;
      case 'started':
        this.beginStopping(state.child);
        break;
      default:
        break;
    }
    if (this.state.name === 'stopped') {
      return Promise.resolve();
    }
    return new Promise((resolve) => {
      const timer = setTimeout(() => {
        this.logger.error(
          'Server did not report exiting within the timeout, giving up waiting for it',
        );
        resolve();
      }, STOP_TIMEOUT);
      this.stopWaiters.push(() => {
        clearTimeout(timer);
        resolve();
      });
    });
  }

  get status(): ServerStatus {
    return this.state.name;
  }

  get healthy(): boolean {
    return this.status === 'started';
  }

  private get activeChild(): ChildProcess | undefined {
    const { state } = this;
    switch (state.name) {
      case 'starting':
      case 'started':
      case 'stopping':
        return state.child;
      case 'stopped':
      case 'spawning':
      case 'restarting':
      default:
        return undefined;
    }
  }

  /**
   * Launches a child and wires up its lifecycle. `resolvers` are supplied for a
   * user-initiated {@link start}; when absent this is an automatic restart whose
   * failures schedule another attempt instead of rejecting a caller.
   */
  private spawn(resolvers?: StartResolvers, options?: SpawnOptions): void {
    // Enter `spawning` synchronously so that a concurrent `stop` (or a repeated
    // `start`) is observed by the asynchronous {@link spawnChild} below via the
    // captured state object.
    const spawning: State = { name: 'spawning', resolvers };
    this.state = spawning;

    (async () => {
      let spawned: ChildProcess;
      try {
        spawned = await this.spawnChild(options);
      } catch (error) {
        if (this.state !== spawning) {
          // A `stop` aborted the spawn while `spawnChild` was in flight.
          return;
        }
        this.handleSpawnFailure(
          resolvers,
          new Error('Failed to spawn server', { cause: error }),
        );
        return;
      }
      if (this.state !== spawning) {
        // A `stop` aborted the spawn while `spawnChild` was in flight; the
        // freshly created child is now orphaned, so reap it and run its
        // teardown.
        this.sendSignal(spawned, 'SIGKILL');
        return;
      }
      this.beginStarting(spawned, resolvers);
    })().catch((err) => {
      this.logger.error({ err }, 'Unexpected error while spawning server');
    });
  }

  /** Wires up a freshly spawned child and begins polling for health. */
  private beginStarting(
    child: ChildProcess,
    resolvers: StartResolvers | undefined,
  ): void {
    const state: State = {
      name: 'starting',
      child,
      resolvers,
      pollTimer: undefined,
      startupTimer: undefined,
    };
    this.state = state;

    child.once('spawn', () => {
      if (this.state === state) {
        this.pollHealth(state);
      }
    });
    child.once('error', (error) => this.onError(child, error));
    child.once('exit', (code, signal) => this.onExit(child, code, signal));

    state.startupTimer = setTimeout(() => {
      if (this.state !== state) {
        return;
      }
      this.logger.error('Server did not become healthy in time, aborting');
      this.failStartup(
        state,
        new Error('Timed out waiting for server to start'),
      );
    }, STARTUP_TIMEOUT);
  }

  private pollHealth(state: State & { name: 'starting' }): void {
    (async () => {
      const healthy = await this.checkHealthy();
      if (this.state !== state) {
        // Superseded by a stop, restart, or exit while the request was in
        // flight; drop the result.
        return;
      }
      if (healthy) {
        this.clearStartupTimers(state);
        this.state = {
          name: 'started',
          child: state.child,
        };
        this.logger.info('Server started');
        state.resolvers?.resolve();
        return;
      }
      state.pollTimer = setTimeout(() => {
        if (this.state === state) {
          this.pollHealth(state);
        }
      }, HEALTH_POLL_INTERVAL);
    })().catch((err) => {
      this.logger.error({ err }, 'Unexpected error while polling for health');
    });
  }

  protected abstract checkHealthy(): Promise<boolean>;

  private onExit(
    child: ChildProcess,
    code: number | null,
    signal: NodeJS.Signals | null,
  ): void {
    if (this.activeChild !== child) {
      return;
    }
    const { state } = this;
    switch (state.name) {
      case 'starting':
        this.failStartup(
          state,
          new Error(
            `Server exited during startup (code=${code}, signal=${signal})`,
          ),
        );
        break;
      case 'started':
        this.logger.error(
          { exitCode: code, signal },
          'Server quit unexpectedly, restarting',
        );
        this.scheduleRestart();
        break;
      case 'stopping':
        clearTimeout(state.killTimer);
        this.state = { name: 'stopped' };
        break;
      default:
        break;
    }
  }

  private onError(child: ChildProcess, cause: unknown): void {
    if (this.activeChild !== child) {
      return;
    }
    const error = new Error('Server process error', { cause });
    const { state } = this;
    switch (state.name) {
      case 'starting':
        // A spawn failure emits `error` without a following `exit`.
        this.failStartup(state, error);
        break;
      case 'started':
        this.logger.error({ err: error }, 'Server errored, restarting');
        this.sendSignal(child, 'SIGKILL');
        this.scheduleRestart();
        break;
      case 'stopping':
        // The kill timer or a subsequent `exit` will finish the teardown.
        this.logger.warn({ err: error }, 'Server errored while stopping');
        break;
      default:
        break;
    }
  }

  /** Fails a `starting` state: rejects a caller, or retries if auto-restarting. */
  private failStartup(state: State & { name: 'starting' }, error: Error): void {
    this.clearStartupTimers(state);
    // Ensure an abandoned-but-alive child (e.g. startup timeout) is reaped.
    this.sendSignal(state.child, 'SIGKILL');
    this.handleSpawnFailure(state.resolvers, error);
  }

  private handleSpawnFailure(
    resolvers: StartResolvers | undefined,
    error: Error,
  ): void {
    if (resolvers) {
      this.state = { name: 'stopped' };
      resolvers.reject(error);
    } else {
      this.logger.error({ err: error }, 'Failed to start server, retrying');
      this.scheduleRestart();
    }
  }

  private scheduleRestart(): void {
    const restartTimer = setTimeout(() => {
      if (
        this.state.name === 'restarting' &&
        this.state.restartTimer === restartTimer
      ) {
        this.spawn();
      }
    }, RESTART_DELAY);
    this.state = { name: 'restarting', restartTimer };
  }

  private beginStopping(child: ChildProcess): void {
    const killTimer = setTimeout(
      () => this.sendSignal(child, 'SIGKILL'),
      KILL_TIMEOUT,
    );
    const stopping: State = { name: 'stopping', child, killTimer };
    this.state = stopping;
    // Queue SIGTERM after the unhealthy event has been emitted. IPC delivery
    // remains asynchronous, but this gives Electron a best-effort chance to
    // notify renderers before the child is killed.
    setImmediate(() => {
      if (this.state === stopping) {
        this.sendSignal(child, 'SIGTERM');
      }
    });
  }

  /** Best-effort tree kill that tolerates an already-dead process. */
  private sendSignal(child: ChildProcess, signal: NodeJS.Signals): void {
    try {
      if (this.treeKill) {
        treeKillSync(child as ProcessLike, signal);
      } else {
        child.kill(signal);
      }
    } catch {
      // The process (or one of its descendants) may have already exited.
    }
  }

  private clearStartupTimers(state: State & { name: 'starting' }): void {
    if (state.pollTimer) {
      clearTimeout(state.pollTimer);
      state.pollTimer = undefined;
    }
    if (state.startupTimer) {
      clearTimeout(state.startupTimer);
      state.startupTimer = undefined;
    }
  }
}
