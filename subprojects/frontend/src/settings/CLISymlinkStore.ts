/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import { makeAutoObservable, runInAction } from 'mobx';

import RefineryContextBridge, {
  CLISymlinkPromptClaimResult,
  CLISymlinkResult,
  CLISymlinkState,
  CLISymlinkStatus,
} from '../RefineryContextBridge';
import type DialogStore from '../dialog/DialogStore';
import getLogger from '../utils/getLogger';

const log = getLogger('settings.CLISymlinkStore');

type CLISymlinkOperation = 'create' | 'remove' | 'disable';

function getCLISymlinkFailure(
  operation: CLISymlinkOperation,
  reason?: 'failed' | 'occupied' | 'busy',
): { title: string; body: string } {
  if (reason === 'busy') {
    return {
      title: 'Command-line launcher operation in progress',
      body: 'Another command-line launcher operation is already in progress. Try again later.',
    };
  }
  switch (operation) {
    case 'create':
      return reason === 'occupied'
        ? {
            title: 'Failed to create command-line launcher',
            body: 'A file already exists at “/usr/local/bin/refinery”. Remove it and try again.',
          }
        : {
            title: 'Failed to create command-line launcher',
            body: 'The command-line launcher could not be created.',
          };
    case 'remove':
      return {
        title: 'Failed to remove command-line launcher',
        body: 'The command-line launcher could not be removed.',
      };
    case 'disable':
      return {
        title: 'Failed to update command-line launcher preference',
        body: 'The command-line launcher preference could not be saved.',
      };
  }
}

export default class CLISymlinkStore {
  status: CLISymlinkStatus | undefined = undefined;

  private localActionInFlight = false;

  private remoteActionInFlight = false;

  private editorReady = false;

  private promptResolved = false;

  private dialogClaimInFlight = false;

  private setLocalActionInFlight(value: boolean): void {
    this.localActionInFlight = value;
  }

  get actionInFlight(): boolean {
    return this.localActionInFlight || this.remoteActionInFlight;
  }

  constructor(
    private readonly dialogStore: DialogStore,
    private readonly refinery: RefineryContextBridge | undefined,
  ) {
    makeAutoObservable<
      CLISymlinkStore,
      | 'dialogStore'
      | 'refinery'
      | 'editorReady'
      | 'promptResolved'
      | 'dialogClaimInFlight'
      | 'showConfirmationAsync'
      | 'invokeAction'
    >(this, {
      dialogStore: false,
      refinery: false,
      editorReady: false,
      promptResolved: false,
      dialogClaimInFlight: false,
      showConfirmationAsync: false,
      invokeAction: false,
    });
    if (this.refinery !== undefined) {
      this.refinery.onCLISymlinkStatusChange((rawState) =>
        this.handleStatusChange(rawState),
      );
    }
  }

  editorInitialized(): void {
    this.editorReady = true;
    this.showConfirmation();
  }

  async update(enabled: boolean): Promise<boolean> {
    return this.invokeAction(
      enabled ? 'create' : 'remove',
      (refinery) => refinery.setCLISymlink(enabled),
      enabled ? 'correct' : 'disabled',
    );
  }

  private disablePreference(showFailure = true): Promise<boolean> {
    return this.invokeAction(
      'disable',
      (refinery) => refinery.setCLISymlinkPreference(false),
      'disabled',
      showFailure,
    );
  }

  private handleStatusChange(rawState: unknown): void {
    const state = CLISymlinkState.safeParse(rawState);
    if (!state.success) {
      log.error(
        { err: state.error },
        'Received invalid CLI symlink status from Electron',
      );
      return;
    }
    runInAction(() => {
      this.status = state.data.status;
      this.remoteActionInFlight = state.data.actionInFlight;
    });
    if (!this.promptResolved) {
      this.showConfirmation();
    }
  }

  private async invokeAction(
    operation: CLISymlinkOperation,
    action: (refinery: RefineryContextBridge) => Promise<CLISymlinkResult>,
    successStatus: CLISymlinkStatus,
    showFailure = true,
  ): Promise<boolean> {
    if (this.refinery === undefined) {
      return false;
    }
    if (this.actionInFlight) {
      // A menu can be reopened while the previous privileged action is still
      // waiting for authorization; do not start a second action in parallel.
      return false;
    }
    this.setLocalActionInFlight(true);
    let result: CLISymlinkResult | undefined;
    try {
      const rawResult = await action(this.refinery);
      result = CLISymlinkResult.parse(rawResult);
      if (result === true) {
        runInAction(() => {
          this.status = successStatus;
        });
        return true;
      }
    } catch (error: unknown) {
      log.error({ err: error }, 'Failed to update CLI symlink');
    } finally {
      this.setLocalActionInFlight(false);
    }
    if (showFailure) {
      const { title, body } = getCLISymlinkFailure(
        operation,
        typeof result === 'object' ? result.reason : undefined,
      );
      this.dialogStore.showError(title, body);
    }
    return false;
  }

  private showConfirmation(): void {
    this.showConfirmationAsync().catch((error: unknown) => {
      log.error({ err: error }, 'Failed to show CLI symlink confirmation');
    });
  }

  private async showConfirmationAsync(): Promise<void> {
    const { status } = this;
    if (
      !this.editorReady ||
      status === undefined ||
      this.dialogStore.errorDialog?.fatal === true ||
      this.promptResolved ||
      this.dialogClaimInFlight ||
      this.actionInFlight ||
      this.refinery === undefined
    ) {
      return;
    }
    if (
      status === 'unsupported' ||
      status === 'disabled' ||
      status === 'correct'
    ) {
      this.promptResolved = true;
      return;
    }
    this.dialogClaimInFlight = true;
    try {
      const shouldShow = CLISymlinkPromptClaimResult.parse(
        await this.refinery.claimCLISymlinkPrompt(),
      );
      this.promptResolved = true;
      if (!shouldShow) {
        return;
      }
      const initial = status === 'notConfigured';
      const update = async (dialogId: string, enabled: boolean) => {
        const success = enabled
          ? await this.update(true)
          : await this.disablePreference();
        if (success) {
          this.dialogStore.dismissConfirmation(dialogId);
        }
      };
      this.dialogStore.showConfirmation({
        kind: 'cliSymlink',
        title: initial
          ? 'Create command-line launcher?'
          : 'Repair command-line launcher?',
        body: initial
          ? 'Create a command-line launcher at “/usr/local/bin/refinery” so you can start Refinery and run model generation and export commands from a terminal?'
          : 'The command-line launcher at “/usr/local/bin/refinery” is missing or points to the wrong application. Recreate it? If you decline, the existing link will be left unchanged.',
        dismissible: true,
        onDismiss: () => {
          this.disablePreference(false).catch((error: unknown) => {
            log.error(
              { err: error },
              'Failed to disable CLI symlink preference after dismissal',
            );
          });
        },
        actions: [
          {
            label: initial ? 'Create launcher' : 'Repair launcher',
            defaultAction: true,
            onClick: (dialogId) => update(dialogId, true),
          },
          {
            label: initial ? "Don't create" : 'Keep it as is',
            color: 'inherit',
            onClick: (dialogId) => update(dialogId, false),
          },
        ],
      });
    } finally {
      this.dialogClaimInFlight = false;
    }
  }
}
