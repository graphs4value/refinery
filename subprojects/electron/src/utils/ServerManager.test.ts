/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import type { ChildProcess } from 'node:child_process';
import { EventEmitter } from 'node:events';

import { treeKillSync } from '@alloc/tree-kill';
import { afterEach, beforeEach, describe, expect, test, vi } from 'vitest';

import ServerManager, {
  HEALTH_POLL_INTERVAL,
  KILL_TIMEOUT,
  RESTART_DELAY,
  STARTUP_TIMEOUT,
  STOP_TIMEOUT,
} from './ServerManager';

vi.mock('@alloc/tree-kill', () => ({
  default: vi.fn(),
  treeKillSync: vi.fn(),
}));

const mockedTreeKillSync = vi.mocked(treeKillSync);

/** A minimal `ChildProcess` stand-in: an `EventEmitter` with a spied `kill`. */
class FakeChildProcess extends EventEmitter {
  readonly kill = vi.fn(() => true);
}

function asChildProcess(child: FakeChildProcess): ChildProcess {
  return child as unknown as ChildProcess;
}

class TestServerManager extends ServerManager {
  readonly spawnChild = vi.fn<() => ChildProcess | Promise<ChildProcess>>();

  readonly checkHealthy = vi.fn<() => Promise<boolean>>();

  constructor(treeKill = true) {
    super('test', treeKill);
  }
}

/** Flushes pending microtasks (e.g. an already-resolved mock promise). */
async function flush(): Promise<void> {
  await vi.advanceTimersByTimeAsync(0);
}

/** Drives `manager` from `stopped` to `started` and returns the fake child. */
async function startServer(
  manager: TestServerManager,
): Promise<FakeChildProcess> {
  const child = new FakeChildProcess();
  manager.spawnChild.mockResolvedValue(asChildProcess(child));
  manager.checkHealthy.mockResolvedValue(true);
  const startPromise = manager.start();
  await flush();
  child.emit('spawn');
  await startPromise;
  return child;
}

beforeEach(() => {
  vi.useFakeTimers();
});

afterEach(() => {
  vi.clearAllTimers();
  vi.useRealTimers();
  mockedTreeKillSync.mockClear();
});

describe('start()', () => {
  test('resolves and transitions to `started` once the child is healthy', async () => {
    const manager = new TestServerManager();
    const healthChanged = vi.fn();
    manager.on('health-changed', healthChanged);

    await startServer(manager);

    expect(manager.status).toBe('started');
    expect(manager.healthy).toBe(true);
    expect(healthChanged).toHaveBeenCalledExactlyOnceWith(true);
  });

  test('rejects if the server is already running', async () => {
    const manager = new TestServerManager();
    await startServer(manager);

    await expect(manager.start()).rejects.toThrow('Server is already running');
  });

  test('retries the health check until the child reports healthy', async () => {
    const manager = new TestServerManager();
    const child = new FakeChildProcess();
    manager.spawnChild.mockResolvedValue(asChildProcess(child));
    manager.checkHealthy
      .mockResolvedValueOnce(false)
      .mockResolvedValueOnce(false)
      .mockResolvedValueOnce(true);

    const startPromise = manager.start();
    await flush();
    child.emit('spawn');
    // Two failed polls, `HEALTH_POLL_INTERVAL` apart, before the third one succeeds.
    await vi.advanceTimersByTimeAsync(2 * HEALTH_POLL_INTERVAL);
    await startPromise;

    expect(manager.checkHealthy).toHaveBeenCalledTimes(3);
    expect(manager.status).toBe('started');
  });

  test('rejects and kills the child if startup takes too long', async () => {
    const manager = new TestServerManager();
    const child = new FakeChildProcess();
    manager.spawnChild.mockResolvedValue(asChildProcess(child));
    manager.checkHealthy.mockResolvedValue(false);

    const startPromise = manager.start();
    await flush();
    // Attach the rejection assertion before advancing time, so the promise
    // never rejects without a handler already in place.
    const rejection = expect(startPromise).rejects.toThrow(
      'Timed out waiting for server to start',
    );
    // Never emit `spawn`, so the child never becomes healthy.
    await vi.advanceTimersByTimeAsync(STARTUP_TIMEOUT);
    await rejection;
    expect(manager.status).toBe('stopped');
    expect(mockedTreeKillSync).toHaveBeenCalledExactlyOnceWith(
      asChildProcess(child),
      'SIGKILL',
    );
  });

  test('rejects if the child fails to spawn', async () => {
    const manager = new TestServerManager();
    manager.spawnChild.mockRejectedValue(new Error('ENOENT'));

    await expect(manager.start()).rejects.toThrow('Failed to spawn server');
    expect(manager.status).toBe('stopped');
  });

  test('rejects if the child exits before becoming healthy', async () => {
    const manager = new TestServerManager();
    const child = new FakeChildProcess();
    manager.spawnChild.mockResolvedValue(asChildProcess(child));

    const startPromise = manager.start();
    await flush();
    child.emit('exit', 1, null);

    await expect(startPromise).rejects.toThrow(
      'Server exited during startup (code=1, signal=null)',
    );
    expect(manager.status).toBe('stopped');
  });

  test('rejects if the child emits an error before becoming healthy', async () => {
    const manager = new TestServerManager();
    const child = new FakeChildProcess();
    manager.spawnChild.mockResolvedValue(asChildProcess(child));

    const startPromise = manager.start();
    await flush();
    child.emit('error', new Error('spawn EACCES'));

    await expect(startPromise).rejects.toThrow('Server process error');
    expect(manager.status).toBe('stopped');
  });
});

