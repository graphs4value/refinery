/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import type {
  Completion,
  CompletionContext,
  CompletionResult,
} from '@codemirror/autocomplete';
import { syntaxTree } from '@codemirror/language';
import type { Transaction } from '@codemirror/state';
import escapeStringRegexp from 'escape-string-regexp';

import { implicitCompletion } from '../language/props';
import getLogger from '../utils/getLogger';

import type UpdateService from './UpdateService';
import type { ContentAssistEntry } from './xtextServiceResults';

const PROPOSALS_LIMIT = 1000;

const IDENTIFIER_REGEXP_STR = '[a-zA-Z0-9_]*';

const HIGH_PRIORITY_KEYWORDS = ['<->', '->', '==>'];

const log = getLogger('xtext.ContentAssistService');

interface IFoundToken {
  from: number;

  to: number;

  implicitCompletion: boolean;

  text: string;
}

function findToken({ pos, state }: CompletionContext): IFoundToken | undefined {
  const token = syntaxTree(state).resolveInner(pos, -1);
  const { from } = token;
  if (from > pos) {
    // We haven't found the token we want to complete.
    // Complete with an empty prefix from `pos` instead.
    // The other `return undefined;` lines also handle this condition.
    return undefined;
  }
  // We look at the text at the beginning of the token.
  // For QualifiedName tokens right before a comment, this may be a comment token.
  const endIndex = token.firstChild?.from ?? token.to;
  if (pos > endIndex) {
    return undefined;
  }
  const text = state.sliceDoc(from, endIndex).trimEnd();
  // Due to parser error recovery, we may get spurious whitespace
  // at the end of the token.
  const to = from + text.length;
  if (to > endIndex) {
    return undefined;
  }
  if (from > pos || endIndex < pos) {
    // We haven't found the token we want to complete.
    // Complete with an empty prefix from `pos` instead.
    return undefined;
  }
  return {
    from,
    to,
    implicitCompletion: token.type.prop(implicitCompletion) || false,
    text,
  };
}

function shouldCompleteImplicitly(
  token: IFoundToken | undefined,
  context: CompletionContext,
): boolean {
  return (
    token !== undefined &&
    token.implicitCompletion &&
    context.pos - token.from >= 2
  );
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
    default:
      if (entry.proposal.startsWith('::')) {
        // Move absolute names below relative names,
        // they should only be preferred if no relative name is available.
        boost = -60;
      } else {
        // Penalize qualified names (vs available unqualified names).
        const extraSegments = entry.proposal.match(/::/g)?.length || 0;
        boost = Math.max(-5 * extraSegments, -50);
      }
      break;
  }
  const completion: Completion = {
    label: entry.proposal,
    type: entry.kind?.toLowerCase(),
    boost,
  };
  if (entry.documentation !== undefined) {
    completion.info = entry.documentation;
  }
  if (entry.description !== undefined) {
    completion.detail = entry.description;
  }
  return completion;
}

export default class ContentAssistService {
  private lastCompletion: CompletionResult | undefined;

  constructor(private readonly updateService: UpdateService) {}

  onTransaction(transaction: Transaction): void {
    if (this.shouldInvalidateCachedCompletion(transaction)) {
      this.lastCompletion = undefined;
    }
  }

  async contentAssist(context: CompletionContext): Promise<CompletionResult> {
    if (!this.updateService.opened) {
      this.lastCompletion = undefined;
      return {
        from: context.pos,
        options: [],
      };
    }
    const tokenBefore = findToken(context);
    if (!context.explicit && !shouldCompleteImplicitly(tokenBefore, context)) {
      return {
        from: context.pos,
        options: [],
      };
    }
    let range: { from: number; to: number };
    let prefix = '';
    if (tokenBefore === undefined) {
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
      if (this.lastCompletion === undefined) {
        throw new Error(
          'There is no cached completion, but we want to return it',
        );
      }
      log.trace('Returning cached completion result');
      return {
        ...this.lastCompletion,
        ...range,
      };
    }
    this.lastCompletion = undefined;
    const entries = await this.updateService.fetchContentAssist(
      {
        caretOffset: context.pos,
        proposalsLimit: PROPOSALS_LIMIT,
      },
      context,
    );
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
    token: { from: number; to: number; text: string } | undefined,
  ): boolean {
    if (token === undefined || this.lastCompletion === undefined) {
      return false;
    }
    const { from, to, text } = token;
    const { from: lastFrom, to: lastTo, validFor } = this.lastCompletion;
    if (!lastTo) {
      return true;
    }
    const [transformedFrom, transformedTo] = this.mapRangeInclusive(
      lastFrom,
      lastTo,
    );
    return (
      from >= transformedFrom &&
      to <= transformedTo &&
      validFor instanceof RegExp &&
      validFor.exec(text) !== null
    );
  }

  private shouldInvalidateCachedCompletion(transaction: Transaction): boolean {
    if (!transaction.docChanged || this.lastCompletion === undefined) {
      return false;
    }
    const { from: lastFrom, to: lastTo } = this.lastCompletion;
    if (lastTo === undefined) {
      return true;
    }
    let transformedFrom: number;
    let transformedTo: number;
    try {
      [transformedFrom, transformedTo] = this.mapRangeInclusive(
        lastFrom,
        lastTo,
      );
    } catch (error) {
      if (error instanceof RangeError) {
        log.debug('Invalidating cache due to invalid range', error);
        return true;
      }
      throw error;
    }
    let invalidate = false;
    transaction.changes.iterChangedRanges((fromA, toA) => {
      if (fromA < transformedFrom || toA > transformedTo) {
        invalidate = true;
      }
    });
    return invalidate;
  }

  private mapRangeInclusive(
    lastFrom: number,
    lastTo: number,
  ): [number, number] {
    const changes = this.updateService.computeChangesSinceLastUpdate();
    const transformedFrom = changes.mapPos(lastFrom);
    const transformedTo = changes.mapPos(lastTo, 1);
    return [transformedFrom, transformedTo];
  }
}
