import type {
  Completion,
  CompletionContext,
  CompletionResult,
} from '@codemirror/autocomplete';
import { syntaxTree } from '@codemirror/language';
import type { Transaction } from '@codemirror/state';
import escapeStringRegexp from 'escape-string-regexp';

import { implicitCompletion } from '../language/props';
import type { UpdateService } from './UpdateService';
import { getLogger } from '../utils/logger';
import type { ContentAssistEntry } from './xtextServiceResults';

const PROPOSALS_LIMIT = 1000;

const IDENTIFIER_REGEXP_STR = '[a-zA-Z0-9_]*';

const HIGH_PRIORITY_KEYWORDS = ['<->', '==>'];

const log = getLogger('xtext.ContentAssistService');

interface IFoundToken {
  from: number;

  to: number;

  implicitCompletion: boolean;

  text: string;
}

function findToken({ pos, state }: CompletionContext): IFoundToken | null {
  const token = syntaxTree(state).resolveInner(pos, -1);
  if (token === null) {
    return null;
  }
  if (token.firstChild !== null) {
    // We only autocomplete terminal nodes. If the current node is nonterminal,
    // returning `null` makes us autocomplete with the empty prefix instead.
    return null;
  }
  return {
    from: token.from,
    to: token.to,
    implicitCompletion: token.type.prop(implicitCompletion) || false,
    text: state.sliceDoc(token.from, token.to),
  };
}

function shouldCompleteImplicitly(token: IFoundToken | null, context: CompletionContext): boolean {
  return token !== null
    && token.implicitCompletion
    && context.pos - token.from >= 2;
}

function computeSpan(prefix: string, entryCount: number): RegExp {
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

function createCompletion(entry: ContentAssistEntry): Completion {
  let boost: number;
  switch (entry.kind) {
    case 'KEYWORD':
      // Some hard-to-type operators should be on top.
      boost = HIGH_PRIORITY_KEYWORDS.includes(entry.proposal) ? 10 : -99;
      break;
    case 'TEXT':
    case 'SNIPPET':
      boost = -90;
      break;
    default: {
      // Penalize qualified names (vs available unqualified names).
      const extraSegments = entry.proposal.match(/::/g)?.length || 0;
      boost = Math.max(-5 * extraSegments, -50);
    }
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

export class ContentAssistService {
  private readonly updateService: UpdateService;

  private lastCompletion: CompletionResult | null = null;

  constructor(updateService: UpdateService) {
    this.updateService = updateService;
  }

  onTransaction(transaction: Transaction): void {
    if (this.shouldInvalidateCachedCompletion(transaction)) {
      this.lastCompletion = null;
    }
  }

  async contentAssist(context: CompletionContext): Promise<CompletionResult> {
    const tokenBefore = findToken(context);
    if (!context.explicit && !shouldCompleteImplicitly(tokenBefore, context)) {
      return {
        from: context.pos,
        options: [],
      };
    }
    let range: { from: number, to: number };
    let prefix = '';
    if (tokenBefore === null) {
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
      if (prefix === entry.prefix) {
        // Xtext will generate completions that do not complete the current token,
        // e.g., `(` after trying to complete an indetifier,
        // but we ignore those, since CodeMirror won't filter for them anyways.
        options.push(createCompletion(entry));
      }
    });
    log.debug('Fetched', options.length, 'completions from server');
    this.lastCompletion = {
      ...range,
      options,
      validFor: computeSpan(prefix, entries.length),
    };
    return this.lastCompletion;
  }

  private shouldReturnCachedCompletion(
    token: { from: number, to: number, text: string } | null,
  ): boolean {
    if (token === null || this.lastCompletion === null) {
      return false;
    }
    const { from, to, text } = token;
    const { from: lastFrom, to: lastTo, validFor } = this.lastCompletion;
    if (!lastTo) {
      return true;
    }
    const [transformedFrom, transformedTo] = this.mapRangeInclusive(lastFrom, lastTo);
    return from >= transformedFrom
      && to <= transformedTo
      && validFor instanceof RegExp
      && validFor.exec(text) !== null;
  }

  private shouldInvalidateCachedCompletion(transaction: Transaction): boolean {
    if (!transaction.docChanged || this.lastCompletion === null) {
      return false;
    }
    const { from: lastFrom, to: lastTo } = this.lastCompletion;
    if (!lastTo) {
      return true;
    }
    const [transformedFrom, transformedTo] = this.mapRangeInclusive(lastFrom, lastTo);
    let invalidate = false;
    transaction.changes.iterChangedRanges((fromA, toA) => {
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
