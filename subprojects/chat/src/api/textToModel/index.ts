/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import {
  type ConcretizationSuccessResult,
  type Issue,
  RefineryError,
} from '@tools.refinery/client';
import {
  type TextToModelResult,
  TextToModelRequest,
  type TextToModelStatus,
} from '@tools.refinery/client/chat';
import type { RequestHandler } from 'express';
import type { OpenAI } from 'openai';
import { zodResponseFormat } from 'openai/helpers/zod';
import { z } from 'zod/v3';

import system from './system.md';

// We deliberately use zod v3 here instead of v4, because this is what OpenAI uses.

const model = process.env['OPENAI_MODEL'] ?? 'google/gemini-2.5-flash';

const ChatResponse = z.object({
  explanation: z.string(),
  assertions: z.string(),
});

const chatResponseFormat = zodResponseFormat(ChatResponse, 'chat_response');

function convertIssues(
  xtextIssues: Issue[],
  prefixLines: number,
  filter: (issue: Issue) => boolean,
): string[] {
  const issues: string[] = [];
  for (const issue of xtextIssues) {
    if (!filter(issue)) {
      continue;
    }
    if (issue.line < prefixLines) {
      // The AI generated code may cause error markers in the metamodel when certain constraints are violated,
      // e.g., when a scope constraint in the metamodel is violated. In such cases, we should not prefix the issue line number.
      issues.push(`* ${issue.description}`);
    } else {
      issues.push(
        `* Line ${issue.line - prefixLines + 1}: ${issue.description}`,
      );
    }
  }
  return issues;
}

function invalidProblemToChatMessage(
  err: RefineryError.InvalidProblem,
  prefixLines: number,
): string {
  return `Refinery has returned the following syntax errors:

${convertIssues(err.issues, prefixLines, ({ severity }) => severity === 'error').join('\n')}

Please check your assertions and fix the errors.`;
}

function concretizationResultToChatMessage(
  result: ConcretizationSuccessResult,
): string | undefined {
  if (result.json === undefined) {
    throw new Error('Concretization result does not contain JSON');
  }
  const { json } = result;
  const issues: string[] = convertIssues(result.issues, 0, () => true);

  for (const relationMetadata of json.relations) {
    const tuples = json.partialInterpretation[relationMetadata.name] ?? [];
    for (const tuple of tuples) {
      const value = tuple[tuple.length - 1];
      if (
        value !== 'error' &&
        !(typeof value === 'object' && 'error' in value)
      ) {
        continue;
      }
      const args = tuple
        .slice(0, -1)
        .map((id) => {
          if (typeof id !== 'number') {
            throw new Error('Invalid node ID');
          }
          const nodeMetadata = json.nodes[id];
          return nodeMetadata?.simpleName ?? String(id);
        })
        .join(', ');
      issues.push(`${relationMetadata.simpleName}(${args}): error.`);
    }
  }

  if (issues.length === 0) {
    // There are no errors, we have successfully generated the model.
    return undefined;
  }

  return `Refinery has returned the following semantics errors in the model:

<errors>
${issues.join('\n')}
</error>

Please check your assertions and fix the errors.`;
}

const textToModel: RequestHandler = async (req, res) => {
  const { metamodel, text, format } = TextToModelRequest.parse(req.body);

  const messages: OpenAI.Chat.Completions.ChatCompletionMessageParam[] = [
    {
      role: 'system',
      content: system,
    },
    {
      role: 'user',
      content: `## Task

<metamodel>
${metamodel.source}
</metamodel>

<specification>
${text}
</specification>
`,
    },
  ];

  for (let i = 0; i < 5; i += 1) {
    const openAIResult = await req.openai.chat.completions
      .stream(
        {
          model,
          messages,
          response_format: chatResponseFormat,
        },
        { signal: req.signal },
      )
      .finalChatCompletion();

    const assistantMessage = openAIResult.choices[0]?.message;
    if (assistantMessage === undefined) {
      throw new Error('AI returned no response');
    }
    messages.push(assistantMessage);
    const chatResponse = ChatResponse.parse(assistantMessage.parsed);

    await res.writeStatus({
      role: 'assistant',
      content: chatResponse.explanation,
    } satisfies TextToModelStatus);

    req.log.debug({ chatResponse }, 'Generated chat response');

    const prefix = `${metamodel.source}\n\n`;
    const prefixLines = prefix.split('\n').length;
    const modelSource = `${prefix}${chatResponse.assertions}`;

    let refineryResult: ConcretizationSuccessResult;
    try {
      refineryResult = await req.refinery.concretize(
        {
          input: { source: modelSource },
          format: {
            ...format,
            json: {
              ...(format.json ?? {}),
              enabled: true,
              nonExistingObjects: 'discard',
            },
          },
        },
        {
          signal: req.signal,
        },
      );
    } catch (err) {
      if (err instanceof RefineryError.InvalidProblem) {
        const errorMessage = invalidProblemToChatMessage(err, prefixLines);
        res.log.debug({ errorMessage }, 'Syntax errors');
        messages.push({
          role: 'user',
          content: errorMessage,
        });
        await res.writeStatus({
          role: 'refinery',
          content: 'AI response contains syntax errors',
        });
        continue;
      } else {
        throw err;
      }
    }

    const errorMessage = concretizationResultToChatMessage(refineryResult);
    if (errorMessage === undefined) {
      if (!(format?.json.enabled ?? true)) {
        // While internally we always generate JSON to find `error` values in the concretized model,
        // we only return it to the user when requested.
        delete refineryResult.json;
      }
      await res.writeSuccess({
        ...refineryResult,
      } satisfies TextToModelResult);
      break;
    }
    res.log.debug({ errorMessage }, 'Semantic errors');
    messages.push({
      role: 'user',
      content: errorMessage,
    });
    await res.writeStatus({
      role: 'refinery',
      content: 'AI response contains semantic errors',
    });
  }

  throw new RefineryError.Unsatisfiable({
    result: 'unsatisfiable',
    message: 'AI failed to generate a suitable response',
  });
};

export default textToModel;
