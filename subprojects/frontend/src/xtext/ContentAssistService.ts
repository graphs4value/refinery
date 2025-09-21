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
import type { Transaction } from '@codemirror/state';
import escapeStringRegexp from 'escape-string-regexp';

import getLogger from '../utils/getLogger';

import type UpdateService from './UpdateService';
import findToken, { type FoundToken } from './findToken';
import type { ContentAssistEntry } from './xtextServiceResults';

const PROPOSALS_LIMIT = 1000;

const IDENTIFIER_REGEXP_STR = '[a-zA-Z0-9_]*';

const HIGH_PRIORITY_KEYWORDS = ['<->', '->', '==>'];

const log = getLogger('xtext.ContentAssistService');

function shouldCompleteImplicitly(
  token: FoundToken | undefined,
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

function createCompletion(
  prefix: string,
  entry: ContentAssistEntry,
): Completion | undefined {
  if (!prefix.endsWith(entry.prefix)) {
    // Since CodeMirror will fuzzy match all entries, we only work with completions that match
    // some suffix of the current prefix according to Xtext. The remaining part of the prefix
    // will be added to each completion using `remainingPrefix` to make the fuzzy match successful
    // and avoid replacing the current prefix in the editor.
    return undefined;
  }
  let boost: number;
  let type: string | undefined;
  switch (entry.kind) {
    case 'KEYWORD':
      // Some hard-to-type operators should be on top.
      boost = HIGH_PRIORITY_KEYWORDS.includes(entry.proposal) ? 10 : -99;
      type = /^[a-z]+$/.test(entry.proposal) ? 'keyword' : 'operator';
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
        const extraSegments = entry.proposal.match(/::/g)?.length ?? 0;
        boost = Math.max(-5 * extraSegments, -50);
      }
      break;
  }
  // The server thinks this part of the prefix is not needed, but we need to add it back to satisfy CodeMirror.
  const remainingPrefix = prefix.slice(0, prefix.length - entry.prefix.length);
  const completion: Completion = {
    label: remainingPrefix + entry.proposal,
    displayLabel: entry.proposal,
    type: type ?? entry.kind,
    boost,
  };
  if (entry.documentation !== undefined) {
    const { documentation } = entry;
    completion.info = async () => {
      const { default: transformDocumentation } = await import(
        './transformDocumentation'
      );
      return transformDocumentation(documentation);
    };
  }
  if (entry.description !== undefined) {
    const { description } = entry;
    completion.detail = description.startsWith('/')
      ? description
      : `\u00a0${description}`;
  }
  return completion;
}

function getMatch(
  completion: Completion,
  matched?: readonly number[],
): readonly number[] {
  if (matched === undefined || matched.length < 2) {
    return [];
  }
  if (completion.displayLabel === undefined) {
    return matched;
  }
  const adjustment = completion.label.length - completion.displayLabel.length;
  if (adjustment <= 0) {
    return matched;
  }
  const adjusted: number[] = [];
  for (let i = 0; i < matched.length; i += 2) {
    const start = Math.max(0, matched[i]! - adjustment);
    const end = matched[i + 1]! - adjustment;
    if (end >= 1) {
      adjusted.push(start, end);
    }
  }
  return adjusted;
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
    const tokenBefore = findToken(context.pos, context.state);
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
      const completion = createCompletion(prefix, entry);
      if (completion !== undefined) {
        options.push(completion);
      }
    });
    log.debug('Fetched %d completions from server', options.length);
    this.lastCompletion = {
      ...range,
      options,
      validFor: computeSpan(prefix, entries.length),
      getMatch,
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
    } catch (err) {
      if (err instanceof RangeError) {
        log.debug({ err }, 'Invalidating cache due to invalid range');
        return true;
      }
      throw err;
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
