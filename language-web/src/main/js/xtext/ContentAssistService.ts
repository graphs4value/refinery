import type {
  Completion,
  CompletionContext,
  CompletionResult,
} from '@codemirror/autocomplete';
import type { ChangeSet, Transaction } from '@codemirror/state';
import escapeStringRegexp from 'escape-string-regexp';

import { getLogger } from '../logging';
import type { UpdateService } from './UpdateService';
import type { IContentAssistEntry } from './xtextServiceResults';

const PROPOSALS_LIMIT = 1000;

const IDENTIFIER_REGEXP_STR = '[a-zA-Z0-9_]*';

const log = getLogger('xtext.ContentAssistService');

function createCompletion(entry: IContentAssistEntry): Completion {
  let boost;
  switch (entry.kind) {
    case 'KEYWORD':
      boost = -99;
      break;
    case 'TEXT':
    case 'SNIPPET':
      boost = -90;
      break;
    default:
      boost = 0;
      break;
  }
  return {
    label: entry.proposal,
    detail: entry.description,
    info: entry.documentation,
    type: entry.kind?.toLowerCase(),
    boost,
  };
}

function computeSpan(prefix: string, entryCount: number) {
  const escapedPrefix = escapeStringRegexp(prefix);
  if (entryCount < PROPOSALS_LIMIT) {
    // Proposals with the current prefix fit the proposals limit.
    // We can filter client side as long as the current prefix is preserved.
    return new RegExp(`^${escapedPrefix}${IDENTIFIER_REGEXP_STR}$`);
  }
  // The current prefix overflows the proposals limits,
  // so we have to fetch the completions again on the next keypress.
  // Hopefully, it'll return a shorter list and we'll be able to filter client side.
  return new RegExp(`^${escapedPrefix}$`);
}

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
    let prefix = '';
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
      prefix = '';
    } else {
      range = {
        from: tokenBefore.from,
        to: tokenBefore.to,
      };
      const prefixLength = context.pos - tokenBefore.from;
      if (prefixLength > 0) {
        prefix = tokenBefore.text.substring(0, context.pos - tokenBefore.from);
      }
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
      proposalsLimit: PROPOSALS_LIMIT,
    }, context);
    if (context.aborted) {
      return {
        ...range,
        options: [],
      };
    }
    const options: Completion[] = [];
    entries.forEach((entry) => {
      options.push(createCompletion(entry));
    });
    log.debug('Fetched', options.length, 'completions from server');
    this.lastCompletion = {
      ...range,
      options,
      span: computeSpan(prefix, entries.length),
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
