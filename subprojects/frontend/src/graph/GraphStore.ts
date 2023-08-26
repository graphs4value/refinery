/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { makeAutoObservable, observable } from 'mobx';

import type { SemanticsSuccessResult } from '../xtext/xtextServiceResults';

export type Visibility = 'all' | 'must' | 'none';

export default class GraphStore {
  semantics: SemanticsSuccessResult = {
    nodes: [],
    relations: [],
    partialInterpretation: {},
  };

  visibility = new Map<string, Visibility>();

  constructor() {
    makeAutoObservable(this, {
      semantics: observable.ref,
    });
  }

  getVisiblity(relation: string): Visibility {
    return this.visibility.get(relation) ?? 'none';
  }

  setSemantics(semantics: SemanticsSuccessResult) {
    this.semantics = semantics;
    this.visibility.clear();
    const names = new Set<string>();
    this.semantics.relations.forEach(({ name, detail }) => {
      names.add(name);
      if (!this.visibility.has(name)) {
        const newVisibility = detail.type === 'builtin' ? 'none' : 'all';
        this.visibility.set(name, newVisibility);
      }
    });
    const oldNames = new Set<string>();
    this.visibility.forEach((_, key) => oldNames.add(key));
    oldNames.forEach((key) => {
      if (!names.has(key)) {
        this.visibility.delete(key);
      }
    });
  }
}