describe('crash recovery', () => {
  test('restarts the server after it exits unexpectedly while started', async () => {
    const manager = new TestServerManager();
    const healthChanged = vi.fn();
    const child = await startServer(manager);
    manager.on('health-changed', healthChanged);

    const nextChild = new FakeChildProcess();
    manager.spawnChild.mockResolvedValue(asChildProcess(nextChild));
    child.emit('exit', 1, null);

    expect(manager.status).toBe('restarting');
    expect(healthChanged).toHaveBeenCalledExactlyOnceWith(false);

    await vi.advanceTimersByTimeAsync(RESTART_DELAY);
    expect(manager.spawnChild).toHaveBeenCalledTimes(2);
  });

  test('restarts the server after an error event while started', async () => {
    const manager = new TestServerManager();
    const healthChanged = vi.fn();
    const child = await startServer(manager);
    manager.on('health-changed', healthChanged);

    const nextChild = new FakeChildProcess();
    manager.spawnChild.mockResolvedValue(asChildProcess(nextChild));
    child.emit('error', new Error('boom'));

    expect(manager.status).toBe('restarting');
    expect(healthChanged).toHaveBeenCalledExactlyOnceWith(false);
    expect(mockedTreeKillSync).toHaveBeenCalledExactlyOnceWith(
      asChildProcess(child),
      'SIGKILL',
    );

    await vi.advanceTimersByTimeAsync(RESTART_DELAY);
    expect(manager.spawnChild).toHaveBeenCalledTimes(2);
  });

  test('keeps retrying to respawn if a restart attempt itself fails to spawn', async () => {
    const manager = new TestServerManager();
    const child = await startServer(manager);

    manager.spawnChild.mockRejectedValue(new Error('spawn failed'));
    child.emit('exit', 1, null);
    expect(manager.status).toBe('restarting');

    await vi.advanceTimersByTimeAsync(RESTART_DELAY);
    await flush();
    // The retry itself failed to spawn; another restart should be scheduled
    // instead of the failure propagating anywhere.
    expect(manager.status).toBe('restarting');

    await vi.advanceTimersByTimeAsync(RESTART_DELAY);
    await flush();
    expect(manager.spawnChild).toHaveBeenCalledTimes(3);
  });

  test('ignores a stale exit event from a child that was already superseded', async () => {
    const manager = new TestServerManager();
    const originalChild = await startServer(manager);

    const nextChild = new FakeChildProcess();
    manager.spawnChild.mockResolvedValue(asChildProcess(nextChild));
    // An `error` (not `exit`) triggers the restart, so `originalChild`'s
    // `exit` listener is still armed for a later, now-stale `exit` event.
    originalChild.emit('error', new Error('boom'));
    expect(manager.status).toBe('restarting');

    await vi.advanceTimersByTimeAsync(RESTART_DELAY);
    await flush();
    expect(manager.status).toBe('starting');

    originalChild.emit('exit', 1, null);
    expect(manager.status).toBe('starting');
  });
});

