/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import MuiDialogActions, {
  type DialogActionsProps,
} from '@mui/material/DialogActions';
import { styled } from '@mui/material/styles';

const DialogActionBarRoot = styled(MuiDialogActions, {
  name: 'DialogActionBar-Root',
  shouldForwardProp: (propName) => propName !== 'divider',
})<{ divider: boolean }>(({ theme, divider }) => ({
  padding: theme.spacing(1),
  ...(divider ? { borderTop: `1px solid ${theme.palette.divider}` } : {}),
}));

export default function DialogActionBar({
  divider = true,
  ...props
}: DialogActionsProps & { divider?: boolean }): React.ReactElement {
  return <DialogActionBarRoot divider={divider} {...props} />;
}
