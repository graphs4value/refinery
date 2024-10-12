/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { makeAutoObservable, observable } from 'mobx';

import type EditorStore from '../editor/EditorStore';
import type {
  RelationMetadata,
  SemanticsModelResult,
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
    case 'base':
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

const TYPE_HASH_HEX_PREFFIX = '_';

export default class GraphStore {
  semantics: SemanticsModelResult = {
    nodes: [],
    relations: [],
    partialInterpretation: {},
  };

  relationMetadata = new Map<string, RelationMetadata>();

  visibility: Map<string, Visibility>;

  abbreviate = true;

  scopes = false;

  hexTypeHashes: string[] = [];

  private typeHashesMap = new Map<string, number>();

  constructor(
    private readonly editorStore: EditorStore,
    private readonly nameOverride?: string,
    visibility?: Map<string, Visibility>,
  ) {
    if (visibility === undefined) {
      this.visibility = new Map<string, Visibility>();
    } else {
      this.visibility = new Map(visibility);
    }
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

  get selectedSymbol(): RelationMetadata | undefined {
    const { selectedSymbolName } = this.editorStore;
    if (selectedSymbolName === undefined) {
      return undefined;
    }
    return this.relationMetadata.get(selectedSymbolName);
  }

  setSelectedSymbol(option: RelationMetadata | undefined): void {
    this.editorStore.setSelectedSymbolName(option?.name);
  }

  setSemantics(semantics: SemanticsModelResult) {
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
    this.updateTypeHashes();
  }

  /**
   * Maintains a list of past and current color codes to avoid flashing
   * when the graph view updates.
   *
   * As long as the previously used colors are still in in `typeHashesMap`,
   * the view will not flash while Graphviz is recomputing, because we'll
   * keep emitting styles for the colors.
   */
  private updateTypeHashes(): void {
    this.semantics.nodes.forEach(({ typeHash }) => {
      if (
        typeHash !== undefined &&
        typeHash.startsWith(TYPE_HASH_HEX_PREFFIX)
      ) {
        const key = typeHash.substring(TYPE_HASH_HEX_PREFFIX.length);
        this.typeHashesMap.set(key, 0);
      }
    });
    this.hexTypeHashes = Array.from(this.typeHashesMap.keys());
    this.hexTypeHashes.forEach((typeHash) => {
      const age = this.typeHashesMap.get(typeHash);
      if (age !== undefined && age < 10) {
        this.typeHashesMap.set(typeHash, age + 1);
      } else {
        this.typeHashesMap.delete(typeHash);
      }
    });
  }

  get colorNodes(): boolean {
    return this.editorStore.colorIdentifiers;
  }

  get name(): string {
    return this.nameOverride ?? this.editorStore.simpleNameOrFallback;
  }

  get showNonExistent(): boolean {
    const existsVisibility = this.visibility.get('builtin::exists') ?? 'none';
    return existsVisibility !== 'none' || this.scopes;
  }
}
