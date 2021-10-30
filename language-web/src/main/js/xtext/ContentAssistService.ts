import type {
  Completion,
  CompletionContext,
  CompletionResult,
} from '@codemirror/autocomplete';
import type { ChangeSet, Transaction } from '@codemirror/state';

import { getLogger } from '../logging';
import type { UpdateService } from './UpdateService';

const log = getLogger('xtext.ContentAssistService');

export class ContentAssistService {
  updateService: UpdateService;

  lastCompletion: CompletionResult | null = null;

  constructor(updateService: UpdateService) {
    this.updateService = updateService;
  }

  onTransaction(transaction: Transaction): void {
    if (this.shouldInvalidateCachedCompletion(transaction.changes)) {
      this.lastCompletion = null;
    }
  }

  async contentAssist(context: CompletionContext): Promise<CompletionResult> {
    const tokenBefore = context.tokenBefore(['QualifiedName']);
    let range: { from: number, to: number };
    let selection: { selectionStart?: number, selectionEnd?: number };
    if (tokenBefore === null) {
      if (!context.explicit) {
        return {
          from: context.pos,
          options: [],
        };
      }
      range = {
        from: context.pos,
        to: context.pos,
      };
      selection = {};
    } else {
      range = {
        from: tokenBefore.from,
        to: tokenBefore.to,
      };
      selection = {
        selectionStart: tokenBefore.from,
        selectionEnd: tokenBefore.to,
      };
    }
    if (!context.explicit && this.shouldReturnCachedCompletion(tokenBefore)) {
      log.trace('Returning cached completion result');
      // Postcondition of `shouldReturnCachedCompletion`: `lastCompletion !== null`
      return {
        ...this.lastCompletion as CompletionResult,
        ...range,
      };
    }
    this.lastCompletion = null;
    const entries = await this.updateService.fetchContentAssist({
      resource: this.updateService.resourceName,
      serviceType: 'assist',
      caretOffset: context.pos,
      ...selection,
    }, context);
    if (context.aborted) {
      return {
        ...range,
        options: [],
      };
    }
    const options: Completion[] = [];
    entries.forEach((entry) => {
      options.push({
        label: entry.proposal,
        detail: entry.description,
        info: entry.documentation,
        type: entry.kind?.toLowerCase(),
        boost: entry.kind === 'KEYWORD' ? -90 : 0,
      });
    });
    log.debug('Fetched', options.length, 'completions from server');
    this.lastCompletion = {
      ...range,
      options,
      span: /^[a-zA-Z0-9_:]*$/,
    };
    return this.lastCompletion;
  }

  private shouldReturnCachedCompletion(
    token: { from: number, to: number, text: string } | null,
  ) {
    if (token === null || this.lastCompletion === null) {
      return false;
    }
    const { from, to, text } = token;
    const { from: lastFrom, to: lastTo, span } = this.lastCompletion;
    if (!lastTo) {
      return true;
    }
    const [transformedFrom, transformedTo] = this.mapRangeInclusive(lastFrom, lastTo);
    return from >= transformedFrom && to <= transformedTo && span && span.exec(text);
  }

  private shouldInvalidateCachedCompletion(changes: ChangeSet) {
    if (changes.empty || this.lastCompletion === null) {
      return false;
    }
    const { from: lastFrom, to: lastTo } = this.lastCompletion;
    if (!lastTo) {
      return true;
    }
    const [transformedFrom, transformedTo] = this.mapRangeInclusive(lastFrom, lastTo);
    let invalidate = false;
    changes.iterChangedRanges((fromA, toA) => {
      if (fromA < transformedFrom || toA > transformedTo) {
        invalidate = true;
      }
    });
    return invalidate;
  }

  private mapRangeInclusive(lastFrom: number, lastTo: number): [number, number] {
    const changes = this.updateService.computeChangesSinceLastUpdate();
    const transformedFrom = changes.mapPos(lastFrom);
    const transformedTo = changes.mapPos(lastTo, 1);
    return [transformedFrom, transformedTo];
  }
}
