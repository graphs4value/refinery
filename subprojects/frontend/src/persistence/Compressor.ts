/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { Visibility } from '@tools.refinery/client';

import getLogger from '../utils/getLogger';

import {
  type CompressRequest,
  CompressorResponse,
  type CompressorVersion,
  type DecompressRequest,
  V2Payload,
} from './compressionMessages';
import CompressionWorker from './compressionWorker?worker';
import initialValue from './initialValue';

const LOG = getLogger('persistence.Compressor');

const FRAGMENT_PREFIX_V1 = '#/1/';

const FRAGMENT_PREFIX_V2 = '#/2/';

export type DecompressCallback = (
  text: string,
  visibility?: Record<string, Visibility>,
) => void;

function toFragment(version: CompressorVersion, value: string): string {
  switch (version) {
    case 1:
      return `${FRAGMENT_PREFIX_V1}${value}`;
    case 2:
      return `${FRAGMENT_PREFIX_V2}${value}`;
    default:
      throw new Error(`Unsupported compressor version: ${String(version)}`);
  }
}

function fromFragment(
  fragment: string,
): { version: CompressorVersion; text: string } | undefined {
  if (fragment.startsWith(FRAGMENT_PREFIX_V1)) {
    return { version: 1, text: fragment.slice(FRAGMENT_PREFIX_V1.length) };
  }
  if (fragment.startsWith(FRAGMENT_PREFIX_V2)) {
    return { version: 2, text: fragment.slice(FRAGMENT_PREFIX_V2.length) };
  }
  return undefined;
}

export default class Compressor {
  private readonly worker = new CompressionWorker();

  private readonly hashChangeHandler = () => this.updateHash();

  private fragment: string | undefined;

  private compressing = false;

  private toCompress: string | undefined;

  private nextVersion: CompressorVersion = 1;

  constructor(private readonly onDecompressed: DecompressCallback) {
    this.worker.onerror = (error) => LOG.error('Worker error', error);
    this.worker.onmessageerror = (error) =>
      LOG.error('Worker message error', error);
    this.worker.onmessage = (event) => {
      try {
        const message = CompressorResponse.parse(event.data);
        switch (message.response) {
          case 'compressed':
            this.fragment = toFragment(message.version, message.compressedText);
            this.compressionEnded();
            window.history.replaceState(null, '', this.fragment);
            break;
          case 'decompressed':
            this.processDecompressed(message.version, message.text);
            break;
          case 'error':
            this.compressionEnded();
            LOG.error('Error processing compressor request', message.message);
            break;
          default:
            LOG.error('Unknown response from compressor worker', event.data);
            break;
        }
      } catch (error) {
        LOG.error('Error processing worker message', event, error);
      }
    };
    window.addEventListener('hashchange', this.hashChangeHandler);
  }

  decompressInitial(): void {
    this.updateHash();
    if (this.fragment === undefined) {
      LOG.debug('Loading default source');
      this.onDecompressed(initialValue);
    }
  }

  compress(text: string, visibility?: Record<string, Visibility>): void {
    if (visibility === undefined || Object.keys(visibility).length === 0) {
      this.doCompress(1, text);
      return;
    }
    const payload = {
      t: text,
      v: visibility,
    } satisfies V2Payload;
    this.doCompress(2, JSON.stringify(payload));
  }

  private doCompress(version: CompressorVersion, text: string): void {
    this.toCompress = text;
    this.nextVersion = version;
    if (this.compressing) {
      return;
    }
    this.compressing = true;
    this.worker.postMessage({
      request: 'compress',
      text,
      version,
    } satisfies CompressRequest);
  }

  private processDecompressed(version: CompressorVersion, text: string): void {
    if (version === 1) {
      this.onDecompressed(text);
      return;
    }
    let payload: V2Payload;
    try {
      payload = V2Payload.parse(JSON.parse(text));
    } catch (e) {
      LOG.error('Failed to parse URI fragment payload', e);
      return;
    }
    this.onDecompressed(payload.t, payload.v);
  }

  dispose(): void {
    window.removeEventListener('hashchange', this.hashChangeHandler);
    this.worker.terminate();
  }

  private compressionEnded(): void {
    this.compressing = false;
    if (this.toCompress !== undefined) {
      this.doCompress(this.nextVersion, this.toCompress);
      this.toCompress = undefined;
    }
  }

  private updateHash(): void {
    if (window.location.hash === this.fragment) {
      return;
    }
    const result = fromFragment(window.location.hash);
    if (result === undefined) {
      return;
    }
    this.fragment = window.location.hash;
    this.worker.postMessage({
      request: 'decompress',
      compressedText: result.text,
      version: result.version,
    } satisfies DecompressRequest);
  }
}
