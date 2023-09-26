/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import getLogger from '../utils/getLogger';

import {
  type CompressRequest,
  CompressorResponse,
  type DecompressRequest,
} from './compressionMessages';
import CompressionWorker from './compressionWorker?worker';
import initialValue from './initialValue';

const LOG = getLogger('persistence.Compressor');

const FRAGMENT_PREFIX = '#/1/';

export default class Compressor {
  private readonly worker = new CompressionWorker();

  private readonly hashChangeHandler = () => this.updateHash();

  private fragment: string | undefined;

  private compressing = false;

  private toCompress: string | undefined;

  constructor(private readonly onDecompressed: (text: string) => void) {
    this.worker.onerror = (error) => LOG.error('Worker error', error);
    this.worker.onmessageerror = (error) =>
      LOG.error('Worker message error', error);
    this.worker.onmessage = (event) => {
      try {
        const message = CompressorResponse.parse(event.data);
        switch (message.response) {
          case 'compressed':
            this.fragment = `${FRAGMENT_PREFIX}${message.compressedText}`;
            this.compressionEnded();
            window.history.replaceState(null, '', this.fragment);
            break;
          case 'decompressed':
            this.onDecompressed(message.text);
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

  compress(text: string): void {
    this.toCompress = text;
    if (this.compressing) {
      return;
    }
    this.compressing = true;
    this.worker.postMessage({
      request: 'compress',
      text,
    } satisfies CompressRequest);
  }

  dispose(): void {
    window.removeEventListener('hashchange', this.hashChangeHandler);
    this.worker.terminate();
  }

  private compressionEnded(): void {
    this.compressing = false;
    if (this.toCompress !== undefined) {
      this.compress(this.toCompress);
      this.toCompress = undefined;
    }
  }

  private updateHash(): void {
    if (
      window.location.hash !== this.fragment &&
      window.location.hash.startsWith(FRAGMENT_PREFIX)
    ) {
      this.fragment = window.location.hash;
      this.worker.postMessage({
        request: 'decompress',
        compressedText: this.fragment.substring(FRAGMENT_PREFIX.length),
      } satisfies DecompressRequest);
    }
  }
}
