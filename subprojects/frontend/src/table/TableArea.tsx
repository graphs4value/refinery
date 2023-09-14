/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import Box from '@mui/material/Box';
import {
  DataGrid,
  type GridRenderCellParams,
  type GridColDef,
} from '@mui/x-data-grid';
import { observer } from 'mobx-react-lite';
import { useMemo } from 'react';

import type GraphStore from '../graph/GraphStore';

import TableToolbar from './TableToolbar';
import ValueRenderer from './ValueRenderer';

interface Row {
  nodes: string[];
  value: string;
}

function TableArea({ graph }: { graph: GraphStore }): JSX.Element {
  const {
    selectedSymbol,
    semantics: { nodes, partialInterpretation },
  } = graph;
  const symbolName = selectedSymbol?.name;
  const arity = selectedSymbol?.arity ?? 0;

  const columns = useMemo<GridColDef<Row>[]>(() => {
    const defs: GridColDef<Row>[] = [];
    for (let i = 0; i < arity; i += 1) {
      defs.push({
        field: `n${i}`,
        headerName: String(i + 1),
        valueGetter: (row) => row.row.nodes[i] ?? '',
        flex: 1,
      });
    }
    defs.push({
      field: 'value',
      headerName: 'Value',
      flex: 1,
      renderCell: ({ value }: GridRenderCellParams<Row, string>) => (
        <ValueRenderer value={value} />
      ),
    });
    return defs;
  }, [arity]);

  const rows = useMemo<Row[]>(() => {
    if (symbolName === undefined) {
      return [];
    }
    const interpretation = partialInterpretation[symbolName] ?? [];
    return interpretation.map((tuple) => {
      const nodeNames: string[] = [];
      for (let i = 0; i < arity; i += 1) {
        const index = tuple[i];
        if (typeof index === 'number') {
          const node = nodes[index];
          if (node !== undefined) {
            nodeNames.push(node.name);
          }
        }
      }
      return {
        nodes: nodeNames,
        value: String(tuple[arity]),
      };
    });
  }, [arity, nodes, partialInterpretation, symbolName]);

  return (
    <Box
      width="100%"
      height="100%"
      p={1}
      sx={(theme) => ({
        '.MuiDataGrid-withBorderColor': {
          borderColor:
            theme.palette.mode === 'dark'
              ? theme.palette.divider
              : theme.palette.outer.border,
        },
      })}
    >
      <DataGrid
        slots={{ toolbar: TableToolbar }}
        slotProps={{
          toolbar: {
            graph,
          },
        }}
        density="compact"
        rowSelection={false}
        columns={columns}
        rows={rows}
        getRowId={(row) => row.nodes.join(',')}
      />
    </Box>
  );
}

export default observer(TableArea);
