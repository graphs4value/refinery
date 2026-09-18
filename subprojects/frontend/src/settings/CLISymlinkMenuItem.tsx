/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import MenuItem from '@mui/material/MenuItem';
import { observer } from 'mobx-react-lite';

import { useRootStore } from '../RootStoreProvider';
import getLogger from '../utils/getLogger';

const log = getLogger('settings.CLISymlinkMenuItem');

export default observer(function CLISymlinkMenuItem({
  onClose,
}: {
  onClose: () => void;
}): React.ReactElement | null {
  const { cliSymlinkStore } = useRootStore();
  const { status } = cliSymlinkStore;

  if (status === undefined || status === 'unsupported') {
    return null;
  }
  const enabled = status === 'correct';
  const needsRepair = status === 'missing' || status === 'incorrect';

  return (
    <MenuItem
      disabled={cliSymlinkStore.actionInFlight}
      onClick={() => {
        onClose();
        cliSymlinkStore.update(!enabled).catch((error: unknown) => {
          // CLISymlinkStore handles expected failures; this is only a final
          // guard for an unexpected rejection from the shared action.
          log.error({ err: error }, 'Unexpected CLI symlink update failure');
        });
      }}
    >
      {enabled
        ? 'Remove command-line launcher'
        : needsRepair
          ? 'Repair command-line launcher'
          : 'Create command-line launcher'}
    </MenuItem>
  );
});