describe('stop()', () => {
  test('sends SIGTERM and settles once the child exits', async () => {
    const manager = new TestServerManager();
    const child = await startServer(manager);
    const healthChanged = vi.fn();
    manager.on('health-changed', healthChanged);

    const onStopped = vi.fn();
    void manager.stop().then(onStopped);
    expect(manager.status).toBe('stopping');
    expect(healthChanged).toHaveBeenCalledExactlyOnceWith(false);
    expect(mockedTreeKillSync).not.toHaveBeenCalled();
    await flush();
    expect(mockedTreeKillSync).toHaveBeenCalledExactlyOnceWith(
      asChildProcess(child),
      'SIGTERM',
    );

    child.emit('exit', 0, null);
    await flush();
    expect(manager.status).toBe('stopped');
    expect(mockedTreeKillSync).not.toHaveBeenCalledWith(
      asChildProcess(child),
      'SIGKILL',
    );
    expect(onStopped).toHaveBeenCalledOnce();
  });

  test('escalates to SIGKILL if the child does not exit in time', async () => {
    const manager = new TestServerManager();
    const child = await startServer(manager);

    void manager.stop();
    await vi.advanceTimersByTimeAsync(KILL_TIMEOUT);

    expect(mockedTreeKillSync).toHaveBeenCalledWith(
      asChildProcess(child),
      'SIGKILL',
    );
  });

  test('gives up waiting and settles anyway if the child never reports exiting', async () => {
    const manager = new TestServerManager();
    await startServer(manager);

    const onStopped = vi.fn();
    void manager.stop().then(onStopped);

    // Even the `SIGKILL` escalation is ignored here, so `exit` never fires.
    await vi.advanceTimersByTimeAsync(STOP_TIMEOUT - 1);
    expect(onStopped).not.toHaveBeenCalled();

    await vi.advanceTimersByTimeAsync(1);
    expect(onStopped).toHaveBeenCalledOnce();
  });

  test('is a no-op when the server is already stopped', () => {
    const manager = new TestServerManager();
    expect(() => manager.stop()).not.toThrow();
    expect(manager.status).toBe('stopped');
  });

  test('is a no-op when the server is already stopping', async () => {
    const manager = new TestServerManager();
    const child = await startServer(manager);

    void manager.stop();
    void manager.stop();

    await flush();
    expect(mockedTreeKillSync).toHaveBeenCalledExactlyOnceWith(
      asChildProcess(child),
      'SIGTERM',
    );
    expect(manager.status).toBe('stopping');
  });

  test('cancels a pending restart', async () => {
    const manager = new TestServerManager();
    const child = await startServer(manager);
    child.emit('exit', 1, null);
    expect(manager.status).toBe('restarting');

    void manager.stop();
    expect(manager.status).toBe('stopped');

    await vi.advanceTimersByTimeAsync(RESTART_DELAY);
    // No respawn should follow the cancelled restart.
    expect(manager.spawnChild).toHaveBeenCalledTimes(1);
  });

  test('aborts a pending startup and stops the not-yet-healthy child', async () => {
    const manager = new TestServerManager();
    const child = new FakeChildProcess();
    manager.spawnChild.mockResolvedValue(asChildProcess(child));
    manager.checkHealthy.mockResolvedValue(false);

    const startPromise = manager.start();
    await flush();
    expect(manager.status).toBe('starting');

    void manager.stop();

    await expect(startPromise).rejects.toThrow('Server startup aborted');
    expect(manager.status).toBe('stopping');
    await flush();
    expect(mockedTreeKillSync).toHaveBeenCalledExactlyOnceWith(
      asChildProcess(child),
      'SIGTERM',
    );

    child.emit('exit', 0, null);
    expect(manager.status).toBe('stopped');
  });

  test('aborts a pending spawn and reaps the orphaned child', async () => {
    const manager = new TestServerManager();
    const child = new FakeChildProcess();
    let resolveSpawn!: (value: ChildProcess) => void;
    manager.spawnChild.mockImplementation(
      () =>
        new Promise<ChildProcess>((resolve) => {
          resolveSpawn = resolve;
        }),
    );

    const startPromise = manager.start();
    await flush();
    void manager.stop();

    await expect(startPromise).rejects.toThrow('Server startup aborted');
    expect(manager.status).toBe('stopped');

    // The spawn that was already in flight resolves after the abort; the
    // orphaned child it produced should be reaped rather than kept around.
    resolveSpawn(asChildProcess(child));
    await flush();
    expect(mockedTreeKillSync).toHaveBeenCalledExactlyOnceWith(
      asChildProcess(child),
      'SIGKILL',
    );
  });
});

describe('treeKill flag', () => {
  test('uses treeKillSync when enabled (the default)', async () => {
    const manager = new TestServerManager(true);
    const child = await startServer(manager);

    void manager.stop();

    await flush();
    expect(mockedTreeKillSync).toHaveBeenCalledExactlyOnceWith(
      asChildProcess(child),
      'SIGTERM',
    );
    expect(child.kill).not.toHaveBeenCalled();
  });

  test('calls child.kill() directly when disabled', async () => {
    const manager = new TestServerManager(false);
    const child = await startServer(manager);

    void manager.stop();

    await flush();
    expect(child.kill).toHaveBeenCalledExactlyOnceWith('SIGTERM');
    expect(mockedTreeKillSync).not.toHaveBeenCalled();
  });
});
