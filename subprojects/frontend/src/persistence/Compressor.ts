/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { Visibility } from '@tools.refinery/client';

import getLogger from '../utils/getLogger';
import isElectron from '../utils/isElectron';

import {
  type CompressRequest,
  CompressorResponse,
  type CompressorVersion,
  type DecompressRequest,
  V2Payload,
} from './compressionMessages';
import CompressionWorker from './compressionWorker?worker';
import initialValue from './initialValue';
import { createShareFragment, parseShareFragment } from './shareURI';

const LOG = getLogger('persistence.Compressor');

export type DecompressCallback = (
  text: string,
  visibility: Record<string, Visibility> | undefined,
  source: DecompressSource,
) => void;

export type DecompressSource = 'initial' | 'openShare' | 'hashChange';

export type DecompressFailure = 'invalidFragment' | 'workerFailed';

export type DecompressErrorCallback = (
  source: DecompressSource,
  failure: DecompressFailure,
  error: Error,
) => void;

interface CompressionInput {
  version: CompressorVersion;
  text: string;
}

interface CompressionWaiter {
  resolve: (fragment: string) => void;
  reject: (error: Error) => void;
}

interface CompressionTask {
  type: 'compress';
  input: CompressionInput;
  updateLocation: boolean;
  waiters: CompressionWaiter[];
}

interface DecompressionTask {
  type: 'decompress';
  fragment: string;
  compressedText: string;
  version: CompressorVersion;
  source: DecompressSource;
}

type WorkerTask = CompressionTask | DecompressionTask;

type UpdateFragmentResult =
  'accepted' | 'invalid' | 'workerFailed' | 'disposed';

function createCompressionInput(
  text: string,
  visibility?: Record<string, Visibility>,
): CompressionInput {
  if (visibility === undefined || Object.keys(visibility).length === 0) {
    return { version: 1, text };
  }
  const payload = {
    t: text,
    v: visibility,
  } satisfies V2Payload;
  return { version: 2, text: JSON.stringify(payload) };
}

function sameCompressionInput(
  left: CompressionInput | undefined,
  right: CompressionInput,
): boolean {
  return left?.version === right.version && left.text === right.text;
}

export default class Compressor {
  private worker: Worker | undefined;

  private readonly hashChangeHandler = () => this.updateHash('hashChange');

  private fragment: string | undefined;

  private fragmentInput: CompressionInput | undefined;

  private readonly workerTasks: WorkerTask[] = [];

  private activeWorkerTask: WorkerTask | undefined;

  private workerError: Error | undefined;

  private disposed = false;

  constructor(
    private readonly onDecompressed: DecompressCallback,
    private readonly onDecompressError: DecompressErrorCallback,
  ) {
    if (!isElectron) {
      window.addEventListener('hashchange', this.hashChangeHandler);
    }
  }

  private getWorker(): Worker {
    if (this.worker !== undefined) {
      return this.worker;
    }
    const worker = new CompressionWorker();
    worker.onerror = (event) =>
      this.workerFailed(new Error(event.message || 'Compression worker error'));
    worker.onmessageerror = () =>
      this.workerFailed(
        new Error('Failed to receive compression worker message'),
      );
    worker.onmessage = (event) => this.processWorkerMessage(event.data);
    this.worker = worker;
    return worker;
  }

  decompressInitial(fragment = window.location.hash): void {
    if (fragment === '') {
      this.loadDefault('initial');
      return;
    }
    this.reportFragmentUpdateFailure(
      this.updateFragment(fragment, 'initial'),
      'initial',
      'The model link is invalid',
    );
  }

  decompress(fragment: string): void {
    this.reportFragmentUpdateFailure(
      this.updateFragment(fragment, 'openShare'),
      'openShare',
      'The shared link is invalid',
    );
  }

  private reportFragmentUpdateFailure(
    result: UpdateFragmentResult,
    source: DecompressSource,
    invalidMessage: string,
  ): void {
    if (result === 'invalid') {
      this.onDecompressError(
        source,
        'invalidFragment',
        new Error(invalidMessage),
      );
    } else if (result === 'workerFailed' && this.workerError !== undefined) {
      this.onDecompressError(source, 'workerFailed', this.workerError);
    }
  }

