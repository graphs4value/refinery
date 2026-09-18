/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Typography from '@mui/material/Typography';
import { observer } from 'mobx-react-lite';
import { useState } from 'react';

import { useRootStore } from '../RootStoreProvider';
import getLogger from '../utils/getLogger';

import Dialog from './Dialog';
import DialogActionBar from './DialogActionBar';
import type DialogStore from './DialogStore';
import type { ConfirmationDialogState } from './DialogStore';
import DialogTitleBar from './DialogTitleBar';

const log = getLogger('ConfirmationDialog');

function ConfirmationDialogView({
  dialogStore,
  dialog,
}: {
  dialogStore: DialogStore;
  dialog: ConfirmationDialogState;
}): React.ReactElement {
  const [pending, setPending] = useState(false);

  const dismiss = () => {
    if (!pending) {
      dialogStore.dismissConfirmation(dialog.id);
      dialog.onDismiss?.();
    }
  };

  const invoke = (action: (dialogId: string) => void | Promise<void>) => {
    if (pending) {
      return;
    }
    setPending(true);
    void (async () => {
      try {
        await action(dialog.id);
      } finally {
        setPending(false);
      }
    })().catch((error) => {
      log.error({ err: error }, 'Error in dialog action');
    });
  };

  const handleKeyDown = (event: React.KeyboardEvent<HTMLDivElement>) => {
    if (event.key !== 'Enter' || event.defaultPrevented) {
      return;
    }
    // Only submit when the dialog surface itself has focus.
    if (event.target !== event.currentTarget) {
      return;
    }
    const defaultAction = dialog.actions.find(
      (action) => action.defaultAction === true,
    );
    if (defaultAction !== undefined) {
      event.preventDefault();
      invoke(defaultAction.onClick);
    }
  };

  return (
    <Dialog
      open
      onClose={dialog.dismissible ? dismiss : undefined}
      slotProps={{
        paper: {
          onKeyDown: handleKeyDown,
        },
      }}
      maxWidth="sm"
    >
      <DialogTitleBar
        close={dialog.dismissible ? dismiss : undefined}
        title={dialog.title}
      />
      <Box sx={{ p: 2 }}>
        <Typography>{dialog.body}</Typography>
      </Box>
      <DialogActionBar>
        {dialog.actions.map((action) => (
          <Button
            key={action.label}
            color={action.color}
            disabled={pending}
            onClick={() => invoke(action.onClick)}
          >
            {action.label}
          </Button>
        ))}
      </DialogActionBar>
    </Dialog>
  );
}

export default observer(function ConfirmationDialog(): React.ReactElement {
  const { dialogStore } = useRootStore();
  return (
    <>
      {dialogStore.confirmationDialogs.map((dialog) => (
        <ConfirmationDialogView
          key={dialog.id}
          dialogStore={dialogStore}
          dialog={dialog}
        />
      ))}
    </>
  );
});
