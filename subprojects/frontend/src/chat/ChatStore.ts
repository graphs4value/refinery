/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { RefineryChat } from '@tools.refinery/client/chat';
import { makeAutoObservable, runInAction } from 'mobx';
import { nanoid } from 'nanoid';

import type EditorStore from '../editor/EditorStore';

export interface Message {
  id: string;

  role: 'user' | 'refinery' | 'assistant' | 'error';

  content: string;
}

export default class ChatStore {
  message = '';

  log: Message[] = [];

  running = false;

  private abortController: AbortController | undefined;

  private readonly client: RefineryChat;

  constructor() {
    this.client = new RefineryChat({
      baseURL: `${window.origin}/chat/v1`,
    });
    makeAutoObservable<ChatStore, 'abortController' | 'client'>(this, {
      abortController: false,
      client: false,
    });
  }

  setMessage(value: string) {
    this.message = value;
  }

  canGenerate(editorStore: EditorStore): boolean {
    return !this.running && this.message !== '' && editorStore.errorCount === 0;
  }

  generate(editorStore: EditorStore) {
    this.pushLog({
      role: 'user',
      content: this.message,
    });
    this.running = true;
    this.abortController = new AbortController();
    const signal = this.abortController.signal;
    (async () => {
      const result = await this.client.textToModel(
        {
          metamodel: { source: editorStore.state.sliceDoc() },
          text: this.message,
          format: {
            source: { enabled: true },
            json: {
              enabled: true,
              nonExistingObjects: 'discard',
              shadowPredicates: 'keep',
            },
          },
        },
        {
          onStatus: (status) => this.pushLog(status),
          signal,
        },
      );
      this.pushLog({
        role: 'refinery',
        content: 'Successfully generated model',
      });
      const uuid = nanoid();
      editorStore.addGeneratedModel(uuid, 0);
      if (result.json !== undefined) {
        editorStore.setGeneratedModelSemantics(
          uuid,
          result.json,
          result.source,
        );
      } else {
        editorStore.setGeneratedModelError(uuid, 'No JSON in AI response');
      }
    })()
      .catch((error) => {
        const message = error instanceof Error ? error.message : String(error);
        this.pushLog({
          role: 'error',
          content: message,
        });
      })
      .finally(() => {
        runInAction(() => (this.running = false));
      });
    this.message = '';
  }

  private pushLog(message: Omit<Message, 'id'>) {
    this.log.push({
      ...message,
      id: nanoid(),
    });
  }

  cancel() {
    this.abortController?.abort();
    this.abortController = undefined;
  }
}