  compress(text: string, visibility?: Record<string, Visibility>): void {
    if (isElectron) {
      // No hash-based links in Electron, so no need to run the compressor.
      return;
    }
    this.enqueueCompression(createCompressionInput(text, visibility), true);
  }

  getShareFragment(
    text: string,
    visibility?: Record<string, Visibility>,
  ): Promise<string> {
    const input = createCompressionInput(text, visibility);
    if (
      this.fragment !== undefined &&
      sameCompressionInput(this.fragmentInput, input)
    ) {
      return Promise.resolve(this.fragment);
    }
    return new Promise((resolve, reject) => {
      this.enqueueCompression(input, !isElectron, { resolve, reject });
    });
  }

  private setCompressedFragment(
    input: CompressionInput,
    compressedText: string,
    updateLocation: boolean,
  ): string {
    const fragment = createShareFragment(input.version, compressedText);
    this.fragment = fragment;
    this.fragmentInput = input;
    if (updateLocation) {
      window.history.replaceState(null, '', fragment);
    }
    return fragment;
  }

  private enqueueCompression(
    input: CompressionInput,
    updateLocation: boolean,
    waiter?: CompressionWaiter,
  ): void {
    if (this.workerError !== undefined || this.disposed) {
      waiter?.reject(
        this.workerError ?? new Error('Compressor has been disposed'),
      );
      return;
    }
    const matchingTask = [this.activeWorkerTask, ...this.workerTasks].find(
      (task): task is CompressionTask =>
        task?.type === 'compress' && sameCompressionInput(task.input, input),
    );
    if (matchingTask !== undefined) {
      matchingTask.updateLocation ||= updateLocation;
      if (waiter !== undefined) {
        matchingTask.waiters.push(waiter);
      }
      return;
    }
    if (waiter === undefined) {
      const queuedBackgroundTask = this.workerTasks.find(
        (task): task is CompressionTask =>
          task.type === 'compress' && task.waiters.length === 0,
      );
      if (queuedBackgroundTask !== undefined) {
        queuedBackgroundTask.input = input;
        queuedBackgroundTask.updateLocation ||= updateLocation;
        return;
      }
    }
    this.workerTasks.push({
      type: 'compress',
      input,
      updateLocation,
      waiters: waiter === undefined ? [] : [waiter],
    });
    this.startNextWorkerTask();
  }

  private processDecompressed(version: CompressorVersion, text: string): void {
    const task =
      this.activeWorkerTask?.type === 'decompress'
        ? this.activeWorkerTask
        : undefined;
    if (task === undefined) {
      throw new Error('Missing decompression task');
    }
    const { source } = task;
    if (
      !isElectron &&
      source === 'hashChange' &&
      window.location.hash !== task.fragment
    ) {
      // A later browser navigation superseded this worker response.
      return;
    }
    if (version === 1) {
      this.fragment = task.fragment;
      this.fragmentInput = { version, text };
      this.updateLocationAfterDecompression(task);
      this.onDecompressed(text, undefined, source);
      return;
    }
    let payload: V2Payload;
    try {
      payload = V2Payload.parse(JSON.parse(text));
    } catch (err) {
      LOG.error({ err }, 'Failed to parse URI fragment payload');
      this.onDecompressError(
        source,
        'invalidFragment',
        err instanceof Error ? err : new Error(String(err)),
      );
      return;
    }
    this.fragment = task.fragment;
    this.fragmentInput = { version, text };
    this.updateLocationAfterDecompression(task);
    this.onDecompressed(payload.t, payload.v, source);
  }

  private updateLocationAfterDecompression(task: DecompressionTask): void {
    if (!isElectron && window.location.hash !== task.fragment) {
      window.history.replaceState(null, '', task.fragment);
    }
  }

  dispose(): void {
    if (this.disposed) {
      return;
    }
    window.removeEventListener('hashchange', this.hashChangeHandler);
    this.rejectCompressionWaiters(new Error('Compressor has been disposed'));
    this.activeWorkerTask = undefined;
    this.workerTasks.length = 0;
    this.worker?.terminate();
    this.worker = undefined;
    this.disposed = true;
  }

