/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import type { Command } from '@codemirror/view';
import { action, makeObservable, observable } from 'mobx';

import getLogger from '../utils/getLogger';

import type EditorStore from './EditorStore';

const log = getLogger('editor.PanelStore');

export default class PanelStore {
  state = false;

  element: Element | undefined;

  constructor(
    readonly panelClass: string,
    private readonly openCommand: Command,
    private readonly closeCommand: Command,
    protected readonly store: EditorStore,
  ) {
    makeObservable<
      PanelStore,
      | 'openCommand'
      | 'closeCommand'
      | 'store'
      | 'setState'
      | 'doOpen'
      | 'doClose'
    >(this, {
      panelClass: false,
      openCommand: false,
      closeCommand: false,
      store: false,
      state: observable,
      element: observable,
      id: false,
      open: action,
      close: action,
      toggle: action,
      setState: false,
      synchronizeStateToView: action,
      doOpen: false,
      doClose: false,
    });
  }

  get id(): string {
    return `${this.store.id}-${this.panelClass}`;
  }

  open(): boolean {
    return this.setState(true);
  }

  close(): boolean {
    return this.setState(false);
  }

  toggle(): void {
    this.setState(!this.state);
  }

  private setState(newState: boolean): boolean {
    if (this.state === newState) {
      return false;
    }
    log.debug('Show', this.panelClass, 'panel', newState);
    if (newState) {
      this.doOpen();
    } else {
      this.doClose();
    }
    this.state = newState;
    return true;
  }

  synchronizeStateToView(): void {
    this.doClose();
    if (this.state) {
      this.doOpen();
    }
  }

  protected doOpen(): void {
    if (!this.store.doCommand(this.openCommand)) {
      return;
    }
    const { view } = this.store;
    if (view === undefined) {
      return;
    }
    // We always access the panel DOM element by class name, even for the search panel,
    // where we control the creation of the element, so that we can have a uniform way to
    // access panel created by both CodeMirror and us.
    this.element =
      view.dom.querySelector(`.${this.panelClass}.cm-panel`) ?? undefined;
    if (this.element === undefined) {
      log.error('Failed to add panel', this.panelClass, 'to DOM');
      return;
    }
    this.element.id = this.id;
    const closeButton = this.element.querySelector('button[name="close"]');
    if (closeButton !== null) {
      log.debug('Addig close button callback to', this.panelClass, 'panel');
      // We must remove the event listener from the button that dispatches a transaction
      // without going through `EditorStore`. This listened is added by CodeMirror,
      // and we can only remove it by cloning the DOM node: https://stackoverflow.com/a/9251864
      const closeButtonWithoutListeners = closeButton.cloneNode(true);
      closeButtonWithoutListeners.addEventListener('click', (event) => {
        this.close();
        event.preventDefault();
      });
      closeButton.replaceWith(closeButtonWithoutListeners);
    }
  }

  protected doClose(): void {
    this.store.doCommand(this.closeCommand);
    this.element = undefined;
  }
}
