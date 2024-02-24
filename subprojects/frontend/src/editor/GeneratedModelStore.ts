/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { makeAutoObservable } from 'mobx';

import GraphStore from '../graph/GraphStore';
import type { SemanticsSuccessResult } from '../xtext/xtextServiceResults';

import type EditorStore from './EditorStore';

export default class GeneratedModelStore {
  title: string;

  message = 'Waiting for server';

  error = false;

  graph: GraphStore | undefined;

  constructor(
    private readonly randomSeed: number,
    private readonly editorStore: EditorStore,
  ) {
    const time = new Date().toLocaleTimeString(undefined, { hour12: false });
    this.title = `Generated at ${time} (${randomSeed})`;
    makeAutoObservable<GeneratedModelStore, 'editorStore'>(this, {
      editorStore: false,
    });
  }

  get running(): boolean {
    return !this.error && this.graph === undefined;
  }

  setMessage(message: string): void {
    if (this.running) {
      this.message = message;
    }
  }

  setError(message: string): void {
    if (this.running) {
      this.error = true;
      this.message = message;
    }
  }

  setSemantics(semantics: SemanticsSuccessResult): void {
    if (this.running) {
      const name = `${this.editorStore.simpleNameOrFallback}_solution_${this.randomSeed}`;
      this.graph = new GraphStore(this.editorStore, name);
      this.graph.setSemantics(semantics);
    }
  }
}