  private processWorkerMessage(data: unknown): void {
    try {
      const message = CompressorResponse.parse(data);
      const task = this.activeWorkerTask;
      if (task === undefined) {
        throw new Error('Unexpected compression worker response');
      }
      switch (message.response) {
        case 'compressed': {
          if (
            task.type !== 'compress' ||
            task.input.version !== message.version
          ) {
            throw new Error('Unexpected compressed response');
          }
          const fragment = this.setCompressedFragment(
            task.input,
            message.compressedText,
            task.updateLocation,
          );
          for (const { resolve } of task.waiters) {
            resolve(fragment);
          }
          break;
        }
        case 'decompressed':
          if (task.type !== 'decompress' || task.version !== message.version) {
            throw new Error('Unexpected decompressed response');
          }
          this.processDecompressed(message.version, message.text);
          break;
        case 'error': {
          const error = new Error(message.message);
          if (task.type === 'compress') {
            for (const { reject } of task.waiters) {
              reject(error);
            }
          } else {
            this.onDecompressError(task.source, 'invalidFragment', error);
          }
          LOG.error({ err: error }, 'Error processing compressor request');
          break;
        }
        default:
          throw new Error('Unknown response from compressor worker');
      }
      this.activeWorkerTask = undefined;
      this.startNextWorkerTask();
    } catch (err) {
      this.workerFailed(err instanceof Error ? err : new Error(String(err)));
    }
  }

  private startNextWorkerTask(): void {
    if (
      this.activeWorkerTask !== undefined ||
      this.workerError !== undefined ||
      this.disposed
    ) {
      return;
    }
    const task = this.workerTasks.shift();
    if (task === undefined) {
      return;
    }
    this.activeWorkerTask = task;
    if (task.type === 'compress') {
      this.getWorker().postMessage({
        request: 'compress',
        text: task.input.text,
        version: task.input.version,
      } satisfies CompressRequest);
    } else {
      this.getWorker().postMessage({
        request: 'decompress',
        compressedText: task.compressedText,
        version: task.version,
      } satisfies DecompressRequest);
    }
  }

  private rejectCompressionWaiters(error: Error): void {
    for (const task of [this.activeWorkerTask, ...this.workerTasks]) {
      if (task?.type === 'compress') {
        for (const { reject } of task.waiters) {
          reject(error);
        }
      }
    }
  }

  private workerFailed(error: Error): void {
    if (this.workerError !== undefined || this.disposed) {
      return;
    }
    this.workerError = error;
    this.rejectCompressionWaiters(error);
    if (this.activeWorkerTask?.type === 'decompress') {
      this.onDecompressError(
        this.activeWorkerTask.source,
        'workerFailed',
        error,
      );
    }
    this.activeWorkerTask = undefined;
    this.workerTasks.length = 0;
    this.worker?.terminate();
    this.worker = undefined;
    LOG.error({ err: error }, 'Compression worker failed');
  }

  private updateHash(source: DecompressSource): void {
    // Compression started for the previous document must not overwrite a
    // location the user reached with browser navigation.
    for (const task of [this.activeWorkerTask, ...this.workerTasks]) {
      if (task?.type === 'compress') {
        task.updateLocation = false;
      }
    }
    const { hash } = window.location;
    if (hash === '') {
      this.loadDefault(source);
      return;
    }
    this.reportFragmentUpdateFailure(
      this.updateFragment(hash, source),
      source,
      'The model link is invalid',
    );
  }

  private loadDefault(source: DecompressSource): void {
    this.fragment = undefined;
    this.fragmentInput = undefined;
    LOG.debug('Loading default source');
    this.onDecompressed(initialValue, undefined, source);
  }

  private updateFragment(
    fragment: string,
    source: DecompressSource,
  ): UpdateFragmentResult {
    if (fragment === this.fragment) {
      return 'accepted';
    }
    if (this.workerError !== undefined) {
      return 'workerFailed';
    }
    if (this.disposed) {
      return 'disposed';
    }
    const pendingTask = [this.activeWorkerTask, ...this.workerTasks].find(
      (task): task is DecompressionTask =>
        task?.type === 'decompress' && task.fragment === fragment,
    );
    if (pendingTask !== undefined) {
      pendingTask.source = source;
      return 'accepted';
    }
    const result = parseShareFragment(fragment);
    if (result === undefined) {
      return 'invalid';
    }
    this.fragmentInput = undefined;
    this.workerTasks.push({
      type: 'decompress',
      fragment,
      compressedText: result.text,
      version: result.version,
      source,
    });
    this.startNextWorkerTask();
    return 'accepted';
  }
}
