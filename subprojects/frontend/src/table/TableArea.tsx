/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import Box from '@mui/material/Box';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { alpha, styled } from '@mui/material/styles';
import {
  DataGrid,
  type GridRenderCellParams,
  type GridColDef,
} from '@mui/x-data-grid';
import { observer } from 'mobx-react-lite';
import { useMemo } from 'react';

import type GraphStore from '../graph/GraphStore';
import RelationName from '../graph/RelationName';
import { binarySearch } from '../graph/valueUtils';

import TableToolbar from './TableToolbar';
import ValueRenderer, { WrappedValue } from './ValueRenderer';

class NodeName {
  constructor(
    public readonly name: string,
    public readonly missing: boolean,
  ) {}

  toString() {
    return this.name;
  }
}

interface Row {
  nodes: NodeName[];
  value: WrappedValue;
}

declare module '@mui/x-data-grid' {
  // Declare our custom prop type for `TableToolbar`.
  interface ToolbarPropsOverrides {
    graph: GraphStore;
  }

  interface NoRowsOverlayPropsOverrides {
    graph: GraphStore;
  }

  interface NoResultsOverlayPropsOverrides {
    graph: GraphStore;
  }
}

const noSymbolMessage =
  'Please select a symbol from the list to view its interpretation';

function NoRowsOverlay({
  graph: { selectedSymbol },
}: {
  graph: GraphStore;
}): React.ReactElement {
  return (
    <Stack
      sx={{
        height: '100%',
        alignItems: 'center',
        justifyContent: 'center',
        textAlign: 'center',
        p: (theme) => theme.spacing(2),
      }}
    >
      {selectedSymbol === undefined ? (
        noSymbolMessage
      ) : (
        <span>
          Interpretation of <RelationName metadata={selectedSymbol} /> is empty
        </span>
      )}
    </Stack>
  );
}

function NoResultsOverlay({
  graph: { selectedSymbol },
}: {
  graph: GraphStore;
}): React.ReactElement {
  return (
    <Stack
      sx={{
        height: '100%',
        alignItems: 'center',
        justifyContent: 'center',
      }}
    >
      {selectedSymbol === undefined ? (
        noSymbolMessage
      ) : (
        <span>
          No results in the interpretation of{' '}
          <RelationName metadata={selectedSymbol} />
        </span>
      )}
    </Stack>
  );
}

const NodeNameRenderer = styled('span', {
  name: 'TableArea-NodeNameRenderer',
  shouldForwardProp: (prop) => prop !== 'missing',
})<{ missing: boolean }>(({ theme, missing }) => ({
  color: missing ? theme.palette.text.secondary : theme.palette.text.primary,
  textDecoration: missing ? 'line-through' : 'none',
}));

