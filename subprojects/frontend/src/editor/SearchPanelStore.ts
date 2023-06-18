/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import {
  closeSearchPanel,
  findNext,
  findPrevious,
  getSearchQuery,
  openSearchPanel,
  replaceAll,
  replaceNext,
  SearchQuery,
  setSearchQuery,
} from '@codemirror/search';
import { action, computed, makeObservable, observable, override } from 'mobx';

import type EditorStore from './EditorStore';
import PanelStore from './PanelStore';

export default class SearchPanelStore extends PanelStore {
  searchField: HTMLInputElement | undefined;

  constructor(store: EditorStore) {
    // Use a custom class name to avoid specificity issues with
    // CodeMirror `.cm-search.cm-panel` CSS styles.
    super('refinery-cm-search', openSearchPanel, closeSearchPanel, store);
    makeObservable<SearchPanelStore, 'selectSearchField'>(this, {
      searchField: observable.ref,
      query: computed,
      invalidRegexp: computed,
      open: override,
      setSearchField: action,
      updateQuery: action,
      findNext: action,
      findPrevious: action,
      replaceNext: action,
      replaceAll: action,
      selectSearchField: false,
    });
  }

  setSearchField(newSearchField: HTMLInputElement | undefined): void {
    this.searchField = newSearchField;
    if (this.state) {
      this.selectSearchField();
    }
  }

  get query(): SearchQuery {
    return getSearchQuery(this.store.state);
  }

  get invalidRegexp(): boolean {
    const { search, valid } = this.query;
    return !valid && search !== '';
  }

  updateQuery(newQueryOptions: {
    search?: string;
    caseSensitive?: boolean;
    literal?: boolean;
    regexp?: boolean;
    replace?: string;
  }): void {
    const { search, caseSensitive, literal, regexp, replace } = this.query;
    const newQuery = new SearchQuery({
      search,
      caseSensitive,
      literal,
      regexp,
      replace,
      ...newQueryOptions,
      ...(newQueryOptions.regexp === true && { literal: false }),
      ...(newQueryOptions.literal === true && { regexp: false }),
    });
    this.store.dispatch({
      effects: [setSearchQuery.of(newQuery)],
    });
  }

  findNext(): void {
    this.store.doCommand(findNext);
  }

  findPrevious(): void {
    this.store.doCommand(findPrevious);
  }

  replaceNext(): void {
    this.store.doCommand(replaceNext);
  }

  replaceAll(): void {
    this.store.doCommand(replaceAll);
  }

  override open(): boolean {
    return super.open() || this.selectSearchField();
  }

  private selectSearchField(): boolean {
    if (this.searchField === undefined) {
      return false;
    }
    this.searchField.select();
    return true;
  }
}
