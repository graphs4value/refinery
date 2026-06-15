/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import FilterListIcon from '@mui/icons-material/FilterList';
import SaveAltIcon from '@mui/icons-material/SaveAlt';
import ViewColumnIcon from '@mui/icons-material/ViewColumn';
import Button from '@mui/material/Button';
import Checkbox from '@mui/material/Checkbox';
import FormControlLabel from '@mui/material/FormControlLabel';
import Stack from '@mui/material/Stack';
import Tooltip from '@mui/material/Tooltip';
import { styled } from '@mui/material/styles';
import {
  ColumnsPanelTrigger,
  ExportCsv,
  FilterPanelTrigger,
} from '@mui/x-data-grid';
import { observer } from 'mobx-react-lite';

import type GraphStore from '../graph/GraphStore';

import SymbolSelector from './SymbolSelector';

const DimLabel = styled(FormControlLabel)(({ theme }) => ({
  margin: '-4px 8px -4px 0',
  '.MuiFormControlLabel-label': {
    ...theme.typography.body2,
    color: theme.palette.text.secondary,
    userSelect: 'none',
  },
}));

const ComputedCheckbox = observer(function ComputedCheckbox({
  graph,
}: {
  graph: GraphStore;
}) {
  const { selectedSymbolHasComputed } = graph;

  return (
    <Tooltip title="Use only forward reasoning">
      <DimLabel
        control={
          <Checkbox
            disabled={!selectedSymbolHasComputed}
            checked={graph.showComputed && selectedSymbolHasComputed}
            onClick={() => graph.toggleShowComputed()}
            size="small"
          />
        }
        label="Computed"
      />
    </Tooltip>
  );
});

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
        justifyContent: 'space-between',
        p: theme.spacing(1),
        rowGap: theme.spacing(1),
        // Correct for the non-integer height of the text box to match up with the editor area toolbar.
        marginBottom: '-0.5px',
      })}
    >
      <SymbolSelector graph={graph} />
      <Stack
        direction="row"
        sx={{ flexGrow: 1, flexWrap: 'wrap', justifyContent: 'flex-end' }}
      >
        <Stack
          direction="row"
          sx={{ flexGrow: 1, mr: (theme) => theme.spacing(2) }}
        >
          <ComputedCheckbox graph={graph} />
        </Stack>
        <ColumnsPanelTrigger
          render={
            <Button
              size="small"
              color="inherit"
              startIcon={<ViewColumnIcon />}
            />
          }
        >
          Columns
        </ColumnsPanelTrigger>
        <FilterPanelTrigger
          render={
            <Button
              size="small"
              color="inherit"
              startIcon={<FilterListIcon />}
            />
          }
        >
          Filter
        </FilterPanelTrigger>
        <ExportCsv
          render={
            <Button size="small" color="inherit" startIcon={<SaveAltIcon />} />
          }
        >
          Export
        </ExportCsv>
      </Stack>
    </Stack>
  );
}
