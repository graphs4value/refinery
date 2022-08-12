import { Transaction } from '@codemirror/state';

import type EditorStore from '../editor/EditorStore';
import type { IOccurrence } from '../editor/findOccurrences';
import Timer from '../utils/Timer';
import getLogger from '../utils/getLogger';

import type UpdateService from './UpdateService';
import type XtextWebSocketClient from './XtextWebSocketClient';
import {
  isConflictResult,
  OccurrencesResult,
  type TextRegion,
} from './xtextServiceResults';

const FIND_OCCURRENCES_TIMEOUT_MS = 1000;

// Must clear occurrences asynchronously from `onTransaction`,
// because we must not emit a conflicting transaction when handling the pending transaction.
const CLEAR_OCCURRENCES_TIMEOUT_MS = 10;

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
  private readonly store: EditorStore;

  private readonly webSocketClient: XtextWebSocketClient;

  private readonly updateService: UpdateService;

  private hasOccurrences = false;

  private readonly findOccurrencesTimer = new Timer(() => {
    this.handleFindOccurrences();
  }, FIND_OCCURRENCES_TIMEOUT_MS);

  private readonly clearOccurrencesTimer = new Timer(() => {
    this.clearOccurrences();
  }, CLEAR_OCCURRENCES_TIMEOUT_MS);

  constructor(
    store: EditorStore,
    webSocketClient: XtextWebSocketClient,
    updateService: UpdateService,
  ) {
    this.store = store;
    this.webSocketClient = webSocketClient;
    this.updateService = updateService;
  }

  onTransaction(transaction: Transaction): void {
    if (transaction.docChanged) {
      this.clearOccurrencesTimer.schedule();
      this.findOccurrencesTimer.reschedule();
    }
    if (transaction.isUserEvent('select')) {
      this.findOccurrencesTimer.reschedule();
    }
  }

  private handleFindOccurrences() {
    this.clearOccurrencesTimer.cancel();
    this.updateOccurrences().catch((error) => {
      log.error('Unexpected error while updating occurrences', error);
      this.clearOccurrences();
    });
  }

  private async updateOccurrences() {
    await this.updateService.update();
    const result = await this.webSocketClient.send({
      resource: this.updateService.resourceName,
      serviceType: 'occurrences',
      expectedStateId: this.updateService.xtextStateId,
      caretOffset: this.store.state.selection.main.head,
    });
    const allChanges = this.updateService.computeChangesSinceLastUpdate();
    if (!allChanges.empty || isConflictResult(result, 'canceled')) {
      // Stale occurrences result, the user already made some changes.
      // We can safely ignore the occurrences and schedule a new find occurrences call.
      this.clearOccurrences();
      this.findOccurrencesTimer.schedule();
      return;
    }
    const parsedOccurrencesResult = OccurrencesResult.safeParse(result);
    if (!parsedOccurrencesResult.success) {
      log.error(
        'Unexpected occurences result',
        result,
        'not an OccurrencesResult: ',
        parsedOccurrencesResult.error,
      );
      this.clearOccurrences();
      return;
    }
    const { stateId, writeRegions, readRegions } = parsedOccurrencesResult.data;
    if (stateId !== this.updateService.xtextStateId) {
      log.error(
        'Unexpected state id, expected:',
        this.updateService.xtextStateId,
        'got:',
        stateId,
      );
      this.clearOccurrences();
      return;
    }
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

  private clearOccurrences() {
    if (this.hasOccurrences) {
      this.store.updateOccurrences([], []);
      this.hasOccurrences = false;
    }
  }
}
