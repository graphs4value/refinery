/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import Autocomplete from '@mui/material/Autocomplete';
import Box from '@mui/material/Box';
import TextField from '@mui/material/TextField';
import { observer } from 'mobx-react-lite';

import type GraphStore from '../graph/GraphStore';
import RelationName from '../graph/RelationName';

function SymbolSelector({ graph }: { graph: GraphStore }): JSX.Element {
  const {
    selectedSymbol,
    semantics: { relations },
  } = graph;

  return (
    <Autocomplete
      renderInput={(params) => (
        <TextField
          {...{
            ...params,
            InputLabelProps: {
              ...params.InputLabelProps,
              // Workaround for type errors.
              className: params.InputLabelProps.className ?? '',
              style: params.InputLabelProps.style ?? {},
            },
          }}
          variant="standard"
          size="medium"
          placeholder="Symbol"
        />
      )}
      options={relations}
      getOptionLabel={(option) => option.name}
      renderOption={(props, option) => (
        <Box component="li" {...props}>
          <RelationName metadata={option} />
        </Box>
      )}
      value={selectedSymbol ?? null}
      isOptionEqualToValue={(option, value) => option.name === value.name}
      onChange={(_event, value) => graph.setSelectedSymbol(value ?? undefined)}
      sx={(theme) => ({
        flexBasis: 200,
        maxWidth: 600,
        flexGrow: 1,
        flexShrink: 1,
        '.MuiInput-underline::before': {
          borderColor:
            theme.palette.mode === 'dark'
              ? theme.palette.divider
              : theme.palette.outer.border,
        },
      })}
    />
  );
}

export default observer(SymbolSelector);
