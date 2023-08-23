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

export default function ZoomButtons({
  changeZoom,
  fitZoom,
}: {
  changeZoom: (event: React.MouseEvent, factor: number) => void;
  fitZoom: (event: React.MouseEvent) => void;
}): JSX.Element {
  return (
    <Stack
      direction="column"
      p={1}
      sx={{ position: 'absolute', bottom: 0, right: 0 }}
    >
      <IconButton
        aria-label="Zoom in"
        onClick={(event) => changeZoom(event, 2)}
      >
        <AddIcon fontSize="small" />
      </IconButton>
      <IconButton
        aria-label="Zoom out"
        onClick={(event) => changeZoom(event, 0.5)}
      >
        <RemoveIcon fontSize="small" />
      </IconButton>
      <IconButton aria-label="Fit screen" onClick={fitZoom}>
        <CropFreeIcon fontSize="small" />
      </IconButton>
    </Stack>
  );
}
