import type { Command } from '@codemirror/view';
import { action, makeObservable, observable } from 'mobx';

import getLogger from '../utils/getLogger';

import type EditorStore from './EditorStore';

const log = getLogger('editor.PanelStore');

export default class PanelStore {
  state = false;

  constructor(
    private readonly panelId: string,
    private readonly openCommand: Command,
    private readonly closeCommand: Command,
    private readonly store: EditorStore,
  ) {
    makeObservable(this, {
      state: observable,
      open: action,
      close: action,
      toggle: action,
      synchronizeStateToView: action,
    });
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
    log.debug('Show', this.panelId, 'panel', newState);
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

  private doOpen(): void {
    if (!this.store.doCommand(this.openCommand)) {
      return;
    }
    const { view } = this.store;
    if (view === undefined) {
      return;
    }
    const buttonQuery = `.cm-${this.panelId}.cm-panel button[name="close"]`;
    const closeButton = view.dom.querySelector(buttonQuery);
    if (closeButton !== null) {
      log.debug('Addig close button callback to', this.panelId, 'panel');
      // We must remove the event listener from the button that dispatches a transaction
      // without going through `EditorStore`. This listened is added by CodeMirror,
      // and we can only remove it by cloning the DOM node: https://stackoverflow.com/a/9251864
      const closeButtonWithoutListeners = closeButton.cloneNode(true);
      closeButtonWithoutListeners.addEventListener('click', (event) => {
        this.close();
        event.preventDefault();
      });
      closeButton.replaceWith(closeButtonWithoutListeners);
    } else {
      log.error('Opened', this.panelId, 'panel has no close button');
    }
  }

  private doClose(): void {
    this.store.doCommand(this.closeCommand);
  }
}
