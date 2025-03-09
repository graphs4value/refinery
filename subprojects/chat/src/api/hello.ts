/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import type { RequestHandler } from 'express';

import example from './example';

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
      model: 'google/gemini-2.0-flash-001',
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
* The key ${'`"rename"`'} is a JSON object, where each key is a name on a node in the instance model, and the value is a new name for that node that is more relevant and plausible in the ${'`"domain"`'} you selected.
* The key ${'`"description"`'} is a string, which is a brief description of the purpose of the instance model and its potential use in the ${'`"domain"`'}.
  Make sure you use all the domain-specific node names from the ${'`"rename"`'} map in your explanation.
`,
        },
      ],
    },
    { signal: req.signal },
  );

  const final = await stream.finalChatCompletion();
  await res.writeSuccess(final.choices[0]?.message.content);
};

export default hello;
