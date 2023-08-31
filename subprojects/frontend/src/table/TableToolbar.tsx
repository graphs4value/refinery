/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import Stack from '@mui/material/Stack';
import {
  GridToolbarColumnsButton,
  GridToolbarContainer,
  GridToolbarExport,
  GridToolbarFilterButton,
} from '@mui/x-data-grid';

import type GraphStore from '../graph/GraphStore';

import SymbolSelector from './SymbolSelector';

export default function TableToolbar({
  graph,
}: {
  graph: GraphStore;
}): JSX.Element {
  return (
    <GridToolbarContainer
      sx={{
        display: 'flex',
        flexDirection: 'row',
        flexWrap: 'wrap-reverse',
        justifyContent: 'space-between',
      }}
    >
      <Stack direction="row" flexWrap="wrap">
        <GridToolbarColumnsButton />
        <GridToolbarFilterButton />
        <GridToolbarExport />
      </Stack>
      <SymbolSelector graph={graph} />
    </GridToolbarContainer>
  );
}
