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

  editorStore: EditorStore | undefined;

  private abortController: AbortController | undefined;

  private client: RefineryChat | undefined;

  constructor() {
    makeAutoObservable<ChatStore, 'abortController' | 'client'>(this, {
      abortController: false,
      client: false,
    });
  }

  setEditorStore(editorStore: EditorStore | undefined) {
    this.editorStore = editorStore;
    const baseURL = editorStore?.backendConfig.chatURL;
    if (baseURL === undefined) {
      this.client = undefined;
      return;
    }
    this.client = new RefineryChat({ baseURL });
  }

  setMessage(value: string) {
    this.message = value;
  }

  get canGenerate(): boolean {
    return (
      this.editorStore !== undefined &&
      this.client !== undefined &&
      !this.running &&
      this.message !== '' &&
      this.editorStore.errorCount === 0
    );
  }

  generate() {
    if (this.editorStore === undefined) {
      return;
    }
    this.pushLog({
      role: 'user',
      content: this.message,
    });
    this.running = true;
    this.abortController = new AbortController();
    const signal = this.abortController.signal;
    (async () => {
      if (this.editorStore === undefined || this.client === undefined) {
        return;
      }
      const result = await this.client.textToModel(
        {
          metamodel: { source: this.editorStore.state.sliceDoc() },
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
      this.editorStore.addGeneratedModel(uuid, 0);
      if (result.json !== undefined) {
        this.editorStore.setGeneratedModelSemantics(
          uuid,
          result.json,
          result.source,
        );
      } else {
        this.editorStore.setGeneratedModelError(uuid, 'No JSON in AI response');
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
