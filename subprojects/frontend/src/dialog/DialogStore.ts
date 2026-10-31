/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { makeAutoObservable, observable } from 'mobx';
import { nanoid } from 'nanoid';

export interface ConfirmationAction {
  label: string;
  color?: 'inherit' | 'error';
  defaultAction?: boolean;
  onClick: (dialogId: string) => void | Promise<void>;
}

export type ConfirmationDialogKind = 'openFile' | 'close' | 'cliSymlink';

export interface ConfirmationDialogState {
  title: string;
  body: string;
  kind: ConfirmationDialogKind;
  dismissible: boolean;
  onDismiss?: () => void;
  actions: readonly ConfirmationAction[];
  id: string;
}

export type ConfirmationDialogConfig = Omit<ConfirmationDialogState, 'id'>;

export interface ErrorDialogState {
  title: string;
  body: string;
  fatal: boolean;
}

export default class DialogStore {
  confirmationDialogs: ConfirmationDialogState[] = [];

  errorDialog: ErrorDialogState | undefined = undefined;

  constructor() {
    makeAutoObservable<DialogStore, 'confirmationDialogs' | 'errorDialog'>(
      this,
      {
        confirmationDialogs: observable.ref,
        errorDialog: observable.ref,
      },
    );
  }

  hasConfirmation(kind: ConfirmationDialogKind): boolean {
    return this.confirmationDialogs.some((dialog) => dialog.kind === kind);
  }

  showConfirmation(dialog: ConfirmationDialogConfig): void {
    this.confirmationDialogs = [
      ...this.confirmationDialogs,
      { ...dialog, id: nanoid() },
    ];
  }

  dismissConfirmation(dialogId: string): void {
    this.confirmationDialogs = this.confirmationDialogs.filter(
      ({ id }) => id !== dialogId,
    );
  }

  showError(title: string, body: string, fatal = false): void {
    // Once initialization has failed, later recoverable errors must not
    // expose an application that was never initialized successfully.
    if (this.errorDialog?.fatal) {
      return;
    }
    this.errorDialog = { title, body, fatal };
  }

  dismissError(): void {
    this.errorDialog = undefined;
  }
}