function TableArea({
  graph,
  touchesTop,
}: {
  graph: GraphStore;
  touchesTop: boolean;
}): React.ReactElement {
  const { concretize, selectedSymbol, showComputed, semantics, dimView } =
    graph;
  const { nodes, partialInterpretation } = semantics;
  const symbolName = selectedSymbol?.name;
  const computedName = showComputed
    ? graph.getComputedName(symbolName)
    : symbolName;
  const arity = selectedSymbol?.arity ?? 0;
  const attribute = selectedSymbol?.dataType !== undefined;
  const parameterNames = selectedSymbol?.parameterNames;

  const cachedConcretize = useMemo(
    () => concretize,
    /* eslint-disable-next-line react-hooks/exhaustive-deps --
     * Deliberately only update `concretize` whenever `semantics` changes to avoid flashing colors.
     */
    [semantics],
  );

  const columns = useMemo<GridColDef<Row>[]>(() => {
    const namesOrEmpty = parameterNames ?? [];
    const defs: GridColDef<Row>[] = [];
    for (let i = 0; i < arity; i += 1) {
      defs.push({
        field: `n${i}`,
        headerName: namesOrEmpty[i] ?? String(i + 1),
        valueGetter: (_, row) => row.nodes[i] ?? '',
        flex: 1,
        renderCell: ({ value }: GridRenderCellParams<Row, NodeName>) => (
          <NodeNameRenderer missing={value?.missing ?? false}>
            {value?.name}
          </NodeNameRenderer>
        ),
      });
    }
    defs.push({
      field: 'value',
      headerName: namesOrEmpty.includes('value') ? '$VALUE' : 'value',
      flex: 1,
      renderHeader: ({ field }) => (
        <Typography
          component="span"
          variant="body2"
          color={showComputed ? 'primary' : 'textPrimary'}
        >
          {showComputed && 'computed '}
          {field}
        </Typography>
      ),
      renderCell: ({ value }: GridRenderCellParams<Row, WrappedValue>) => (
        <ValueRenderer
          concretize={cachedConcretize}
          value={value}
          attribute={attribute}
        />
      ),
    });
    return defs;
  }, [arity, attribute, cachedConcretize, parameterNames, showComputed]);

  const rows = useMemo<Row[]>(() => {
    if (computedName === undefined) {
      return [];
    }
    const interpretation = partialInterpretation[computedName] ?? [];
    const existsInterpretation = partialInterpretation['builtin::exists'] ?? [];
    return interpretation.map((tuple) => {
      const nodeNames: NodeName[] = [];
      for (let i = 0; i < arity; i += 1) {
        const index = tuple[i];
        if (typeof index === 'number') {
          const node = nodes[index];
          const exists = binarySearch(existsInterpretation, [index]);
          if (node !== undefined) {
            nodeNames.push(new NodeName(node.name, exists === undefined));
          }
        }
      }
      return {
        nodes: nodeNames,
        value: new WrappedValue(tuple[arity]),
      };
    });
  }, [arity, nodes, partialInterpretation, computedName]);

  return (
    <Box sx={{ width: '100%', height: '100%' }}>
      <DataGrid
        slots={{
          toolbar: TableToolbar,
          noRowsOverlay: NoRowsOverlay,
          noResultsOverlay: NoResultsOverlay,
        }}
        slotProps={{
          toolbar: {
            graph,
          },
          noRowsOverlay: {
            graph,
          },
          noResultsOverlay: {
            graph,
          },
        }}
        showToolbar
        initialState={{ density: 'compact' }}
        rowSelection={false}
        columns={columns}
        rows={rows}
        getRowId={(row) => row.nodes.map(({ name }) => name).join(',')}
        sx={(theme) => ({
          border: 'none',
          backgroundColor: dimView
            ? theme.palette.outer.disabled
            : 'transparent',
          transition: theme.transitions.create('background-color', {
            duration: theme.transitions.duration.short,
          }),
          '@media (prefers-reduced-motion: reduce)': {
            backgroundColor: 'transparent',
          },
          '--DataGrid-rowBorderColor':
            theme.palette.mode === 'dark'
              ? alpha(theme.palette.text.primary, 0.24)
              : theme.palette.outer.border,
          '.MuiDataGrid-withBorderColor': {
            borderColor: theme.palette.outer.border,
          },
          '.TableToolbar-root': {
            background: touchesTop
              ? 'transparent'
              : theme.palette.outer.background,
          },
          '.MuiDataGrid-columnHeaders': {
            '.MuiDataGrid-columnHeader, .MuiDataGrid-filler, .MuiDataGrid-scrollbarFiller':
              {
                background: theme.palette.outer.background,
                borderBottom: `1px solid ${theme.palette.outer.border}`,
                borderTop: touchesTop
                  ? `1px solid ${theme.palette.outer.border}`
                  : 'none',
              },
          },
          '.MuiDataGrid-row--firstVisible .MuiDataGrid-scrollbarFiller': {
            display: 'none',
          },
          '.MuiDataGrid-footerContainer': {
            backgroundColor: theme.palette.outer.background,
          },
          '.MuiDataGrid-columnSeparator': {
            color: theme.palette.text.disabled,
          },
        })}
      />
    </Box>
  );
}

export default observer(TableArea);
