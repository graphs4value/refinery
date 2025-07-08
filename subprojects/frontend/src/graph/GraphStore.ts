/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import type { Visibility } from '@tools.refinery/client';
import { makeAutoObservable, observable } from 'mobx';

import type EditorStore from '../editor/EditorStore';
import isBuiltIn from '../utils/isBuiltIn';
import type {
  RelationMetadata,
  SemanticsModelResult,
} from '../xtext/xtextServiceResults';

export type { Visibility } from '@tools.refinery/client';

// Supertype of `ModelSemanticsResult` and `GeneratedModelSemanticsResult`.
export type ModelResultWithSource = SemanticsModelResult & { source?: string };

function hideBuiltIn(
  metadata: RelationMetadata,
  visibility: Visibility,
): Visibility {
  return isBuiltIn(metadata) ? 'none' : visibility;
}

export function getDefaultVisibility(
  metadata: RelationMetadata | undefined,
): Visibility {
  if (metadata === undefined || metadata.arity <= 0 || metadata.arity > 2) {
    return 'none';
  }
  if (metadata.visibility) {
    return metadata.visibility;
  }
  const { detail } = metadata;
  switch (detail.type) {
    case 'class':
    case 'reference':
    case 'attribute':
    case 'opposite':
      return hideBuiltIn(metadata, 'all');
    case 'pred':
      switch (detail.kind) {
        case 'base':
          return hideBuiltIn(metadata, 'all');
        case 'error':
          return 'must';
        default:
          return 'none';
      }
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
  if (detail.type === 'pred' && detail.kind === 'error') {
    // We can't display may matches of error predicates,
    // because they have none by definition.
    return visibility !== 'all';
  }
  if (detail.type === 'computed') {
    return false;
  }
  return true;
}

function getComputedName(relationName: string) {
  return `${relationName}::computed`;
}

const TYPE_HASH_HEX_PREFFIX = '_';

export default class GraphStore {
  semantics: ModelResultWithSource = {
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
    public readonly editorStore: EditorStore,
    private readonly generatedModelName?: string,
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

  get generated(): boolean {
    return this.generatedModelName !== undefined;
  }

  get hasSource(): boolean {
    return (
      !!this.semantics.source ||
      // We currently don't serialize the source code for concretized models on the server.
      (!this.generated && !this.editorStore.concretize)
    );
  }

  get source(): string {
    const { source } = this.semantics;
    if (source) {
      return source;
    }
    if (!this.generated) {
      return this.editorStore.state.sliceDoc();
    }
    return '';
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

  get visibilityObject(): Record<string, Visibility> {
    return Object.fromEntries(this.visibility);
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

  get showComputed(): boolean {
    return this.editorStore.showComputed;
  }

  toggleShowComputed(): void {
    this.editorStore.toggleShowComputed();
  }

  setSemantics(semantics: SemanticsModelResult, source?: string): void {
    this.semantics = source
      ? {
          ...semantics,
          source,
        }
      : semantics;
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
    this.semantics.nodes.forEach(({ color }) => {
      if (color?.startsWith(TYPE_HASH_HEX_PREFFIX)) {
        const key = color.substring(TYPE_HASH_HEX_PREFFIX.length);
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
    return this.generatedModelName ?? this.editorStore.simpleNameOrFallback;
  }

  get showNonExistent(): boolean {
    const existsVisibility = this.visibility.get('builtin::exists') ?? 'none';
    return existsVisibility !== 'none' || this.scopes;
  }

  private hasComputed(relationName: string | undefined): boolean {
    if (relationName === undefined) {
      return false;
    }
    const computedName = getComputedName(relationName);
    const computedMetadata = this.relationMetadata.get(computedName);
    if (computedMetadata === undefined) {
      return false;
    }
    const { detail } = computedMetadata;
    return detail.type === 'computed' && detail.of === relationName;
  }

  getComputedName(relationName: string | undefined): string | undefined {
    if (relationName === undefined) {
      return undefined;
    }
    return this.hasComputed(relationName)
      ? getComputedName(relationName)
      : relationName;
  }

  get selectedSymbolHasComputed(): boolean {
    return this.hasComputed(this.editorStore.selectedSymbolName);
  }

  get concretize(): boolean {
    return this.generated || this.editorStore.concretize;
  }

  get upToDate(): boolean {
    return (
      this.generated ||
      this.editorStore.delayedErrors.semanticsUpToDate ||
      // Do not dim the graph and table views if there are no nodes to show
      // (e.g., during page loading).
      this.semantics.nodes.length === 0
    );
  }

  get dimView(): boolean {
    return !this.upToDate;
  }
}
