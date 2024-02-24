/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { makeAutoObservable, observable } from 'mobx';

import type EditorStore from '../editor/EditorStore';
import type {
  RelationMetadata,
  SemanticsSuccessResult,
} from '../xtext/xtextServiceResults';

export type Visibility = 'all' | 'must' | 'none';

export function getDefaultVisibility(
  metadata: RelationMetadata | undefined,
): Visibility {
  if (metadata === undefined || metadata.arity <= 0 || metadata.arity > 2) {
    return 'none';
  }
  const { detail } = metadata;
  switch (detail.type) {
    case 'class':
    case 'reference':
    case 'opposite':
      return 'all';
    case 'predicate':
      return detail.error ? 'must' : 'none';
    default:
      return 'none';
  }
}

export function isVisibilityAllowed(
  metadata: RelationMetadata | undefined,
  visibility: Visibility,
): boolean {
  if (metadata === undefined || metadata.arity <= 0 || metadata.arity > 2) {
    return visibility === 'none';
  }
  const { detail } = metadata;
  if (detail.type === 'predicate' && detail.error) {
    // We can't display may matches of error predicates,
    // because they have none by definition.
    return visibility !== 'all';
  }
  return true;
}

export default class GraphStore {
  semantics: SemanticsSuccessResult = {
    nodes: [],
    relations: [],
    partialInterpretation: {},
  };

  relationMetadata = new Map<string, RelationMetadata>();

  visibility = new Map<string, Visibility>();

  abbreviate = true;

  scopes = false;

  selectedSymbol: RelationMetadata | undefined;

  constructor(
    private readonly editorStore: EditorStore,
    private readonly nameOverride?: string,
  ) {
    makeAutoObservable<GraphStore, 'editorStore'>(this, {
      editorStore: false,
      semantics: observable.ref,
    });
  }

  getVisibility(relation: string): Visibility {
    const visibilityOverride = this.visibility.get(relation);
    if (visibilityOverride !== undefined) {
      return visibilityOverride;
    }
    return this.getDefaultVisibility(relation);
  }

  getDefaultVisibility(relation: string): Visibility {
    const metadata = this.relationMetadata.get(relation);
    return getDefaultVisibility(metadata);
  }

  isVisibilityAllowed(relation: string, visibility: Visibility): boolean {
    const metadata = this.relationMetadata.get(relation);
    return isVisibilityAllowed(metadata, visibility);
  }

  setVisibility(relation: string, visibility: Visibility): void {
    const metadata = this.relationMetadata.get(relation);
    if (metadata === undefined || !isVisibilityAllowed(metadata, visibility)) {
      return;
    }
    const defaultVisiblity = getDefaultVisibility(metadata);
    if (defaultVisiblity === visibility) {
      this.visibility.delete(relation);
    } else {
      this.visibility.set(relation, visibility);
    }
  }

  cycleVisibility(relation: string): void {
    const metadata = this.relationMetadata.get(relation);
    if (metadata === undefined) {
      return;
    }
    switch (this.getVisibility(relation)) {
      case 'none':
        if (isVisibilityAllowed(metadata, 'must')) {
          this.setVisibility(relation, 'must');
        }
        break;
      case 'must':
        {
          const next = isVisibilityAllowed(metadata, 'all') ? 'all' : 'none';
          this.setVisibility(relation, next);
        }
        break;
      default:
        this.setVisibility(relation, 'none');
        break;
    }
  }

  hideAll(): void {
    this.relationMetadata.forEach((metadata, name) => {
      if (getDefaultVisibility(metadata) === 'none') {
        this.visibility.delete(name);
      } else {
        this.visibility.set(name, 'none');
      }
    });
  }

  resetFilter(): void {
    this.visibility.clear();
  }

  getName({ name, simpleName }: { name: string; simpleName: string }): string {
    return this.abbreviate ? simpleName : name;
  }

  toggleAbbrevaite(): void {
    this.abbreviate = !this.abbreviate;
  }

  toggleScopes(): void {
    this.scopes = !this.scopes;
  }

  setSelectedSymbol(option: RelationMetadata | undefined): void {
    if (option === undefined) {
      this.selectedSymbol = undefined;
      return;
    }
    const metadata = this.relationMetadata.get(option.name);
    if (metadata !== undefined) {
      this.selectedSymbol = metadata;
    } else {
      this.selectedSymbol = undefined;
    }
  }

  setSemantics(semantics: SemanticsSuccessResult) {
    this.semantics = semantics;
    this.relationMetadata.clear();
    this.semantics.relations.forEach((metadata) => {
      this.relationMetadata.set(metadata.name, metadata);
    });
    const toRemove = new Set<string>();
    this.visibility.forEach((value, key) => {
      if (
        !this.isVisibilityAllowed(key, value) ||
        this.getDefaultVisibility(key) === value
      ) {
        toRemove.add(key);
      }
    });
    toRemove.forEach((key) => {
      this.visibility.delete(key);
    });
    this.setSelectedSymbol(this.selectedSymbol);
  }

  get colorNodes(): boolean {
    return this.editorStore.colorIdentifiers;
  }

  get name(): string {
    return this.nameOverride ?? this.editorStore.simpleNameOrFallback;
  }
}
