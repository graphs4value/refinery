/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import MuiTooltip, { TooltipProps } from '@mui/material/Tooltip';

export default function Tooltip({
  children,
  ...props
}: TooltipProps): React.ReactElement {
  return (
    <MuiTooltip {...props}>
      <span className="RefineryTooltip-Container">{children}</span>
    </MuiTooltip>
  );
}
