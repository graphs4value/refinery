/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import AddIcon from '@mui/icons-material/Add';
import CropFreeIcon from '@mui/icons-material/CropFree';
import RemoveIcon from '@mui/icons-material/Remove';
import IconButton from '@mui/material/IconButton';
import Stack from '@mui/material/Stack';
import ToggleButton from '@mui/material/ToggleButton';
import Tooltip from '@mui/material/Tooltip';

import type { ChangeZoomCallback, SetFitZoomCallback } from './ZoomCanvas';

export default function ZoomButtons({
  changeZoom,
  fitZoom,
  setFitZoom,
}: {
  changeZoom: ChangeZoomCallback;
  fitZoom: boolean;
  setFitZoom: SetFitZoomCallback;
}): JSX.Element {
  return (
    <Stack
      direction="column"
      p={1}
      sx={{ position: 'absolute', bottom: 0, right: 0 }}
    >
      <Tooltip title="Zoom in" placement="left">
        <IconButton onClick={() => changeZoom(2)}>
          <AddIcon fontSize="small" />
        </IconButton>
      </Tooltip>
      <Tooltip title="Zoom out" placement="left">
        <IconButton onClick={() => changeZoom(0.5)}>
          <RemoveIcon fontSize="small" />
        </IconButton>
      </Tooltip>
      <Tooltip title="Fit screen" placement="left">
        <ToggleButton
          value="show-replace"
          selected={fitZoom}
          onClick={() => setFitZoom(!fitZoom)}
          size="small"
          className="iconOnly"
        >
          <CropFreeIcon fontSize="small" />
        </ToggleButton>
      </Tooltip>
    </Stack>
  );
}
