/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import MuiDialog, { type DialogProps } from '@mui/material/Dialog';
import { useEffect, useId } from 'react';

export default function Dialog(props: DialogProps) {
  const id = useId();
  const { open } = props;
  useEffect(() => {
    if (open && 'refinery' in window) {
      window.refinery.openDialog(id);
      return () => window.refinery?.closeDialog(id);
    }
    // If we aren't running in Electron or the dialog is not open,
    // there's nothing to clean up.
    return undefined;
  }, [id, open]);

  return (
    <MuiDialog
      {...props}
      slotProps={{
        ...(props.slotProps ?? {}),
        backdrop: {
          ...(props.slotProps?.backdrop ?? {}),
          sx: {
            ...((
              props.slotProps?.backdrop as unknown as {
                sx?: Record<string, unknown>;
              }
            )?.sx ?? {}),
            appRegion: 'no-drag',
          },
        },
      }}
    />
  );
}
