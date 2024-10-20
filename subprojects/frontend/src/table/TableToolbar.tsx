/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import Checkbox from '@mui/material/Checkbox';
import FormControlLabel from '@mui/material/FormControlLabel';
import Stack from '@mui/material/Stack';
import Tooltip from '@mui/material/Tooltip';
import { styled } from '@mui/material/styles';
import {
  GridToolbarColumnsButton,
  GridToolbarContainer,
  GridToolbarExport,
  GridToolbarFilterButton,
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
        <ComputedCheckbox graph={graph} />
      </Stack>
      <SymbolSelector graph={graph} />
    </GridToolbarContainer>
  );
}
