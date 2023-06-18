/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import type { Transaction } from '@codemirror/state';
import { debounce } from 'lodash-es';
import ms from 'ms';

import type EditorStore from '../editor/EditorStore';
import {
  type IOccurrence,
  isCursorWithinOccurence,
} from '../editor/findOccurrences';
import getLogger from '../utils/getLogger';

import type UpdateService from './UpdateService';
import type { TextRegion } from './xtextServiceResults';

const FIND_OCCURRENCES_TIMEOUT = ms('1s');

const log = getLogger('xtext.OccurrencesService');

function transformOccurrences(regions: TextRegion[]): IOccurrence[] {
  const occurrences: IOccurrence[] = [];
  regions.forEach(({ offset, length }) => {
    if (length > 0) {
      occurrences.push({
        from: offset,
        to: offset + length,
      });
    }
  });
  return occurrences;
}

export default class OccurrencesService {
  private hasOccurrences = false;

  private readonly findOccurrencesLater = debounce(
    () => this.findOccurrences(),
    FIND_OCCURRENCES_TIMEOUT,
  );

  constructor(
    private readonly store: EditorStore,
    private readonly updateService: UpdateService,
  ) {}

  onReconnect(): void {
    this.clearOccurrences();
    this.findOccurrencesLater();
  }

  onDisconnect(): void {
    this.clearOccurrences();
  }

  onTransaction(transaction: Transaction): void {
    if (transaction.docChanged) {
      // Must clear occurrences asynchronously from `onTransaction`,
      // because we must not emit a conflicting transaction when handling the pending transaction.
      this.clearAndFindOccurrencesLater();
      return;
    }
    if (!transaction.isUserEvent('select')) {
      return;
    }
    if (this.needsOccurrences) {
      if (!isCursorWithinOccurence(this.store.state)) {
        this.clearAndFindOccurrencesLater();
      }
    } else {
      this.clearOccurrencesLater();
    }
  }

  private get needsOccurrences(): boolean {
    return this.store.state.selection.main.empty;
  }

  private clearAndFindOccurrencesLater(): void {
    this.clearOccurrencesLater();
    this.findOccurrencesLater();
  }

  /**
   * Clears the occurences from a new immediate task to let the current editor transaction finish.
   */
  private clearOccurrencesLater() {
    setTimeout(() => this.clearOccurrences(), 0);
  }

  private clearOccurrences() {
    if (this.hasOccurrences) {
      this.store.updateOccurrences([], []);
      this.hasOccurrences = false;
    }
  }

  private findOccurrences() {
    this.updateOccurrences().catch((error) => {
      log.error('Unexpected error while updating occurrences', error);
      this.clearOccurrences();
    });
  }

  private async updateOccurrences() {
    if (!this.needsOccurrences || !this.updateService.opened) {
      this.clearOccurrences();
      return;
    }
    const fetchResult = await this.updateService.fetchOccurrences(() => {
      return this.needsOccurrences
        ? {
            cancelled: false,
            data: this.store.state.selection.main.head,
          }
        : { cancelled: true };
    });
    if (fetchResult.cancelled) {
      // Stale occurrences result, the user already made some changes.
      // We can safely ignore the occurrences and schedule a new find occurrences call.
      this.clearOccurrences();
      if (this.needsOccurrences) {
        this.findOccurrencesLater();
      }
      return;
    }
    const {
      data: { writeRegions, readRegions },
    } = fetchResult;
    const write = transformOccurrences(writeRegions);
    const read = transformOccurrences(readRegions);
    this.hasOccurrences = write.length > 0 || read.length > 0;
    log.debug(
      'Found',
      write.length,
      'write and',
      read.length,
      'read occurrences',
    );
    this.store.updateOccurrences(write, read);
  }
}
