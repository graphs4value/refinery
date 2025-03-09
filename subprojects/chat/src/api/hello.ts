/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import type { RequestHandler } from 'express';
import { zodResponseFormat } from 'openai/helpers/zod';
import { z } from 'zod';

import example from './example';

const RenameResult = z.object({
  domain: z.string(),
  rename: z
    .object({
      oldName: z.string(),
      newName: z.string(),
    })
    .array(),
  description: z.string(),
});

const hello: RequestHandler = async (req, res, next) => {
  const { seed } = req.body as { seed: number };

  const { source } = await req.refinery.generate(
    {
      input: { source: example },
      outputFormats: {
        json: { enabled: false },
        source: { enabled: true },
      },
      randomSeed: seed,
    },
    {
      onStatus: (status) => res.writeStatus(status).catch(next),
      signal: req.signal,
    },
  );

  await res.writeStatus({ message: 'Generating explanation' });

  const stream = req.openai.beta.chat.completions.stream(
    {
      // model: 'google/gemini-2.0-flash-001',
      model: 'openai/gpt-4o-mini',
      messages: [
        {
          role: 'user',
          content: `This is a metamodel and instance model expressed in the Refinery language. Refinery uses an Xcore-like language for metamodels, and a Datalog-like language for constraints and instance models.
${'```'}
${source}
${'```'}

The instance model was generated using a logical procedure that ensures consistency with the metamodel and constraints.

Answer with a JSON object with the following structure:

* The key ${'`"domain"`'} is string, which is a name of a domain where an instance model such as the one provided could be useful.
* The key ${'`"rename"`'} is an array of object, where each object has an ${'`"oldName"`'} string, which is the name of a node in the instance model that is being renamed, and a ${'`"newName"`'} string, which is the new name for the node.
  Make sure that you rename all nodes in the instace model exactly once, and that the new names are realistic and plausible in the context of the ${'`"domain"`'}.
* The key ${'`"description"`'} is a string, which is a brief description of the purpose of the instance model and its potential use in the ${'`"domain"`'}.
  Make sure you use all the domain-specific ${'`"newName"`'}node names from the ${'`"rename"`'} map in your explanation, and sorround then with square brackets ${'`[like so]`'} whenever they appear.
  Do not use square brackets for any other purpose.

Start by coming up with an appropriate ${'`"domain"`'}.
Then determine the ${'`"rename"`'} array by renaming nodes in the instance model to names that are plausible and meaningful in the ${'`"domain"`'}.
Finally, write a ${'`"description"`'} that explains how the instance model works in the ${'`"domain"`'}.
`,
        },
      ],
      response_format: zodResponseFormat(RenameResult, 'rename_result'),
    },
    { signal: req.signal },
  );

  const final = await stream.finalChatCompletion();
  await res.writeSuccess(final.choices[0]?.message.parsed);
};

export default hello;
