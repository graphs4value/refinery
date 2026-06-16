/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import FilterListIcon from '@mui/icons-material/FilterList';
import SaveAltIcon from '@mui/icons-material/SaveAlt';
import ViewColumnIcon from '@mui/icons-material/ViewColumn';
import IconButton from '@mui/material/IconButton';
import Stack from '@mui/material/Stack';
import Tooltip from '@mui/material/Tooltip';
import {
  ColumnsPanelTrigger,
  ExportCsv,
  FilterPanelTrigger,
} from '@mui/x-data-grid';

import type GraphStore from '../graph/GraphStore';

import SymbolSelector from './SymbolSelector';

export default function TableToolbar({
  graph,
}: {
  graph: GraphStore;
}): React.ReactElement {
  return (
    <Stack
      direction="row"
      className="TableToolbar-root"
      sx={(theme) => ({
        flexWrap: 'wrap',
        alignItems: 'center',
        px: theme.spacing(1),
        py: theme.spacing(0.5),
      })}
    >
      <Stack
        direction="row"
        sx={{
          height: (theme) => theme.spacing(5),
          alignItems: 'center',
          flexBasis: 200,
          maxWidth: 600,
          flexGrow: 1000,
          flexShrink: 1,
        }}
      >
        <SymbolSelector graph={graph} />
      </Stack>
      <Stack
        direction="row"
        sx={{
          flexGrow: 1,
          alignItems: 'center',
          justifyContent: 'flex-end',
        }}
      >
        <Tooltip title="Columns">
          <ColumnsPanelTrigger render={<IconButton color="inherit" />}>
            <ViewColumnIcon fontSize="inherit" />
          </ColumnsPanelTrigger>
        </Tooltip>
        <Tooltip title="Filter">
          <FilterPanelTrigger render={<IconButton color="inherit" />}>
            <FilterListIcon fontSize="inherit" />
          </FilterPanelTrigger>
        </Tooltip>
        <Tooltip title="Export CSV">
          <ExportCsv render={<IconButton color="inherit" />}>
            <SaveAltIcon fontSize="inherit" />
          </ExportCsv>
        </Tooltip>
      </Stack>
    </Stack>
  );
}
