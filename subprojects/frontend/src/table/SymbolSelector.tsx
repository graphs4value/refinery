/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import Autocomplete from '@mui/material/Autocomplete';
import Box from '@mui/material/Box';
import TextField from '@mui/material/TextField';
import clsx from 'clsx';
import { observer } from 'mobx-react-lite';
import { useMemo } from 'react';

import type GraphStore from '../graph/GraphStore';
import RelationName from '../graph/RelationName';
import isBuiltIn from '../utils/isBuiltIn';
import type { RelationMetadata } from '../xtext/xtextServiceResults';

const placeholderText = 'Select symbol to view';

function SymbolSelector({ graph }: { graph: GraphStore }): React.ReactElement {
  const {
    selectedSymbol,
    semantics: { relations },
  } = graph;

  const filteredRelations = useMemo(() => {
    const userRelations: RelationMetadata[] = [];
    const builtInRelations: RelationMetadata[] = [];
    relations.forEach((metadata) => {
      if (metadata.detail.type !== 'computed') {
        if (isBuiltIn(metadata)) {
          builtInRelations.push(metadata);
        } else {
          userRelations.push(metadata);
        }
      }
    });
    return [...userRelations, ...builtInRelations];
  }, [relations]);

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
              'aria-placeholder': placeholderText,
            },
          }}
          variant="standard"
          size="medium"
          placeholder={
            // Workaround to reduce flashing when changing generated model tabs
            selectedSymbol?.name ?? placeholderText
          }
          sx={(theme) =>
            selectedSymbol === undefined
              ? {}
              : {
                  // Workaround to reduce flashing when changing generated model tabs
                  '.MuiInput-input::placeholder': {
                    color: theme.palette.text.primary,
                    opacity: 1,
                  },
                }
          }
        />
      )}
      options={filteredRelations}
      getOptionLabel={(option) => option.name}
      renderOption={({ className, key, ...props }, option) => (
        <Box
          component="li"
          key={key as undefined}
          {...props}
          className={clsx(className ?? '', {
            builtInOption: isBuiltIn(option),
          })}
        >
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
      slotProps={{
        listbox: {
          sx: (theme) => ({
            '.builtInOption:not(.builtInOption + .builtInOption):not(:first-of-type)':
              {
                borderTop: `1px solid ${theme.palette.divider}`,
              },
          }),
        },
      }}
    />
  );
}

export default observer(SymbolSelector);
